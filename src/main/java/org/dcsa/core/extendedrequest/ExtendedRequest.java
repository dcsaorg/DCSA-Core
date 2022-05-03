package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.dcsa.core.exception.ConcreteRequestErrorMessageException;
import org.dcsa.core.query.DBEntityAnalysis;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.sql.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.PreparedOperation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A class to handle extended requests. Extended requests is request requiring one of the following:
 * pagination, filtering, sorting or limiting of Collection results.
 * It converts between JSON names, POJO field names and Database column names using the modelClass
 * which is the POJO used as DAO. This is done by looking at @JsonProperty and @Column annotations on the modelClass.
 * This class contains 4 helper classes: Sort, Join, Filter and Pagination whom are specialized classes handling
 * different parts of the request.
 * The class manages everything from creating the SQL to extract the result to creating the query parameter for
 * paginated results.
 * All parameters can be configured in Application.yaml, default values are stored in the ExtendedParameter class
 * NB: Because if time limitations the current implementation simulates Key-Set based pagination but is in fact
 * offset-based pagination (OFFSET is used in the SQL queries)
 * @param <T> the type of the class modeled by this {@code Class}
 *  * object.
 */
@RequiredArgsConstructor
public class ExtendedRequest<T> {

  public static final String PARAMETER_SPLIT = "&";
  public static final String CURSOR_SPLIT = "=";
  public static final String FILTER_SPLIT = "=";


  private final ExtendedParameters extendedParameters;
  private final R2dbcDialect r2dbcDialect;
  private final Class<T> modelClass;
  protected Pagination<T> pagination;
  protected QueryParameterParser<T> queryParameterParser;
  private CursorBackedFilterCondition filterCondition;

  @Getter
  @Setter
  protected boolean selectDistinct;

  private final Set<String> joinAliasInUse = new HashSet<>();
  @Getter
  private DBEntityAnalysis<T> dbEntityAnalysis;

  @Getter
  @Setter
  private int queryCount = -1;  // -1 is a placeholder, so we can tell whether setQueryTotal has been called.

  public Class<T> getModelClass() {
    return modelClass;
  }

  public ExtendedParameters getExtendedParameters() {
    return extendedParameters;
  }

  public void parseParameter(Map<String, List<String>> params) {
    // Reset parameters
    resetParameters();
    getQueryParameterParser().parseQueryParameter(params);
    finishedParsingParameters();
  }

  // For sub-classes to hook into this
  protected void finishedParsingParameters() {
    filterCondition = getQueryParameterParser().build();
    for (QueryField queryField : filterCondition.getReferencedQueryFields()) {
      this.markQueryFieldInUse(queryField);
    }
  }

  public void resetParameters() {
    pagination = new Pagination<>(getExtendedParameters());
    selectDistinct = false;
    dbEntityAnalysis = this.prepareDBEntityAnalysis().build();
    queryParameterParser = new QueryParameterParser<>(extendedParameters, r2dbcDialect, dbEntityAnalysis);
    joinAliasInUse.clear();
  }

  public QueryParameterParser<T> getQueryParameterParser() {
    return queryParameterParser;
  }


  public DatabaseClient.GenericExecuteSpec getCount(DatabaseClient databaseClient) {
    return databaseClient.sql(this.getCountQuery());
  }

  public DatabaseClient.GenericExecuteSpec getFindAll(DatabaseClient databaseClient) {
    return databaseClient.sql(this.getQuery());
  }

  @SuppressWarnings("unchecked")
  protected <SB extends SelectBuilder.SelectLimitOffset> SB applyLimitOffset(SB t) {
    int limit = filterCondition.getLimit();
    int indexCursor = filterCondition.getOffset();
    if (limit != 0 && indexCursor != 0) {
      return (SB) t.limitOffset(limit, indexCursor);
    }
    if (limit != 0) {
      return (SB) t.limit(limit);
    }
    return indexCursor != 0 ? (SB) t.offset(indexCursor) : t;
  }

  public Select getSelectQuery() {
    List<Expression> expressions = dbEntityAnalysis.getAllSelectableFields().stream().map(queryField -> {
      markQueryFieldInUse(queryField);
      return queryField.getSelectColumn();
    }).collect(Collectors.toList());

    return generateBaseQuery(Select.builder().select(expressions), true)
      .orderBy(filterCondition.getOrderByFields()).build();
  }

  public Select getSelectCountQuery() {
    return generateBaseQuery(Select.builder().select(
      Functions.count(Expressions.asterisk()).as("count")
    ), false).build();
  }

  protected SelectBuilder.SelectOrdered generateBaseQuery(SelectBuilder.SelectAndFrom selectBuilder, boolean withLimits) {
    if (filterCondition == null) {
      finishedParsingParameters();
    }
    if (selectDistinct) {
      selectBuilder = selectBuilder.distinct();
    }
    SelectBuilder.SelectFromAndJoin selectFromAndJoin = selectBuilder.from(
      dbEntityAnalysis.getTableAndJoins().getPrimaryTable()
    );
    if (withLimits) {
      selectFromAndJoin = applyLimitOffset(selectFromAndJoin);
    }
    SelectBuilder.SelectWhere selectWhere = applyJoins(selectFromAndJoin);
    Condition con = filterCondition.computeCondition(r2dbcDialect);
    if (TrueCondition.INSTANCE.equals(con)) {
      return selectWhere;
    }
    return selectWhere.where(con);
  }

  protected SelectBuilder.SelectWhere applyJoins(SelectBuilder.SelectFromAndJoin selectBuilder) {
    if (!joinAliasInUse.isEmpty()) {
      return dbEntityAnalysis.getTableAndJoins().applyJoins(selectBuilder, joinAliasInUse);
    }
    return selectBuilder;
  }

  public PreparedOperation<Select> getCountQuery() {
    if (filterCondition == null) {
      finishedParsingParameters();
    }
    RenderContextFactory factory = new RenderContextFactory(r2dbcDialect);
    return PreparedQuery.of(getSelectCountQuery(), factory.createRenderContext(), filterCondition.getBindings());
  }

  /**
   * Called whenever a field is being used
   *
   * This is called when a field has been detected as in use for the coming query.
   * Unused fields can cause joins to be omitted.
   *
   * Note that any field registered via {@link #getQueryParameterParser()} are automatically
   * registered via {@link #parseParameter(Map)} (etc.).  However, subclasses
   * that need special handling of some fields/Sort rules, etc. can call this to
   * register the fields as in used as needed.
   *
   * @param fieldInUse The DbField that has been used
   */
  protected void markQueryFieldInUse(QueryField fieldInUse) {
    String joinAlias = fieldInUse.getTableJoinAlias();
    TableAndJoins tableAndJoins = dbEntityAnalysis.getTableAndJoins();
    while (joinAlias != null) {
      JoinDescriptor descriptor = tableAndJoins.getJoinDescriptor(joinAlias);
      if (descriptor == null) {
        break;
      }
      String alias = descriptor.getJoinAliasId();
      if (!joinAliasInUse.add(alias)) {
        break;
      }
      joinAlias = descriptor.getDependentAlias();
    }
  }


  /**
   * Called during initialization to load and register all fields.
   *
   * Subclasses can overwrite this to alter which fields and joins are available.
   * The default implementation uses {@link DBEntityAnalysis.DBEntityAnalysisBuilder#loadFieldsAndJoinsFromModel()}
   * which should be sufficient for most use-cases.
   *
   * Returns a builder to enable composing (i.e. enable the subclass to do
   * {@code return super.prepareDBEntityAnalysis().my().Change().Here();}
   */
  protected DBEntityAnalysis.DBEntityAnalysisBuilder<T> prepareDBEntityAnalysis() {
    return DBEntityAnalysis.builder(this.modelClass).loadFieldsAndJoinsFromModel();
  }

  public PreparedOperation<Select> getQuery() {
    if (filterCondition == null) {
      finishedParsingParameters();
      assert filterCondition != null;
    }
    RenderContextFactory factory = new RenderContextFactory(r2dbcDialect);
    return PreparedQuery.of(getSelectQuery(), factory.createRenderContext(), filterCondition.getBindings());
  }

  public boolean ignoreUnknownProperties() {
    JsonIgnoreProperties jsonIgnoreProperties = modelClass.getAnnotation(JsonIgnoreProperties.class);
    return jsonIgnoreProperties != null && jsonIgnoreProperties.ignoreUnknown();
  }

  public void insertHeaders(ServerHttpResponse response, ServerHttpRequest request) {
    HttpHeaders headers = response.getHeaders();
    StringBuilder exposeHeaders = new StringBuilder();

    String uri = getURI(request);
    addPaginationHeaders(exposeHeaders, headers, uri);

    // Expose headers to frontEnd
    response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders.toString());
  }

  protected void addPaginationHeaders(StringBuilder exposeHeaders, HttpHeaders headers, String uri) {
    assert filterCondition != null : "Parameters ought to have been parsed by now";
//    TODO: Fix me so that I don't break integration tests (https://dcsa.atlassian.net/browse/DDT-1034)
//    assert queryCount > -1 : "The total number of entities must be known for this to work";
    if (filterCondition.getLimit() > 0) {
      addPaginationHeader(uri, headers, getExtendedParameters().getPaginationCurrentPageName(), Pagination.PageRequest.CURRENT, exposeHeaders);
      addPaginationHeader(uri, headers, getExtendedParameters().getPaginationFirstPageName(), Pagination.PageRequest.FIRST, exposeHeaders);
      addPaginationHeader(uri, headers, getExtendedParameters().getPaginationPreviousPageName(), Pagination.PageRequest.PREVIOUS, exposeHeaders);
      addPaginationHeader(uri, headers, getExtendedParameters().getPaginationNextPageName(), Pagination.PageRequest.NEXT, exposeHeaders);
      addPaginationHeader(uri, headers, getExtendedParameters().getPaginationLastPageName(), Pagination.PageRequest.LAST, exposeHeaders);
    } else {
      addPaginationHeader(uri, headers, getExtendedParameters().getPaginationCurrentPageName(), null, exposeHeaders);
    }
  }

  protected void addPaginationHeader(String uri, HttpHeaders headers, String headerName, Pagination.PageRequest page, StringBuilder exposeHeaders) {
    String pageHeader = getHeaderPageCursor(page);
    if (pageHeader != null) {
      headers.add(headerName, uri + (pageHeader.isEmpty() ? "" : "?" + pageHeader));
      exposeHeaders.append(headerName).append(',');
    }
  }

  protected String getURI(ServerHttpRequest request) {
    return request.getURI().getScheme() + "://" + request.getURI().getRawAuthority() + request.getURI().getRawPath();
  }

  private String getHeaderPageCursor(Pagination.PageRequest page) {
    StringBuilder sb = new StringBuilder();
    if (page != null && !pagination.encodePagination(sb, page, filterCondition.getOffset(), filterCondition.getLimit(), queryCount)) {
      return null;
    }
    for (Map.Entry<String, List<String>> filterParam : filterCondition.getCursorParameters().entrySet()) {
      String parameter = filterParam.getKey();
      for (String value : filterParam.getValue()) {
        if (sb.length() > 0) {
          sb.append(PARAMETER_SPLIT);
        }
        sb.append(parameter).append(FILTER_SPLIT).append(value);
      }
    }
    if (page == null) {
      return sb.toString();
    }
    byte[] parameters = sb.toString().getBytes(StandardCharsets.UTF_8);
    return getExtendedParameters().getPaginationCursorName() + CURSOR_SPLIT + Base64.getUrlEncoder().withoutPadding().encodeToString(parameters);
  }
}
