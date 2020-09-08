package org.dcsa.core.extendedrequest;

import org.dcsa.core.exception.GetException;

/**
 * A class to help managing pagination parameters and limiting the sql result.
 * It parses pagination parameters (limit and internal cursorIndex).
 * It encodes the pagination parameters to be used in pagination links (cursor link).
 * It creates the SQL (LIMIT and OFFSET clause) used for database requests.
 * NB: It should be noted that OFFSET is used for offset based bagination - KeySet based pagination
 * will be implemented at a later stage
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
    private static final String LIMIT_SPLIT = "=";

    private final ExtendedRequest<T> extendedRequest;
    private final ExtendedParameters extendedParameters;

    private Integer limit;
    private int indexCursor;
    private int total;

    public Pagination(ExtendedRequest<T> extendedRequest, ExtendedParameters extendedParameters) {
        this.extendedRequest = extendedRequest;
        this.extendedParameters = extendedParameters;

        limit = extendedParameters.getDefaultPageSize();
    }

    protected void parseInternalPaginationCursor(String cursorValue) {
        indexCursor = Integer.parseInt(cursorValue);
    }

    protected void parseLimitParameter(String value, boolean fromCursor) {
        if (extendedRequest.isCursor()) {
            throw new GetException("Cannot use the Limit parameter while accessing a paginated result using the " + extendedParameters.getPaginationCursorName() + " parameter!");
        }

        try {
            limit = Integer.parseInt(value);
            if (!fromCursor) {
                extendedRequest.setNoCursor();
            }
        } catch (NumberFormatException numberFormatException) {
            throw new GetException("Unknown " + extendedParameters.getPaginationPageSizeName() + " value:" + value + ". Must be a Number!");
        }
    }

    protected void getOffsetQueryString(StringBuilder sb) {
        if (indexCursor != 0) {
            sb.append(" OFFSET ").append(indexCursor);
        }
    }

    protected void getLimitQueryString(StringBuilder sb) {
        if (limit != null) {
            sb.append(" LIMIT ").append(limit);
        }
    }

    protected boolean encodePagination(StringBuilder sb, PageRequest page) {
        switch (page) {
            case CURRENT: encodeIndexCursor(sb, indexCursor); return true;
            case NEXT: return encodeNext(sb, indexCursor + limit);
            case PREVIOUS: return encodePrevious(sb, indexCursor - limit);
            case FIRST: return encodeFirst(sb);
            case LAST: return encodeLast(sb, total - limit);
            default: return false;
        }
    }

    private boolean encodeNext(StringBuilder sb, int nextIndex) {
        if (nextIndex < total) {
            encodeIndexCursor(sb, nextIndex);
            return true;
        } else {
            return false;
        }
    }

    private boolean encodePrevious(StringBuilder sb, int previousIndex) {
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

    private boolean encodeFirst(StringBuilder sb) {
        if (indexCursor != 0) {
            encodeIndexCursor(sb, 0);
            return true;
        } else {
            return false;
        }
    }

    private boolean encodeLast(StringBuilder sb, int lastIndex) {
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

    protected void encodeLimit(StringBuilder sb) {
        if (limit != null) {
            if (sb.length() != 0) {
                sb.append(ExtendedRequest.PARAMETER_SPLIT);
            }
            sb.append(extendedParameters.getPaginationPageSizeName()).append(LIMIT_SPLIT).append(limit);
        }
    }

    public Integer getIndexCursor() {
        return indexCursor;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotal() {
        return total;
    }
}
