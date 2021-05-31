package org.dcsa.core;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.mock.MockR2dbcDialect;
import org.dcsa.core.models.CitySpecificExtendedRequest;
import org.dcsa.core.models.combined.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

@SpringBootTest(properties = {
        "sort.sortName=sort",
        "search.queryParameterAttributeHandling=PARAMETER_NAME_ARRAY_NOTATION"
})
@ContextConfiguration(classes = ExtendedParameters.class)
public class ExtendedRequestTest {

    private static final Pattern COLLAPSE_SPACE = Pattern.compile("\\s\\s++");
    private static final Pattern PRETTY_PRINT_SPLIT =
            Pattern.compile("\\s+(FROM|(?:LEFT|RIGHT)?\\s*(?:INNER|OUTER)?\\s*JOIN|WHERE|ORDER BY)\\s");

    @Autowired
    private ExtendedParameters extendedParameters;

    @Test
    public void testCustomerWithAddress() {
        String baseQuery = "SELECT customer_table.customer_id AS \"id\", customer_table.customer_name AS \"name\", address_table.street_name AS \"address\""
                + " FROM customer_table"
                + " JOIN address_table ON customer_table.address_id = address_table.address_id";
        request(CustomerWithAddress.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testCustomerWithForeignKeyAddresses() {
        String baseQuery = "SELECT delivery_address.address_id AS \"deliveryAddress.address_id\", delivery_address.street_name AS \"deliveryAddress.street_name\", delivery_address.city_id AS \"deliveryAddress.city_id\", payment_address.address_id AS \"paymentAddress.address_id\", payment_address.street_name AS \"paymentAddress.street_name\", payment_address.city_id AS \"paymentAddress.city_id\", customer_table.customer_id AS \"customer_id\", customer_table.customer_name AS \"customer_name\", customer_table.delivery_address_id AS \"delivery_address_id\", customer_table.payment_address_id AS \"payment_address_id\""
                + " FROM customer_table"
                + " JOIN address_table delivery_address ON customer_table.delivery_address_id = delivery_address.address_id"
                + " JOIN address_table payment_address ON customer_table.payment_address_id = payment_address.address_id";
        request(CustomerWithForeignKeyAddresses.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testCustomerBook() {
        String baseQuery = "SELECT customer_table.customer_id AS \"customer.customer_id\", customer_table.customer_name AS \"customer.customer_name\", delivery_address.address_id AS \"customer.deliveryAddress.address_id\", delivery_address.street_name AS \"customer.deliveryAddress.street_name\", delivery_address.city_id AS \"customer.deliveryAddress.city_id\", customer_table.delivery_address_id AS \"customer.delivery_address_id\", payment_address.address_id AS \"customer.paymentAddress.address_id\", payment_address.street_name AS \"customer.paymentAddress.street_name\", payment_address.city_id AS \"customer.paymentAddress.city_id\", customer_table.payment_address_id AS \"customer.payment_address_id\", customer_book_table.customer_book_id AS \"customer_book_id\", customer_book_table.customer_book_name AS \"customer_book_name\", customer_book_table.customer_id AS \"customer_id\""
                + " FROM customer_book_table"
                + " JOIN customer_table ON customer_book_table.customer_id = customer_table.customer_id"
                + " JOIN address_table delivery_address ON customer_table.delivery_address_id = delivery_address.address_id"
                + " JOIN address_table payment_address ON customer_table.payment_address_id = payment_address.address_id";
        request(CustomerBook.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testCityCustomerBook() {
        String baseQuery = "SELECT city_table.id AS \"city.id\", city_table.city_name AS \"city.city_name\", city_table.country_id AS \"city.country_id\", customer_table.customer_id AS \"customer.customer_id\", customer_table.customer_name AS \"customer.customer_name\", delivery_address.address_id AS \"customer.deliveryAddress.address_id\", delivery_address.street_name AS \"customer.deliveryAddress.street_name\", delivery_address.city_id AS \"customer.deliveryAddress.city_id\", customer_table.delivery_address_id AS \"customer.delivery_address_id\", payment_address.address_id AS \"customer.paymentAddress.address_id\", payment_address.street_name AS \"customer.paymentAddress.street_name\", payment_address.city_id AS \"customer.paymentAddress.city_id\", customer_table.payment_address_id AS \"customer.payment_address_id\", customer_book_table.city_id AS \"city_id\", customer_book_table.customer_book_id AS \"customer_book_id\", customer_book_table.customer_book_name AS \"customer_book_name\", customer_book_table.customer_id AS \"customer_id\""
                + " FROM customer_book_table"
                + " JOIN city_table ON customer_book_table.city_id = city_table.id"
                + " JOIN customer_table ON customer_book_table.customer_id = customer_table.customer_id"
                + " JOIN address_table delivery_address ON customer_table.delivery_address_id = delivery_address.address_id"
                + " JOIN address_table payment_address ON customer_table.payment_address_id = payment_address.address_id";
        request(CityCustomerBook.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testOrderWithCustomerAndAddresses() {
        String baseQuery = "SELECT order_table.orderline AS \"orderline\", customer_table.customer_name AS \"customerName\", customer_address.street_name AS \"customerAddress\", warehouse_address.street_name AS \"warehouseAddress\""
                + " FROM order_table"
                + " JOIN customer_table ON order_table.customer_id = customer_table.address_id"
                + " JOIN address_table customer_address ON customer_table.address_id = customer_address.address_id"
                + " JOIN address_table warehouse_address ON order_table.address_id = warehouse_address.address_id";
        request(OrderWithCustomerAndAddresses.class, extendedParameters).verify(baseQuery);

        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("warehouse", "a")
                .verify(baseQuery + " WHERE warehouse_address.street_name = :warehouse");

        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("warehouse", "a")
                .withParam("customerName", "b")
                .verify(baseQuery
                        + " WHERE warehouse_address.street_name = :warehouse"
                        +  " AND customer_table.customer_name = :customerName"
                );

        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("warehouse", "a")
                .withParam("customerName", "b")
                .withParam("customerAddress", "c")
                .withParam("sort", "customerAddress,warehouse")
                .verify(baseQuery
                        + " WHERE warehouse_address.street_name = :warehouse"
                        + " AND customer_table.customer_name = :customerName"
                        + " AND customer_address.street_name = :customerAddress"
                        + " ORDER BY \"customerAddress\" ASC, \"warehouseAddress\" ASC"
                );
    }

    @Test
    public void testOrderByCountry() {
        String baseQueryNoExtraJoins =
                "SELECT order_table.orderline AS \"orderline\", customer_table.customer_name AS \"customerName\", customer_address.street_name AS \"customerAddress\", warehouse_address.street_name AS \"warehouseAddress\""
                + " FROM order_table"
                + " JOIN customer_table ON order_table.customer_id = customer_table.address_id"
                + " JOIN address_table customer_address ON customer_table.address_id = customer_address.address_id"
                + " JOIN address_table warehouse_address ON order_table.address_id = warehouse_address.address_id";
        String extraJoins = " JOIN city_table ON customer_address.city_id = city_table.id"
                +  " JOIN country_table ON city_table.country_id = country_table.id";
        request(OrderInCountry.class, extendedParameters).verify(baseQueryNoExtraJoins);

        request(OrderInCountry.class, extendedParameters)
                .withParam("countryName", "dk")
                .verify(baseQueryNoExtraJoins + extraJoins + " WHERE country_table.country_name = :countryName");
    }

    @Test
    public void testOrderWithEverything() {
        String baseQueryNoExtraJoins =
                "SELECT customer_table.customer_name AS \"customerName\", customer_address.street_name AS \"customerAddress\", warehouse_address.street_name AS \"warehouseAddress\", order_table.order_id AS \"order_id\", order_table.orderline AS \"orderline\", order_table.customer_id AS \"customer_id\", order_table.address_id AS \"address_id\", order_table.delivery_date AS \"delivery_date\""
                        + " FROM order_table"
                        + " JOIN customer_table ON order_table.customer_id = customer_table.address_id"
                        + " JOIN address_table customer_address ON customer_table.address_id = customer_address.address_id"
                        + " JOIN address_table warehouse_address ON order_table.address_id = warehouse_address.address_id";
        String extraJoins = " JOIN city_table ON customer_address.city_id = city_table.id"
                +  " JOIN country_table ON city_table.country_id = country_table.id";
        request(OrderWithEverything.class, extendedParameters).verify(baseQueryNoExtraJoins);

        request(OrderWithEverything.class, extendedParameters)
                .withParam("countryName", "dk")
                .verify(baseQueryNoExtraJoins + extraJoins + " WHERE country_table.country_name = :countryName");
    }

    @Test
    public void testExtendedOrder() {
        String query =
                "SELECT address_table.street_name AS \"warehouseAddress\", order_table.order_id AS \"order_id\", order_table.orderline AS \"orderline\", order_table.customer_id AS \"customer_id\", order_table.address_id AS \"address_id\", order_table.delivery_date AS \"delivery_date\""
                        + " FROM order_table"
                        + " JOIN address_table ON order_table.address_id = address_table.address_id";
        request(ExtendedOrder.class, extendedParameters).verify(query);

        request(ExtendedOrder.class, extendedParameters)
                .withParam("deliveryDate[gte]", "2021-01-01T00:00:00Z")
                .verify(query + " WHERE order_table.delivery_date >= :deliveryDate");
    }

    @Test
    public void testExtendedOrderDistinct() {
        String query =
                "SELECT DISTINCT address_table.street_name AS \"warehouseAddress\", order_table.order_id AS \"order_id\", order_table.orderline AS \"orderline\", order_table.customer_id AS \"customer_id\", order_table.address_id AS \"address_id\", order_table.delivery_date AS \"delivery_date\""
                        + " FROM order_table"
                        + " JOIN address_table ON order_table.address_id = address_table.address_id";
        request(ExtendedOrder.class, extendedParameters).verify(query, req -> req.setSelectDistinct(true));
    }

    @Test
    public void testQueryBySubclassExtendedRequest() {
        String baseQueryNoExtraJoins =
                "SELECT city_table.id AS \"id\", city_table.city_name AS \"city_name\", city_table.country_id AS \"country_id\""
                        + " FROM city_table";
        String extraJoins = " JOIN country_table c ON city_table.country_id = c.id";
        Function<R2dbcDialect, CitySpecificExtendedRequest> requestConstructor = (r2dbcDialect) -> new CitySpecificExtendedRequest(extendedParameters, r2dbcDialect);
        request(requestConstructor).verify(baseQueryNoExtraJoins);

        request(requestConstructor)
                .withParam("cn", "dk")
                .verify(baseQueryNoExtraJoins + extraJoins + " WHERE c.country_name = :cn");
    }

    private static <T> ExtendedRequestVerifier<T> request(Class<T> clazz, ExtendedParameters extendedParameters) {
        return new ExtendedRequestVerifier<>(new ExtendedRequest<>(extendedParameters, new MockR2dbcDialect(), clazz));
    }

    private static <T> ExtendedRequestVerifier<T> request(Function<R2dbcDialect, ? extends ExtendedRequest<T>> requestCreator) {
        return new ExtendedRequestVerifier<>(requestCreator.apply(new MockR2dbcDialect()));
    }

    @RequiredArgsConstructor
    private static class ExtendedRequestVerifier<T> {

        private final ExtendedRequest<T> request;

        private final LinkedHashMap<String, List<String>> params = new LinkedHashMap<>();

        public ExtendedRequestVerifier<T> withParam(String param, String value) {
            this.params.computeIfAbsent(param, k -> new ArrayList<>()).add(value);
            return this;
        }

        public void verify(String query, Consumer<ExtendedRequest<T>> requestMutator) {
            String generated;
            if (params.isEmpty()) {
                request.resetParameters();
            } else {
                request.parseParameter(params);
            }
            if (requestMutator != null) {
                requestMutator.accept(request);
            }
            generated = request.getQuery().toQuery();
            Assertions.assertEquals(prettifyQuery(query), prettifyQuery(generated));
        }

        public void verify(String query) {
            this.verify(query, null);
        }

        // makes IntelliJ's "show differences" view more useful in case of a mismatch
        private static String prettifyQuery(String text) {
            String intermediate = COLLAPSE_SPACE.matcher(text).replaceAll(" ");
            return PRETTY_PRINT_SPLIT.matcher(intermediate).replaceAll("\n $1 ");
        }
    }
}
