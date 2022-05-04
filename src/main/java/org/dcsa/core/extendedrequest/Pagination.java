package org.dcsa.core.extendedrequest;

/**
 * A class to help provide pagination headers.
 * It encodes the pagination parameters to be used in pagination links (cursor link).
 * @param <T> the type of the class modeled by this {@code Class}
 */
public class Pagination<T> {
  public enum PageRequest {
    CURRENT,
    NEXT,
    PREVIOUS,
    FIRST,
    LAST
  }

  private static final String INDEX_CURSOR_SPLIT = "=";
  private final ExtendedParameters extendedParameters;
  public Pagination(ExtendedParameters extendedParameters) {
    this.extendedParameters = extendedParameters;
  }



  protected boolean encodePagination(StringBuilder sb, PageRequest page, int indexCursor, int limit, int total) {
    switch (page) {
      case CURRENT: encodeIndexCursor(sb, indexCursor); return true;
      case NEXT: return encodeNext(sb, indexCursor + limit, total);
      case PREVIOUS: return encodePrevious(sb, indexCursor - limit, limit);
      case FIRST: return encodeFirst(sb, indexCursor);
      case LAST: return encodeLast(sb, indexCursor, total - limit);
      default: return false;
    }
  }

  private boolean encodeNext(StringBuilder sb, int nextIndex, int total) {
    if (nextIndex < total) {
      encodeIndexCursor(sb, nextIndex);
      return true;
    } else {
      return false;
    }
  }

  private boolean encodePrevious(StringBuilder sb, int previousIndex, int limit) {
    if (previousIndex > 0) {
      encodeIndexCursor(sb, previousIndex);
      return true;
    } else if (previousIndex > -limit) {
      encodeIndexCursor(sb, 0);
      return true;
    } else {
      return false;
    }
  }

  private boolean encodeFirst(StringBuilder sb, int indexCursor) {
    if (indexCursor != 0) {
      encodeIndexCursor(sb, 0);
      return true;
    } else {
      return false;
    }
  }

  private boolean encodeLast(StringBuilder sb, int indexCursor, int lastIndex) {
    if (lastIndex > indexCursor) {
      encodeIndexCursor(sb, lastIndex);
      return true;
    } else {
      return false;
    }
  }

  private void encodeIndexCursor(StringBuilder sb, Integer index) {
    if (sb.length() != 0) {
      sb.append(ExtendedRequest.PARAMETER_SPLIT);
    }
    sb.append(extendedParameters.getIndexCursorName()).append(INDEX_CURSOR_SPLIT).append(index);
  }

}
