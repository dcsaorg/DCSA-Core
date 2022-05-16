package org.dcsa.core.extendedrequest.testsupport;

import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Test helper for {@link ExtendedRequest} or subclasses thereof
 *
 * This class can be used for writing test cases for custom implementations of ExtendedRequest.
 * The test will cause the ExtendedRequest to generate the SQL it would use and let the test
 * compare that to an expected SQL.  The verifier uses a pre-defined dialect (Postgres) with
 * some tweaks to provide stable SQL with named bind parameters for making the test cases easier
 * to write and reason about.
 *
 *
 * <pre>{@code
 * @SpringBootTest
 * @ContextConfiguration(classes = ExtendedParameters.class)
 * public class MyExtendedRequestTest {
 *
 *     @Autowired
 *     private ExtendedParameters extendedParameters;
 *
 *     @Test
 *     public void testQueryBySubclassExtendedRequest() {
 *         String baseQueryNoExtraJoins =
 *                 "SELECT city_table.id AS \"id\", city_table.city_name AS \"name\", city_table.country_id AS \"countryId\""
 *                         + " FROM city_table";
 *         String extraJoins = " JOIN country_table c ON city_table.country_id = c.id";
 *         Function<R2dbcDialect, CitySpecificExtendedRequest> requestConstructor = (r2dbcDialect) -> new CitySpecificExtendedRequest(extendedParameters, r2dbcDialect);
 *         verifierFor(requestConstructor).verify(baseQueryNoExtraJoins);
 *
 *         verifierFor(requestConstructor)
 *                 .withParam("cn", "dk")
 *                 .verify(baseQueryNoExtraJoins + extraJoins + " WHERE c.country_name = :cn");
 *     }
 * }
 *
 * }</pre>
 *
 * @param <E> The concrete type of ExtendedRequest.
 * @param <T> The model type for the ExtendedRequest (subclass).
 */
public interface ExtendedRequestVerifier<E extends ExtendedRequest<T>, T> {
  /**
   * Provide a given query parameter which will be passed to the ExtendedRequest when generating the query.
   *
   * @param param Name of the parameter (e.g., "carrierBookingReference" or "sort"). The method <i>can</i>
   *              be repeated with the same value for param.  In this case, the values are combined into a
   *              list. The parameter will be treated as a query parameter and subject to attribute parsing
   *              (e.g., "foo:gte" is the "foo" parameter with the attribute "gte").
   * @param value The value associate to the param.
   * @return An ExtendedRequestVerifier enabling you to chain into another method.
   */
  ExtendedRequestVerifier<E, T> withParam(String param, String value);

  /**
   * Provide a given query parameter which will be passed to the ExtendedRequest as if it was done via cursor.
   *
   * This is similar to {@link #withParam(String, String)} but uses the cursor instead.
   *
   * Note: As implementation detail, this may be done by parsing the cursor parameters as regular parameters first
   * and then have the underlying {@link ExtendedRequest} generate the cursor from that. This implies that
   * "cursor-only" parameters may be restricted in use.
   *
   * @param param Name of the parameter (e.g., "carrierBookingReference" or "sort"). The method <i>can</i>
   *              be repeated with the same value for param.  In this case, the values are combined into a
   *              list. The parameter will be treated as a query parameter and subject to attribute parsing
   *              (e.g., "foo:gte" is the "foo" parameter with the attribute "gte").
   * @param value The value associate to the param.
   * @return An ExtendedRequestVerifier enabling you to chain into another method.
   */
  ExtendedRequestVerifier<E, T> withCursorParam(String param, String value);

  /**
   * Determine the offset for the cursor
   *
   * Set the cursor's offset value to the provided value. This will trigger an "OFFSET" in the SQL on the
   * assumption that the cursor will use the OFFSET based cursoring.
   *
   * Note: As implementation detail, this may be done by parsing the cursor parameters as regular parameters first
   * and then have the underlying {@link ExtendedRequest} generate the cursor from that. This should not matter
   * from a functional behaviour test perspective, but it may affect things like mocking.
   *
   * @param value The value associate to the param.
   * @return An ExtendedRequestVerifier enabling you to chain into another method.
   */
  ExtendedRequestVerifier<E, T> withCursorOffset(int value);

  /**
   * Perform the test and verify the generated SQL with an expected SQL query
   *
   * @param expectedQuery The expected version of the SQL.  Note the SQL will be beautified.
   * @param requestMutator A general purpose mutator enabling arbitrary changes to the ExtendedRequest
   *                       before generating the SQL. This is mostly useful if you want to do some
   *                       tweaking that cannot be done directly via this verifier interface.
   *                       The mutator can undo things that the verifier normally handles such
   *                       as parameter parsing.
   */
  void verify(String expectedQuery, Consumer<E> requestMutator);

  /**
   * Perform the test and verify the generated SQL with an expected SQL query
   *
   * @param expectedQuery The expected version of the SQL.  Note the SQL will be beautified.
   */
  default void verify(String expectedQuery) {
    this.verify(expectedQuery, null);
  }

  /**
   * Create an ExtendedRequestVerifier for a standard ExtendedRequest
   *
   * @param extendedParameters An instance of ExtendedParameters (rely on bean injection to provide it).
   * @param clazz The model class
   * @param <T> The model type for ExtendedRequest.
   * @return An ExtendedRequestVerifier
   */
  static <T> ExtendedRequestVerifier<ExtendedRequest<T>, T> verifierFor(ExtendedParameters extendedParameters, Class<T> clazz) {
    return verifierFor(ExtendedRequest::new, extendedParameters, clazz);
  }

  /**
   * Create an ExtendedRequestVerifier for a ExtendedRequest with a constructor compatible with ExtendedRequest
   *
   * <pre>{@code
   * @SpringBootTest
   * @ContextConfiguration(classes = ExtendedParameters.class)
   * public class MyExtendedRequestTest {
   *
   *     @Autowired
   *     private ExtendedParameters extendedParameters;
   *
   *     @Test
   *     public void testQueryBySubclassExtendedRequest() {
   *        String expectedQuery = "...";
   *        ExtendedRequestVerifier.verifierFor(MyExtendedRequestSubclass::new, extendedRequest, ModelClass.class)
   *          .verify(expectedQuery);
   *     }
   * }
   * }</pre>
   *
   * @param constructor A method reference to the constructor (usually something like <code>ExtendedRequestSubclass::new</code>)
   * @param extendedParameters An instance of ExtendedParameters (rely on bean injection to provide it).
   * @param clazz The model class
   * @param <T> The model type for the ExtendedRequest subclass.
   * @return An ExtendedRequestVerifier
   */
  static <E extends ExtendedRequest<T>, T> ExtendedRequestVerifier<E, T> verifierFor(TriFunction<ExtendedParameters, R2dbcDialect, Class<T>, E> constructor,
                                                                                     ExtendedParameters extendedParameters,
                                                                                     Class<T> clazz) {
    return new ExtendedRequestVerifierImpl<>(constructor.apply(extendedParameters, new MockR2dbcDialect(), clazz));
  }


  /**
   * Create an ExtendedRequestVerifier using only a dialect
   *
   * This method is primarily useful when the subclass does not have a compatible constructor
   * (e.g., does not need the class argument) and it is cleaner not to "fudge" into one of
   * the other cases.
   *
   * <pre>{@code
   * @SpringBootTest
   * @ContextConfiguration(classes = ExtendedParameters.class)
   * public class MyExtendedRequestTest {
   *
   *     @Autowired
   *     private ExtendedParameters extendedParameters;
   *
   *     @Test
   *     public void testQueryBySubclassExtendedRequest() {
   *        String expectedQuery = "...";
   *        ExtendedRequestVerifier.verifierFor(dialect -> new MyExtendedRequestSubclass(extendedParameters, dialect))
   *          .verify(expectedQuery);
   *     }
   * }
   * }</pre>
   *
   * @param constructor A lambda reference to the constructor (usually a lambda function)
   * @param <T> The model type for the ExtendedRequest subclass.
   * @return An ExtendedRequestVerifier
   */
  static <E extends ExtendedRequest<T>, T> ExtendedRequestVerifier<E, T> verifierFor(Function<R2dbcDialect, E> constructor) {
    return new ExtendedRequestVerifierImpl<>(constructor.apply(new MockR2dbcDialect()));
  }
}
