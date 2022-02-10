package org.dcsa.core;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.mock.MockR2dbcDialect;
import org.dcsa.core.models.A;
import org.dcsa.core.models.CitySpecificExtendedRequest;
import org.dcsa.core.models.combined.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
    private static final Pattern FIELD_LIST_SPLIT = Pattern.compile(",");

    @Autowired
    private ExtendedParameters extendedParameters;

    @Test
    public void testCustomerWithAddress() {
        String baseQuery = "SELECT customer_table.address AS \"address\","
                + "    customer_table.customer_id AS \"id\","
                + "    customer_table.customer_name AS \"name\","
                + "    customer_table.address_id AS \"addressId\","
                + "    address_table.address_id AS \"address.addressId\","
                + "    address_table.street_name AS \"address.address\","
                + "    address_table.city_id AS \"address.cityId\""
                + " FROM customer_table"
                + " JOIN address_table ON customer_table.address_id = address_table.address_id";
        request(CustomerWithAddress.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testCustomerWithForeignKeyAddresses() {
        String baseQuery = "SELECT customer_table.customer_id AS \"id\", customer_table.customer_name AS \"name\", customer_table.delivery_address_id AS \"deliveryAddressId\", customer_table.payment_address_id AS \"paymentAddressId\","
                + "     delivery_address.address_id AS \"deliveryAddress.addressId\", delivery_address.street_name AS \"deliveryAddress.address\", delivery_address.city_id AS \"deliveryAddress.cityId\", payment_address.address_id AS \"paymentAddress.addressId\", payment_address.street_name AS \"paymentAddress.address\", payment_address.city_id AS \"paymentAddress.cityId\""
                + " FROM customer_table"
                + " JOIN address_table delivery_address ON customer_table.delivery_address_id = delivery_address.address_id"
                + " JOIN address_table payment_address ON customer_table.payment_address_id = payment_address.address_id";
        request(CustomerWithForeignKeyAddresses.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testCustomerBook() {
        String baseQuery = "SELECT customer_book_table.customer_book_id AS \"id\", customer_book_table.customer_book_name AS \"name\", customer_book_table.customer_id AS \"customerId\","
                + "      customer_table.customer_id AS \"customer.id\", customer_table.customer_name AS \"customer.name\", customer_table.delivery_address_id AS \"customer.deliveryAddressId\", customer_table.payment_address_id AS \"customer.paymentAddressId\","
                + "      customer_table__delivery_address.address_id AS \"customer.deliveryAddress.addressId\", customer_table__delivery_address.street_name AS \"customer.deliveryAddress.address\", customer_table__delivery_address.city_id AS \"customer.deliveryAddress.cityId\","
                + "      customer_table__payment_address.address_id AS \"customer.paymentAddress.addressId\", customer_table__payment_address.street_name AS \"customer.paymentAddress.address\", customer_table__payment_address.city_id AS \"customer.paymentAddress.cityId\""
                + " FROM customer_book_table"
                + " JOIN customer_table ON customer_book_table.customer_id = customer_table.customer_id"
                + " JOIN address_table customer_table__delivery_address ON customer_table.delivery_address_id = customer_table__delivery_address.address_id"
                + " JOIN address_table customer_table__payment_address ON customer_table.payment_address_id = customer_table__payment_address.address_id";
        request(CustomerBook.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testCityCustomerBook() {
        String baseQuery = "SELECT customer_book_table.city_id AS \"cityId\", customer_book_table.customer_book_id AS \"id\", customer_book_table.customer_book_name AS \"name\", customer_book_table.customer_id AS \"customerId\","
                + "     city_table.id AS \"city.id\", city_table.city_name AS \"city.name\", city_table.country_id AS \"city.countryId\","
                + "     customer_table.customer_id AS \"customer.id\", customer_table.customer_name AS \"customer.name\", customer_table.delivery_address_id AS \"customer.deliveryAddressId\", customer_table.payment_address_id AS \"customer.paymentAddressId\","
                + "     customer_table__delivery_address.address_id AS \"customer.deliveryAddress.addressId\", customer_table__delivery_address.street_name AS \"customer.deliveryAddress.address\", customer_table__delivery_address.city_id AS \"customer.deliveryAddress.cityId\","
                + "     customer_table__payment_address.address_id AS \"customer.paymentAddress.addressId\", customer_table__payment_address.street_name AS \"customer.paymentAddress.address\", customer_table__payment_address.city_id AS \"customer.paymentAddress.cityId\""
                + " FROM customer_book_table"
                + " JOIN city_table ON customer_book_table.city_id = city_table.id"
                + " JOIN customer_table ON customer_book_table.customer_id = customer_table.customer_id"
                + " JOIN address_table customer_table__delivery_address ON customer_table.delivery_address_id = customer_table__delivery_address.address_id"
                + " JOIN address_table customer_table__payment_address ON customer_table.payment_address_id = customer_table__payment_address.address_id";
        request(CityCustomerBook.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testA() {
        String baseQuery = "SELECT A_table.id AS \"id\", A_table.bId AS \"bId\", B_table.id AS \"b.id\", B_table.cId AS \"b.cId\", B_table.dId AS \"b.dId\", B_table.fId_column AS \"b.fId\", B_table__c1__e1.id AS \"b.e1.id\", B_table__c1__e1.name AS \"b.e1.name\", B_table__c2__e2.id AS \"b.e2.id\", B_table__c2__e2.name AS \"b.e2.name\", B_table__D_table.id AS \"b.d.id\", B_table__D_table.cId AS \"b.d.cId\", B_table__D_table__C_table.id AS \"b.d.c.id\", B_table__D_table__C_table.eId AS \"b.d.c.eId\", B_table__F_table.id AS \"b.f.id\""
                + " FROM A_table"
                + " JOIN B_table ON A_table.bId = B_table.id"
                + " JOIN C_table B_table__c1 ON B_table.cId = B_table__c1.id"
                + " JOIN E_table B_table__c1__e1 ON B_table__c1.eId = B_table__c1__e1.id"
                + " JOIN C_table B_table__c2 ON B_table.cId = B_table__c2.id"
                + " JOIN E_table B_table__c2__e2 ON B_table__c2.eId = B_table__c2__e2.id"
                + " JOIN D_table B_table__D_table ON B_table.dId = B_table__D_table.id"
                + " JOIN C_table B_table__D_table__C_table ON B_table__D_table.cId = B_table__D_table__C_table.id"
                + " JOIN F_table B_table__F_table ON B_table.fId_column = B_table__F_table.id";
        request(A.class, extendedParameters).verify(baseQuery);
    }

    @Test
    public void testAWithWhere() {
        String baseQuery = "SELECT A_table.id AS \"id\", A_table.bId AS \"bId\", B_table.id AS \"b.id\", B_table.cId AS \"b.cId\", B_table.dId AS \"b.dId\", B_table.fId_column AS \"b.fId\", B_table__c1__e1.id AS \"b.e1.id\", B_table__c1__e1.name AS \"b.e1.name\", B_table__c2__e2.id AS \"b.e2.id\", B_table__c2__e2.name AS \"b.e2.name\", B_table__D_table.id AS \"b.d.id\", B_table__D_table.cId AS \"b.d.cId\", B_table__D_table__C_table.id AS \"b.d.c.id\", B_table__D_table__C_table.eId AS \"b.d.c.eId\", B_table__F_table.id AS \"b.f.id\""
                + " FROM A_table"
                + " JOIN B_table ON A_table.bId = B_table.id"
                + " JOIN C_table B_table__c1 ON B_table.cId = B_table__c1.id"
                + " JOIN E_table B_table__c1__e1 ON B_table__c1.eId = B_table__c1__e1.id"
                + " JOIN C_table B_table__c2 ON B_table.cId = B_table__c2.id"
                + " JOIN E_table B_table__c2__e2 ON B_table__c2.eId = B_table__c2__e2.id"
                + " JOIN D_table B_table__D_table ON B_table.dId = B_table__D_table.id"
                + " JOIN C_table B_table__D_table__C_table ON B_table__D_table.cId = B_table__D_table__C_table.id"
                + " JOIN F_table B_table__F_table ON B_table.fId_column = B_table__F_table.id";
        request(A.class, extendedParameters)
                .withParam("b.e1.name", "a")
                .verify(baseQuery + " WHERE B_table__c1__e1.name = :b.e1.name");

        request(A.class, extendedParameters)
                .withParam("b.e1.name", "a")
                .verify(baseQuery.replace("SELECT", "SELECT DISTINCT") + " WHERE B_table__c1__e1.name = :b.e1.name",
                        req -> req.setSelectDistinct(true));
    }

    @Test
    public void testOrderWithCustomerAndAddresses() {
        String baseQuery = "SELECT order_table.customer AS \"customer\","
                + "    order_table.customerAddress AS \"customerAddress\","
                + "    order_table.warehouseAddress AS \"warehouse\","
                + "    order_table.order_id AS \"id\","
                + "    order_table.orderline AS \"orderline\","
                + "    order_table.customer_id AS \"receiverId\","
                + "    order_table.address_id AS \"warehouseAddressId\","
                + "    order_table.delivery_date AS \"deliveryDate\","
                + "    customer_table.customer_id AS \"customer.id\","
                + "    customer_table.customer_name AS \"customer.name\","
                + "    customer_table.address_id AS \"customer.addressId\","
                + "    customer_table__customer_address.address_id AS \"customerAddress.addressId\","
                + "    customer_table__customer_address.street_name AS \"customerAddress.address\","
                + "    customer_table__customer_address.city_id AS \"customerAddress.cityId\","
                + "    warehouse_address.address_id AS \"warehouse.addressId\","
                + "    warehouse_address.street_name AS \"warehouse.address\","
                + "    warehouse_address.city_id AS \"warehouse.cityId\""
                + " FROM order_table"
                + " JOIN customer_table ON order_table.customer_id = customer_table.address_id"
                + " JOIN address_table customer_table__customer_address ON customer_table.address_id = customer_table__customer_address.address_id"
                + " JOIN address_table warehouse_address ON order_table.address_id = warehouse_address.address_id";
        request(OrderWithCustomerAndAddresses.class, extendedParameters).verify(baseQuery);

        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("warehouse.address", "a")
                .verify(baseQuery + " WHERE warehouse_address.street_name = :warehouse.address");

        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("warehouse.address", "a")
                .withParam("customer.name", "b")
                .verify(baseQuery
                        + " WHERE warehouse_address.street_name = :warehouse.address"
                        +  " AND customer_table.customer_name = :customer.name"
                );

        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("warehouse.address", "a")
                .withParam("customer.name", "b")
                .withParam("customerAddress.address", "c")
                .withParam("sort", "customerAddress,warehouse")
                .verify(baseQuery
                        + " WHERE warehouse_address.street_name = :warehouse.address"
                        + " AND customer_table.customer_name = :customer.name"
                        + " AND customer_table__customer_address.street_name = :customerAddress.address"
                        + " ORDER BY \"customerAddress\" ASC, \"warehouse\" ASC"
                );

        request(OrderWithCustomerAndAddresses.class, extendedParameters)
                .withParam("deliveryDate[gte]", "2021-01-01T00:00:00Z")
                .verify(baseQuery + " WHERE order_table.delivery_date >= :deliveryDate");
    }

    @Test
    public void testQueryBySubclassExtendedRequest() {
        String baseQueryNoExtraJoins =
                "SELECT city_table.id AS \"id\", city_table.city_name AS \"name\", city_table.country_id AS \"countryId\""
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
            String generatedPretty = prettifyQuery(generated);
            Assertions.assertEquals(prettifyQuery(query), generatedPretty);
            Assertions.assertFalse(generatedPretty.contains(".."),
                    "Generated SQL contains \"..\" which is unlikely to be intentional");
        }

        public void verify(String query) {
            this.verify(query, null);
        }

        // makes IntelliJ's "show differences" view more useful in case of a mismatch
        private static String prettifyQuery(String text) {
            String intermediate = COLLAPSE_SPACE.matcher(text).replaceAll(" ");
            String multiline = PRETTY_PRINT_SPLIT.matcher(intermediate).replaceAll("\n $1 ");
            return FIELD_LIST_SPLIT.matcher(multiline).replaceAll(",\n   ");
        }
    }
}
