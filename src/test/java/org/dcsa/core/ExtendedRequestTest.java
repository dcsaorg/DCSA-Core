package org.dcsa.core;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.models.CitySpecificExtendedRequest;
import org.dcsa.core.models.combined.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@SpringBootTest(properties = { "sort.sortName=sort" })
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
                + " JOIN address_table AS address_table ON customer_table.address_id=address_table.address_id";
        request(CustomerWithAddress.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testOrderWithCustomerAndAddresses() {
        String baseQuery = "SELECT order_table.orderline AS \"orderline\", customer_table.customer_name AS \"customerName\", customer_address.street_name AS \"customerAddress\", warehouse_address.street_name AS \"warehouseAddress\""
                + " FROM order_table"
                + " JOIN customer_table AS customer_table ON order_table.customer_id=customer_table.address_id"
                + " JOIN address_table AS customer_address ON customer_table.address_id=customer_address.address_id"
                + " JOIN address_table AS warehouse_address ON order_table.address_id=warehouse_address.address_id";
        request(OrderWithCustomerAndAddresses.class, extendedParameters).verify(baseQuery);

        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("warehouse", "a")
                .verify(baseQuery + " WHERE warehouse_address.street_name ILIKE :warehouse1");

        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("warehouse", "a")
                .withParam("customerName", "b")
                .verify(baseQuery
                        + " WHERE warehouse_address.street_name ILIKE :warehouse1"
                        + " AND customer_table.customer_name ILIKE :customerName2"
                );


        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("warehouse", "a")
                .withParam("customerName", "b")
                .withParam("customerAddress", "c")
                .withParam("sort", "customerAddress,warehouse")
                .verify(baseQuery
                        + " WHERE warehouse_address.street_name ILIKE :warehouse1"
                        + " AND customer_table.customer_name ILIKE :customerName2"
                        + " AND customer_address.street_name ILIKE :customerAddress3"
                        + " ORDER BY customer_address.street_name ASC, warehouse_address.street_name ASC"
                );
    }

    @Test
    public void testOrderByCountry() {
        String baseQueryNoExtraJoins =
                "SELECT order_table.orderline AS \"orderline\", customer_table.customer_name AS \"customerName\", customer_address.street_name AS \"customerAddress\", warehouse_address.street_name AS \"warehouseAddress\""
                + " FROM order_table"
                + " JOIN customer_table AS customer_table ON order_table.customer_id=customer_table.address_id"
                + " JOIN address_table AS customer_address ON customer_table.address_id=customer_address.address_id"
                + " JOIN address_table AS warehouse_address ON order_table.address_id=warehouse_address.address_id";
        String extraJoins = " JOIN city_table AS city_table ON address_table.city_id=city_table.id"
                +  " JOIN country_table AS country_table ON city_table.country_id=country_table.id";
        request(OrderInCountry.class, extendedParameters).verify(baseQueryNoExtraJoins);

        request(OrderInCountry.class, extendedParameters)
                .withParam("countryName", "dk")
                .verify(baseQueryNoExtraJoins + extraJoins + " WHERE country_table.country_name ILIKE :countryName1");
    }

    @Test
    public void testOrderWithEverything() {
        String baseQueryNoExtraJoins =
                "SELECT customer_table.customer_name AS \"customerName\", customer_address.street_name AS \"customerAddress\", warehouse_address.street_name AS \"warehouseAddress\", order_table.order_id AS \"order_id\", order_table.orderline AS \"orderline\", order_table.customer_id AS \"customer_id\", order_table.address_id AS \"address_id\""
                        + " FROM order_table"
                        + " JOIN customer_table AS customer_table ON order_table.customer_id=customer_table.address_id"
                        + " JOIN address_table AS customer_address ON customer_table.address_id=customer_address.address_id"
                        + " JOIN address_table AS warehouse_address ON order_table.address_id=warehouse_address.address_id";
        String extraJoins = " JOIN city_table AS city_table ON address_table.city_id=city_table.id"
                +  " JOIN country_table AS country_table ON city_table.country_id=country_table.id";
        request(OrderWithEverything.class, extendedParameters).verify(baseQueryNoExtraJoins);

        request(OrderWithEverything.class, extendedParameters)
                .withParam("countryName", "dk")
                .verify(baseQueryNoExtraJoins + extraJoins + " WHERE country_table.country_name ILIKE :countryName1");
    }

    @Test
    public void testExtendedOrder() {
        String query =
                "SELECT address_table.street_name AS \"warehouseAddress\", order_table.order_id AS \"order_id\", order_table.orderline AS \"orderline\", order_table.customer_id AS \"customer_id\", order_table.address_id AS \"address_id\""
                        + " FROM order_table"
                        + " JOIN address_table AS address_table ON order_table.address_id=address_table.address_id";
        request(ExtendedOrder.class, extendedParameters).verify(query);
    }

    @Test
    public void testExtendedOrderDistinct() {
        String query =
                "SELECT DISTINCT address_table.street_name AS \"warehouseAddress\", order_table.order_id AS \"order_id\", order_table.orderline AS \"orderline\", order_table.customer_id AS \"customer_id\", order_table.address_id AS \"address_id\""
                        + " FROM order_table"
                        + " JOIN address_table AS address_table ON order_table.address_id=address_table.address_id";
        request(ExtendedOrder.class, extendedParameters).verify(query, req -> req.setSelectDistinct(true));
    }

    @Test
    public void testQueryBySubclassExtendedRequest() {
        String baseQueryNoExtraJoins =
                "SELECT city_table.id AS \"id\", city_table.city_name AS \"city_name\", city_table.country_id AS \"country_id\""
                        + " FROM city_table";
        String extraJoins = " JOIN country_table AS c ON c.id = city_table.country_id";
        CitySpecificExtendedRequest request = new CitySpecificExtendedRequest(extendedParameters);
        request(request).verify(baseQueryNoExtraJoins);

        request(request)
                .withParam("cn", "dk")
                .verify(baseQueryNoExtraJoins + extraJoins + " WHERE c.country_name ILIKE :cn1");
    }

    private static <T> ExtendedRequestVerifier<T> request(Class<T> clazz, ExtendedParameters extendedParameters) {
        return new ExtendedRequestVerifier<>(new ExtendedRequest<>(extendedParameters, clazz));
    }

    private static <T> ExtendedRequestVerifier<T> request(ExtendedRequest<T> request) {
        return new ExtendedRequestVerifier<>(request);
    }

    @RequiredArgsConstructor
    private static class ExtendedRequestVerifier<T> {

        private final ExtendedRequest<T> request;

        private final LinkedHashMap<String, String> params = new LinkedHashMap<>();

        public ExtendedRequestVerifier<T> withParam(String param, String value) {
            this.params.put(param, value);
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
            generated = request.getQuery();
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
