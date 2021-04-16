package org.dcsa.core.extendedrequest;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class ExtendedParameters {

    // ":" used as default separator between column to sort by and the direction (ASC or DESC)
    // This can be changed in the Application.yaml file to | by writing:
    // sort:
    //   direction:
    //     separator: \|
    @Value( "${sort.direction.separator::}" )
    private String sortDirectionSeparator;

    // "ASC" used as default specification for ascending sort
    // This can be changed in the Application.yaml file to "a" by writing:
    // sort:
    //   direction:
    //     ascending: a
    @Value( "${sort.direction.ascending:ASC}" )
    private String sortDirectionAscendingName;

    // "DESC" used as default specification for descending sort
    // This can be changed in the Application.yaml file to "d" by writing:
    // sort:
    //   direction:
    //     descending: d
    @Value( "${sort.direction.descending:DESC}" )
    private String sortDirectionDescendingName;

    // "sort" used as sorting parameter
    // This can be changed in the Application.yaml file to "orderBy" by writing:
    // sort:
    //   sortName: orderBy
    @Value( "${sort.sortName:sort}" )
    private String sortParameterName;

    // Default pagination pageSize set to ALL elements
    // This can be changed in the Application.yaml file to 20 by writing:
    // pagination:
    //   defaultPageSize: 20
    @Value( "${pagination.defaultPageSize:#{null}}" )
    private Integer defaultPageSize;

    // Default pagination pageSize variable name set to "limit"
    // This can be changed in Application.yaml file to "pageSize" by writing:
    // pagination:
    //   config:
    //     pageSizeName: pageSize
    @Value( "${pagination.config.pageSizeName:limit}" )
    private String paginationPageSizeName;

    // Default pagination cursor variable name set to "cursor"
    // This can be changed in Application.yaml fil to "page" by writing:
    // pagination:
    //   config:
    //     cursorName: page
    @Value( "${pagination.config.cursorName:cursor}" )
    private String paginationCursorName;

    // Default current-page header name set to "Current-Page"
    // This can be changed in Application.yaml fil to "current" by writing:
    // pagination:
    //   config:
    //     currentPageName: current
    @Value( "${pagination.config.currentPageName:Current-Page}" )
    private String paginationCurrentPageName;

    // Default next-page header name set to "Next-Page"
    // This can be changed in Application.yaml fil to "next" by writing:
    // pagination:
    //   config:
    //     currentPageName: next
    @Value( "${pagination.config.nextPageName:Next-Page}" )
    private String paginationNextPageName;

    // Default previous-page header name set to "Previous-Page"
    // This can be changed in Application.yaml fil to "previous" by writing:
    // pagination:
    //   config:
    //     currentPageName: previous
    @Value( "${pagination.config.previousPageName:Previous-Page}" )
    private String paginationPreviousPageName;

    // Default first-page header name set to "First-Page"
    // This can be changed in Application.yaml fil to "first" by writing:
    // pagination:
    //   config:
    //     currentPageName: first
    @Value( "${pagination.config.firstPageName:First-Page}" )
    private String paginationFirstPageName;

    // Default current-page header name set to "Last-Page"
    // This can be changed in Application.yaml fil to "last" by writing:
    // pagination:
    //   config:
    //     currentPageName: last
    @Value( "${pagination.config.firstPageName:Last-Page}" )
    private String paginationLastPageName;

    // Set a list of reserved parameters that the extended base controller should ignore
    // This can be changed in Application.yaml fil to "expand,show" by writing:
    // pagination:
    //   config:
    //     currentPageName: expand,show
    @Value( "${pagination.reservedParameters:#{null}}" )
    private String reservedParameters;

    // Set the encryption key used to encrypt cursor (KeySet) based pagination. If null - no encryption will be used.
    // This can be changed in Application.yaml fil to "xyz123" by writing:
    // pagination:
    //   encryptionKey: xyz123
    @Value( "${pagination.encryptionKey:#{null}}" )
    private String encryptionKey;

    // Set the internal pagination parameter name to "|Offset|". It is important that this name does NOT
    // conflict with field names. It needs to be unique!
    // This can be changed in Application.yaml fil to "|index|" by writing:
    // pagination:
    //   internal
    //     cursor: |index|
    @Value( "${pagination.internal.cursor:|Offset|}" )
    private String indexCursorName;

    // Set the splitter for Enum values. If multiple Enum values are specified - a list will be created by splitting
    // on the specified value.
    // This can be changed in Application.yaml fil to "|" by writing:
    // enum:
    //   split: |
    @Value( "${enum.split:,}" )
    private String enumSplit;

    // Choose how query parameter attributes are handled.  Options include:
    //
    // * NO_ATTRIBUTES                 - There are no attributes at all.
    // * PARAMETER_NAME_ARRAY_NOTATION - The attribute is in the query parameter
    //                                   name a la "name[attribute]=value"
    // * PARAMETER_NAME_SUFFIX         - The attribute is in the query parameter
    //                                   name a la "name:attribute=value".
    // * PARAMETER_VALUE_PREFIX        - The attribute is a prefix of the value
    //                                   a la "name=attribute:value".
    //
    // Note that attributes are parsed from the decoded parameter name or value.
    //
    // Attributes can be used to alter how a query parameter is interpreted and
    // is often used for operators a la "x >= y"-operations.  However, the
    // application can implement its own attributes.
    //
    // This can be changed in application.yaml file by writing:
    // search:
    //   queryParameterAttributeHandling: PARAMETER_VALUE_PREFIX
    @Value("${search.queryParameterAttributeHandling:PARAMETER_NAME_SUFFIX}")
    @Getter
    private QueryParameterAttributeHandling queryParameterAttributeHandling;

    // Choose the separator for attributes embedded in the query parameters.
    // The separator is applied on the decoded value.  Whether it is used
    // for the name or the value is determined by
    // queryParameterAttributeHandling.
    //
    // This can be changed in application.yaml file by writing:
    // search:
    //   queryParameterAttributeSeparator: "_"
    @Value("${search.queryParameterAttributeSeparator::}")
    @Getter
    private String queryParameterAttributeSeparator;

    public String getSortDirectionSeparator() {
        return sortDirectionSeparator;
    }

    public String getSortDirectionAscendingName() {
        return sortDirectionAscendingName;
    }

    public String getSortDirectionDescendingName() {
        return sortDirectionDescendingName;
    }

    public String getSortParameterName() {
        return sortParameterName;
    }

    public Integer getDefaultPageSize() {
        return defaultPageSize;
    }

    public String getPaginationPageSizeName() {
        return paginationPageSizeName;
    }

    public String getPaginationCursorName() {
        return paginationCursorName;
    }

    public String getPaginationCurrentPageName() {
        return paginationCurrentPageName;
    }

    public String getPaginationNextPageName() {
        return paginationNextPageName;
    }

    public String getPaginationPreviousPageName() {
        return paginationPreviousPageName;
    }

    public String getPaginationFirstPageName() {
        return paginationFirstPageName;
    }

    public String getPaginationLastPageName() {
        return paginationLastPageName;
    }

    public List<String> getReservedParameters() {
        return reservedParameters != null ?
                Arrays.asList(reservedParameters.split("\\\\s*,\\\\s*")) :
                Collections.emptyList();
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public String getIndexCursorName() {
        return indexCursorName;
    }

    public String getEnumSplit() {
        return enumSplit;
    }

}
