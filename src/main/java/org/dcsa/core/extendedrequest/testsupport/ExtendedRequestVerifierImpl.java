package org.dcsa.core.extendedrequest.testsupport;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.ExtendedRequest;
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
            Pattern.compile("\\s+(FROM|(?:LEFT|RIGHT)?\\s*(?:INNER|OUTER)?\\s*JOIN|WHERE|ORDER BY)\\s");
    private static final Pattern FIELD_LIST_SPLIT = Pattern.compile(",");

    private final E request;

    private final LinkedHashMap<String, List<String>> params = new LinkedHashMap<>();

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

    public void verify(String expectedQuery, Consumer<E> requestMutator) {
        String generated;
        checkNotSpent();
        spent = true;
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
