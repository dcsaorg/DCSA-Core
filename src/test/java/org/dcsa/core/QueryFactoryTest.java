package org.dcsa.core;

import org.dcsa.core.mock.MockR2dbcDialect;
import org.dcsa.core.models.combined.*;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.query.QueryFactoryBuilder;
import org.dcsa.core.query.impl.AbstractQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.function.Function;
import java.util.regex.Pattern;

public class QueryFactoryTest {

    private static final Pattern COLLAPSE_SPACE = Pattern.compile("\\s\\s++");
    private static final Pattern PRETTY_PRINT_SPLIT =
            Pattern.compile("\\s+(FROM|(?:LEFT|RIGHT)?\\s*(?:INNER|OUTER)?\\s*JOIN|WHERE|ORDER BY)\\s");

    @Test
    public void testCustomerWithAddress() {
        String baseQuery = "SELECT customer_table.customer_id AS \"id\", customer_table.customer_name AS \"name\", address_table.street_name AS \"address\""
                + " FROM customer_table"
                + " JOIN address_table ON customer_table.address_id = address_table.address_id";
        verify(CustomerWithAddress.class, baseQuery);
    }

    @Test
    public void testCustomerWithForeignKeyAddresses() {
        String baseQuery = "SELECT delivery_address.address_id AS \"deliveryAddress.address_id\", delivery_address.street_name AS \"deliveryAddress.street_name\", delivery_address.city_id AS \"deliveryAddress.city_id\", payment_address.address_id AS \"paymentAddress.address_id\", payment_address.street_name AS \"paymentAddress.street_name\", payment_address.city_id AS \"paymentAddress.city_id\", customer_table.customer_id AS \"customer_id\", customer_table.customer_name AS \"customer_name\", customer_table.delivery_address_id AS \"delivery_address_id\", customer_table.payment_address_id AS \"payment_address_id\""
                + " FROM customer_table"
                + " JOIN address_table delivery_address ON customer_table.delivery_address_id = delivery_address.address_id"
                + " JOIN address_table payment_address ON customer_table.payment_address_id = payment_address.address_id";
        verify(CustomerWithForeignKeyAddresses.class, baseQuery);
    }

    @Test
    public void testCustomerBook() {
        String baseQuery = "SELECT customer_table.customer_id AS \"customer.customer_id\", customer_table.customer_name AS \"customer.customer_name\", delivery_address.address_id AS \"customer.deliveryAddress.address_id\", delivery_address.street_name AS \"customer.deliveryAddress.street_name\", delivery_address.city_id AS \"customer.deliveryAddress.city_id\", customer_table.delivery_address_id AS \"customer.delivery_address_id\", payment_address.address_id AS \"customer.paymentAddress.address_id\", payment_address.street_name AS \"customer.paymentAddress.street_name\", payment_address.city_id AS \"customer.paymentAddress.city_id\", customer_table.payment_address_id AS \"customer.payment_address_id\", customer_book_table.customer_id AS \"customer_id\", customer_book_table.customer_book_id AS \"customer_book_id\", customer_book_table.customer_book_name AS \"customer_book_name\""
                + " FROM customer_book_table"
                + " JOIN customer_table ON customer_book_table.customer_id = customer_table.customer_id"
                + " JOIN address_table delivery_address ON customer_table.delivery_address_id = delivery_address.address_id"
                + " JOIN address_table payment_address ON customer_table.payment_address_id = payment_address.address_id";
        verify(CustomerBook.class, baseQuery);
    }

    @Test
    public void testCityCustomerBook() {
        String baseQuery = "SELECT city_table.id AS \"city.id\", city_table.city_name AS \"city.city_name\", city_table.country_id AS \"city.country_id\", customer_table.customer_id AS \"customer.customer_id\", customer_table.customer_name AS \"customer.customer_name\", delivery_address.address_id AS \"customer.deliveryAddress.address_id\", delivery_address.street_name AS \"customer.deliveryAddress.street_name\", delivery_address.city_id AS \"customer.deliveryAddress.city_id\", customer_table.delivery_address_id AS \"customer.delivery_address_id\", payment_address.address_id AS \"customer.paymentAddress.address_id\", payment_address.street_name AS \"customer.paymentAddress.street_name\", payment_address.city_id AS \"customer.paymentAddress.city_id\", customer_table.payment_address_id AS \"customer.payment_address_id\", customer_book_table.city_id AS \"city_id\", customer_book_table.customer_id AS \"customer_id\", customer_book_table.customer_book_id AS \"customer_book_id\", customer_book_table.customer_book_name AS \"customer_book_name\""
                + " FROM customer_book_table"
                + " JOIN city_table ON customer_book_table.city_id = city_table.id"
                + " JOIN customer_table ON customer_book_table.customer_id = customer_table.customer_id"
                + " JOIN address_table delivery_address ON customer_table.delivery_address_id = delivery_address.address_id"
                + " JOIN address_table payment_address ON customer_table.payment_address_id = payment_address.address_id";
        verify(CityCustomerBook.class, baseQuery);
    }

    @Test
    public void testOrderWithCustomerAndAddresses() {
        String baseQuery = "SELECT order_table.orderline AS \"orderline\", customer_table.customer_name AS \"customerName\", customer_address.street_name AS \"customerAddress\", warehouse_address.street_name AS \"warehouseAddress\""
                + " FROM order_table"
                + " JOIN customer_table ON order_table.customer_id = customer_table.address_id"
                + " JOIN address_table customer_address ON customer_table.address_id = customer_address.address_id"
                + " JOIN address_table warehouse_address ON order_table.address_id = warehouse_address.address_id";

        QueryFactoryBuilder.FrozenBuilder<QueryFactoryBuilder.ConditionBuilder<OrderInCountry>> baseBuilder = builderForClass(OrderInCountry.class)
                .conditions().freeze();

        QueryFactoryBuilder.FrozenBuilder<QueryFactoryBuilder.ConditionBuilder<OrderInCountry>> withWarehouse = baseBuilder.copyBuilder()
                .fieldByJsonName("warehouse").equalTo("a").freeze();

        QueryFactoryBuilder.FrozenBuilder<QueryFactoryBuilder.ConditionBuilder<OrderInCountry>> withWarehouseAndCustomerName = withWarehouse.copyBuilder()
                .fieldByJsonName("customerName").equalTo("b").freeze();

        verify(baseBuilder.copyBuilder().build(), baseQuery);

        verify(withWarehouse.copyBuilder().build(),
                baseQuery + " WHERE warehouse_address.street_name = :warehouse");

        verify(withWarehouseAndCustomerName.copyBuilder().build(),
                baseQuery
                        + " WHERE warehouse_address.street_name = :warehouse"
                        +  " AND customer_table.customer_name = :customerName"
                );

        verify(withWarehouseAndCustomerName.copyBuilder()
                .fieldByJsonName("customerAddress").equalTo("c")
                .withQuery()
                .order((orderBuilder, dbAnalysis) -> orderBuilder.orderBy(
                        dbAnalysis.getQueryFieldFromJSONName("customerAddress").asOrderByField(Sort.Direction.ASC),
                        dbAnalysis.getQueryFieldFromJSONName("warehouse").asOrderByField(Sort.Direction.ASC)
                )).build(),
                baseQuery
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

        QueryFactoryBuilder.FrozenBuilder<QueryFactoryBuilder.ConditionBuilder<OrderInCountry>> builder = builderForClass(OrderInCountry.class)
                .conditions().freeze();
        verify(builder.copyBuilder().build(), baseQueryNoExtraJoins);

        verify(builder.copyBuilder().fieldByJsonName("countryName").equalTo("dk").build(),
                baseQueryNoExtraJoins + extraJoins + " WHERE country_table.country_name = :countryName");
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

        QueryFactoryBuilder.FrozenBuilder<QueryFactoryBuilder.ConditionBuilder<OrderWithEverything>> builder = builderForClass(OrderWithEverything.class)
                .conditions().freeze();

        verify(builder.copyBuilder().build(), baseQueryNoExtraJoins);

        verify(builder.copyBuilder().fieldByJsonName("countryName").equalTo("dk").build(),
                baseQueryNoExtraJoins + extraJoins + " WHERE country_table.country_name = :countryName");
    }

    @Test
    public void testExtendedOrder() {
        String query =
                "SELECT address_table.street_name AS \"warehouseAddress\", order_table.order_id AS \"order_id\", order_table.orderline AS \"orderline\", order_table.customer_id AS \"customer_id\", order_table.address_id AS \"address_id\", order_table.delivery_date AS \"delivery_date\""
                        + " FROM order_table"
                        + " JOIN address_table ON order_table.address_id = address_table.address_id";
        QueryFactoryBuilder.FrozenBuilder<QueryFactoryBuilder.ConditionBuilder<ExtendedOrder>> builder = builderForClass(ExtendedOrder.class)
                .conditions().freeze();

        verify(builder.copyBuilder().build(), query);

        verify(builder.copyBuilder().fieldByJsonName("deliveryDate").greaterThanOrEqualTo(OffsetDateTime.parse("2021-01-01T00:00:00Z")).build(),
                query + " WHERE order_table.delivery_date >= :deliveryDate");
    }

    @Test
    public void testExtendedOrderDistinct() {
        String query =
                "SELECT DISTINCT address_table.street_name AS \"warehouseAddress\", order_table.order_id AS \"order_id\", order_table.orderline AS \"orderline\", order_table.customer_id AS \"customer_id\", order_table.address_id AS \"address_id\", order_table.delivery_date AS \"delivery_date\""
                        + " FROM order_table"
                        + " JOIN address_table ON order_table.address_id = address_table.address_id";
        verify(ExtendedOrder.class, builder -> builder.distinct().conditions().build(), query);
    }


    private static <T> QueryFactoryBuilder<T> builderForClass(Class<T> clazz) {
        return QueryFactoryBuilder.builder(DBEntityAnalysis.builder(clazz).loadFieldsAndJoinsFromModel().build())
                .r2dbcDialect(new MockR2dbcDialect());
    }

    private static <T> void verify(AbstractQueryFactory<T> factory, String expected) {
        String generated = factory.generateSelectQuery().toQuery();
        Assertions.assertEquals(prettifyQuery(expected), prettifyQuery(generated));
    }

    private static <T> void verify(Class<T> clazz, String expected) {
        verify(clazz, b -> b.conditions().build(), expected);
    }


    private static <T> void verify(Class<T> clazz, Function<QueryFactoryBuilder<T>, AbstractQueryFactory<T>> mutator, String expected) {
        verify(mutator.apply(builderForClass(clazz)), expected);
    }

    // makes IntelliJ's "show differences" view more useful in case of a mismatch
    private static String prettifyQuery(String text) {
        String intermediate = COLLAPSE_SPACE.matcher(text).replaceAll(" ");
        return PRETTY_PRINT_SPLIT.matcher(intermediate).replaceAll("\n $1 ");
    }
}
