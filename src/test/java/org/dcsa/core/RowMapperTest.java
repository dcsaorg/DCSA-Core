package org.dcsa.core;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.mock.MockR2dbcDialect;
import org.dcsa.core.models.Address;
import org.dcsa.core.models.City;
import org.dcsa.core.models.combined.*;
import org.dcsa.core.repository.RowMapper;
import org.dcsa.core.stub.StubRow;
import org.dcsa.core.stub.StubRowMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SpringBootTest(properties = {
        "sort.sortName=sort",
        "search.queryParameterAttributeHandling=PARAMETER_NAME_ARRAY_NOTATION"
})
@ContextConfiguration(classes = ExtendedParameters.class)
public class RowMapperTest {

    @Autowired
    private ExtendedParameters extendedParameters;

    @Test
    public void testCustomerWithAddress() {
        CustomerWithAddress customerWithAddress = new CustomerWithAddress();
        customerWithAddress.setId(1L);
        customerWithAddress.setName("customerName");
        customerWithAddress.setAddress("customerAddress");

        StubRow row = StubRow.of(Map.of(
                "id", 1L,
                "name", "customerName",
                "address", "customerAddress"
        ));
        List<ColumnMetadata> columnMetadatas = row.getStubColumnMetadatas();
        RowMetadata rowMetadata = StubRowMetadata.of(columnMetadatas);

        rowMap(row, rowMetadata, CustomerWithAddress.class, extendedParameters).verify(customerWithAddress);
    }

    @Test
    public void testCustomerWithForeignKeyAddresses() {
        CustomerWithForeignKeyAddresses customer = new CustomerWithForeignKeyAddresses();
        Address deliveryAddress = new Address();
        Address paymentAddress = new Address();
        deliveryAddress.setAddressId(1L);
        deliveryAddress.setAddress("deliveryAddress");
        deliveryAddress.setCityId(2L);
        paymentAddress.setAddressId(3L);
        paymentAddress.setAddress("paymentAddress");
        paymentAddress.setCityId(4L);
        customer.setId(5L);
        customer.setName("customerName");
        customer.setDeliveryAddressId(1L);
        customer.setDeliveryAddress(deliveryAddress);
        customer.setPaymentAddressId(3L);
        customer.setPaymentAddress(paymentAddress);

        StubRow row = StubRow.of(Map.of(
                "deliveryAddress.address_id", 1L,
                "deliveryAddress.street_name", "deliveryAddress",
                "deliveryAddress.city_id", 2L,
                "paymentAddress.address_id", 3L,
                "paymentAddress.street_name", "paymentAddress",
                "paymentAddress.city_id", 4L,
                "customer_id", 5L,
                "customer_name", "customerName",
                "delivery_address_id", 1L,
                "payment_address_id", 3L
        ));
        List<ColumnMetadata> columnMetadatas = row.getStubColumnMetadatas();
        RowMetadata rowMetadata = StubRowMetadata.of(columnMetadatas);

        rowMap(row, rowMetadata, CustomerWithForeignKeyAddresses.class, extendedParameters).verify(customer);
    }

    @Test
    public void testCustomerBook() {
        CustomerBook customerBook = new CustomerBook();
        CustomerWithForeignKeyAddresses customer = new CustomerWithForeignKeyAddresses();
        Address deliveryAddress = new Address();
        Address paymentAddress = new Address();
        deliveryAddress.setAddressId(1L);
        deliveryAddress.setAddress("deliveryAddress");
        deliveryAddress.setCityId(2L);
        paymentAddress.setAddressId(3L);
        paymentAddress.setAddress("paymentAddress");
        paymentAddress.setCityId(4L);
        customer.setId(5L);
        customer.setName("customerName");
        customer.setDeliveryAddressId(1L);
        customer.setDeliveryAddress(deliveryAddress);
        customer.setPaymentAddressId(3L);
        customer.setPaymentAddress(paymentAddress);
        customerBook.setId(6L);
        customerBook.setName("customerBookName");
        customerBook.setCustomerId(5L);
        customerBook.setCustomer(customer);

        StubRow row = StubRow.of(Map.ofEntries(
                Map.entry("customer.deliveryAddress.address_id", 1L),
                Map.entry("customer.deliveryAddress.street_name", "deliveryAddress"),
                Map.entry("customer.deliveryAddress.city_id", 2L),
                Map.entry("customer.paymentAddress.address_id", 3L),
                Map.entry("customer.paymentAddress.street_name", "paymentAddress"),
                Map.entry("customer.paymentAddress.city_id", 4L),
                Map.entry("customer.customer_id", 5L),
                Map.entry("customer.customer_name", "customerName"),
                Map.entry("customer.delivery_address_id", 1L),
                Map.entry("customer.payment_address_id", 3L),
                Map.entry("customer_book_id", 6L),
                Map.entry("customer_book_name", "customerBookName"),
                Map.entry("customer_id", 5L)
        ));
        List<ColumnMetadata> columnMetadatas = row.getStubColumnMetadatas();
        RowMetadata rowMetadata = StubRowMetadata.of(columnMetadatas);

        rowMap(row, rowMetadata, CustomerBook.class, extendedParameters).verify(customerBook);
    }

    @Test
    public void testCityCustomerBook() {
        CityCustomerBook cityCustomerBook = new CityCustomerBook();
        CustomerWithForeignKeyAddresses customer = new CustomerWithForeignKeyAddresses();
        Address deliveryAddress = new Address();
        Address paymentAddress = new Address();
        City city = new City();
        deliveryAddress.setAddressId(1L);
        deliveryAddress.setAddress("deliveryAddress");
        deliveryAddress.setCityId(2L);
        paymentAddress.setAddressId(3L);
        paymentAddress.setAddress("paymentAddress");
        paymentAddress.setCityId(4L);
        customer.setId(5L);
        customer.setName("customerName");
        customer.setDeliveryAddressId(1L);
        customer.setDeliveryAddress(deliveryAddress);
        customer.setPaymentAddressId(3L);
        customer.setPaymentAddress(paymentAddress);
        city.setId("cityId");
        city.setName("cityName");
        city.setCountryId(6L);
        cityCustomerBook.setId(7L);
        cityCustomerBook.setName("customerBookName");
        cityCustomerBook.setCustomerId(5L);
        cityCustomerBook.setCustomer(customer);
        cityCustomerBook.setCityId("cityId");
        cityCustomerBook.setCity(city);

        StubRow row = StubRow.of(Map.ofEntries(
                Map.entry("customer.deliveryAddress.address_id", 1L),
                Map.entry("customer.deliveryAddress.street_name", "deliveryAddress"),
                Map.entry("customer.deliveryAddress.city_id", 2L),
                Map.entry("customer.paymentAddress.address_id", 3L),
                Map.entry("customer.paymentAddress.street_name", "paymentAddress"),
                Map.entry("customer.paymentAddress.city_id", 4L),
                Map.entry("customer.customer_id", 5L),
                Map.entry("customer.customer_name", "customerName"),
                Map.entry("customer.delivery_address_id", 1L),
                Map.entry("customer.payment_address_id", 3L),
                Map.entry("city.id", "cityId"),
                Map.entry("city.city_name", "cityName"),
                Map.entry("city.country_id", 6L),
                Map.entry("customer_book_id", 7L),
                Map.entry("customer_book_name", "customerBookName"),
                Map.entry("city_id", "cityId"),
                Map.entry("customer_id", 5L)
        ));
        List<ColumnMetadata> columnMetadatas = row.getStubColumnMetadatas();
        RowMetadata rowMetadata = StubRowMetadata.of(columnMetadatas);

        rowMap(row, rowMetadata, CityCustomerBook.class, extendedParameters).verify(cityCustomerBook);
    }

    @Test
    public void testOrderWithCustomerAndAddresses() {
        OrderWithCustomerAndAddresses orderWithCustomerAndAddresses = new OrderWithCustomerAndAddresses();
        orderWithCustomerAndAddresses.setOrderline("orderLine");
        orderWithCustomerAndAddresses.setCustomerName("customerName");
        orderWithCustomerAndAddresses.setWarehouseAddress("warehouseAddress");

        StubRow row = StubRow.of(Map.ofEntries(
                Map.entry("orderline", "orderLine"),
                Map.entry("customerName", "customerName"),
                Map.entry("warehouseAddress", "warehouseAddress")
        ));
        List<ColumnMetadata> columnMetadatas = row.getStubColumnMetadatas();
        RowMetadata rowMetadata = StubRowMetadata.of(columnMetadatas);

        rowMap(row, rowMetadata, OrderWithCustomerAndAddresses.class, extendedParameters).verify(orderWithCustomerAndAddresses);
    }

    @Test
    public void testExtendedOrder() {
        ExtendedOrder extendedOrder = new ExtendedOrder();
        extendedOrder.setId(1L);
        extendedOrder.setOrderline("orderLine");
        extendedOrder.setReceiverId(2L);
        extendedOrder.setWarehouseAddressId(3L);
        extendedOrder.setDeliveryDate(OffsetDateTime.parse("2021-05-18T15:20:30+01:00"));
        extendedOrder.setWarehouseAddress("warehouseAddress");

        StubRow row = StubRow.of(Map.ofEntries(
                Map.entry("order_id", 1L),
                Map.entry("orderline", "orderLine"),
                Map.entry("customer_id", 2L),
                Map.entry("address_id", 3L),
                Map.entry("delivery_date", OffsetDateTime.parse("2021-05-18T15:20:30+01:00")),
                Map.entry("warehouseAddress", "warehouseAddress")
        ));
        List<ColumnMetadata> columnMetadatas = row.getStubColumnMetadatas();
        RowMetadata rowMetadata = StubRowMetadata.of(columnMetadatas);

        rowMap(row, rowMetadata, ExtendedOrder.class, extendedParameters).verify(extendedOrder);
    }

    private static <T> RowMapperTest.RowMapVerifier<T> rowMap(Row row, RowMetadata rowMetadata, Class<T> clazz, ExtendedParameters extendedParameters) {
        ExtendedRequest<T> extendedRequest = new ExtendedRequest<>(extendedParameters, new MockR2dbcDialect(), clazz);
        return new RowMapVerifier<>(row, rowMetadata, extendedRequest);
    }

    @RequiredArgsConstructor
    private static class RowMapVerifier<T> {
        private final Row row;
        private final RowMetadata rowMetadata;

        private final ExtendedRequest<T> request;
        private final RowMapper rowMapper = new RowMapper();

        public void verify(T expected, Consumer<ExtendedRequest<T>> requestMutator) {
            T actual;

            request.resetParameters();

            if (requestMutator != null) {
                requestMutator.accept(request);
            }

            actual = rowMapper.mapRow(row, rowMetadata, request.getDbEntityAnalysis(), request.getModelClass(),
                    request.ignoreUnknownProperties());
            Assertions.assertEquals(expected, actual);
        }

        public void verify(T expected) {
            this.verify(expected, null);
        }
    }
}
