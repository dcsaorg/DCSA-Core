package org.dcsa.core.extendedrequest.testsupport;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.extendedrequest.Pagination;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@RequiredArgsConstructor
class ExtendedRequestVerifierImpl<E extends ExtendedRequest<T>, T> implements ExtendedRequestVerifier<E, T> {

  private static final Pattern COLLAPSE_SPACE = Pattern.compile("\\s\\s++");
  private static final Pattern PRETTY_PRINT_SPLIT =
    Pattern.compile("\\s+(FROM|(?:LEFT|RIGHT)?\\s*(?:INNER|OUTER)?\\s*JOIN|WHERE|ORDER BY|LIMIT|OFFSET)\\s");
  private static final Pattern FIELD_LIST_SPLIT = Pattern.compile(",");

  private final E request;

  private final LinkedHashMap<String, List<String>> params = new LinkedHashMap<>();
  private final LinkedHashMap<String, List<String>> cursorParam = new LinkedHashMap<>();

  private int offset = -1;
  boolean spent = false;

  private void checkNotSpent() {
    if (spent) {
      throw new IllegalStateException("The ExtendedRequestVerifier has been spent. Please create new one for further testing");
    }
  }
  public ExtendedRequestVerifier<E, T> withParam(String param, String value) {
    checkNotSpent();
    this.params.computeIfAbsent(param, k -> new ArrayList<>()).add(value);
    return this;
  }

  @Override
  public ExtendedRequestVerifier<E, T> withCursorParam(String param, String value) {
    checkNotSpent();
    if (param.equals(request.getExtendedParameters().getIndexCursorName())) {
      throw new IllegalArgumentException("The cursor offset parameter should be set via withCursorOffset");
    }
    this.cursorParam.computeIfAbsent(param, k -> new ArrayList<>()).add(value);
    return this;
  }

  @Override
  public ExtendedRequestVerifier<E, T> withCursorOffset(int value) {
    checkNotSpent();
    if (value < 0) {
      throw new IllegalArgumentException("Cursor offset must be at least 0.");
    }
    if (offset != -1) {
      throw new IllegalStateException("Only one cursor offset is possible");
    }
    offset = value;
    return this;
  }

  public void verify(String expectedQuery, Consumer<E> requestMutator) {
    String generated;
    checkNotSpent();
    spent = true;
    if (!cursorParam.isEmpty() || offset > -1) {
      if (offset == -1) {
        offset = 0;
      }
      request.parseParameter(cursorParam);
      // Set to an arbitrary number to ensure it is initialized.
      request.setQueryCount(100);
      String cursorHeaderValue = request.getHeaderPageCursor(Pagination.PageRequest.CURRENT, offset);
      assert cursorHeaderValue.contains(ExtendedRequest.CURSOR_SPLIT);
      String cursorValue = cursorHeaderValue.split(ExtendedRequest.CURSOR_SPLIT, 2)[1];
      params.computeIfAbsent(request.getExtendedParameters().getPaginationCursorName(), k -> new ArrayList<>()).add(cursorValue);
    }
    if (params.isEmpty()) {
      request.resetParameters();
    } else {
      request.parseParameter(params);
    }
    if (requestMutator != null) {
      requestMutator.accept(request);
    }
    generated = request.getQuery().toQuery();
    String generatedPretty = prettifyQuery(generated);
    Assertions.assertEquals(prettifyQuery(expectedQuery), generatedPretty);
    Assertions.assertFalse(generatedPretty.contains(".."),
      "Generated SQL contains \"..\" which is unlikely to be intentional");
  }

  // makes IntelliJ's "show differences" view more useful in case of a mismatch
  static String prettifyQuery(String text) {
    String intermediate = COLLAPSE_SPACE.matcher(text).replaceAll(" ");
    String multiline = PRETTY_PRINT_SPLIT.matcher(intermediate).replaceAll("\n $1 ");
    return FIELD_LIST_SPLIT.matcher(multiline).replaceAll(",\n   ");
  }
}
