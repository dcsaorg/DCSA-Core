package org.dcsa.core.repository;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.mock.MockR2dbcDialect;
import org.dcsa.core.models.Address;
import org.dcsa.core.models.City;
import org.dcsa.core.models.Customer;
import org.dcsa.core.models.combined.*;
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
        Address address = new Address();
        address.setAddressId(2L);
        address.setCityId(3L);
        address.setAddress("customerAddress");
        customerWithAddress.setId(1L);
        customerWithAddress.setName("customerName");
        customerWithAddress.setAddress(address);
        customerWithAddress.setAddressId(address.getAddressId());

        StubRow row = StubRow.of(Map.of(
                "id", customerWithAddress.getId(),
                "name", customerWithAddress.getName(),
                "addressId", customerWithAddress.getAddressId(),
                "address.addressId", address.getAddressId(),
                "address.address", address.getAddress(),
                "address.cityId", address.getCityId()

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
                "deliveryAddress.addressId", 1L,
                "deliveryAddress.address", "deliveryAddress",
                "deliveryAddress.cityId", 2L,
                "paymentAddress.addressId", 3L,
                "paymentAddress.address", "paymentAddress",
                "paymentAddress.cityId", 4L,
                "id", 5L,
                "name", "customerName",
                "deliveryAddressId", 1L,
                "paymentAddressId", 3L
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
                Map.entry("customer.deliveryAddress.addressId", 1L),
                Map.entry("customer.deliveryAddress.address", "deliveryAddress"),
                Map.entry("customer.deliveryAddress.cityId", 2L),
                Map.entry("customer.paymentAddress.addressId", 3L),
                Map.entry("customer.paymentAddress.address", "paymentAddress"),
                Map.entry("customer.paymentAddress.cityId", 4L),
                Map.entry("customer.id", 5L),
                Map.entry("customer.name", "customerName"),
                Map.entry("customer.deliveryAddressId", 1L),
                Map.entry("customer.paymentAddressId", 3L),
                Map.entry("id", 6L),
                Map.entry("name", "customerBookName"),
                Map.entry("customerId", 5L)
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
                Map.entry("customer.deliveryAddress.addressId", 1L),
                Map.entry("customer.deliveryAddress.address", "deliveryAddress"),
                Map.entry("customer.deliveryAddress.cityId", 2L),
                Map.entry("customer.paymentAddress.addressId", 3L),
                Map.entry("customer.paymentAddress.address", "paymentAddress"),
                Map.entry("customer.paymentAddress.cityId", 4L),
                Map.entry("customer.id", 5L),
                Map.entry("customer.name", "customerName"),
                Map.entry("customer.deliveryAddressId", 1L),
                Map.entry("customer.paymentAddressId", 3L),
                Map.entry("city.id", "cityId"),
                Map.entry("city.name", "cityName"),
                Map.entry("city.countryId", 6L),
                Map.entry("id", 7L),
                Map.entry("name", "customerBookName"),
                Map.entry("cityId", "cityId"),
                Map.entry("customerId", 5L)
        ));
        List<ColumnMetadata> columnMetadatas = row.getStubColumnMetadatas();
        RowMetadata rowMetadata = StubRowMetadata.of(columnMetadatas);

        rowMap(row, rowMetadata, CityCustomerBook.class, extendedParameters).verify(cityCustomerBook);
    }

    @Test
    public void testOrderWithCustomerAndAddresses() {
        OrderWithCustomerAndAddresses orderWithCustomerAndAddresses = new OrderWithCustomerAndAddresses();
        Address customerAddress = new Address();
        customerAddress.setAddressId(2L);
        customerAddress.setAddress("customer street 10");
        customerAddress.setCityId(11L);
        Address warehouseAddress = new Address();
        warehouseAddress.setAddressId(7L);
        warehouseAddress.setAddress("warehouse street 20");
        warehouseAddress.setCityId(13L);
        Customer customer = new Customer();
        customer.setId(3L);
        customer.setAddressId(customerAddress.getAddressId());
        customer.setName("customerName");
        orderWithCustomerAndAddresses.setId(17L);
        orderWithCustomerAndAddresses.setOrderline("orderLine");
        orderWithCustomerAndAddresses.setReceiverId(customer.getAddressId());
        orderWithCustomerAndAddresses.setCustomerAddress(customerAddress);
        orderWithCustomerAndAddresses.setCustomer(customer);
        orderWithCustomerAndAddresses.setWarehouseAddress(warehouseAddress);
        orderWithCustomerAndAddresses.setWarehouseAddressId(warehouseAddress.getAddressId());
        orderWithCustomerAndAddresses.setDeliveryDate(OffsetDateTime.now());

        StubRow row = StubRow.of(Map.ofEntries(
                Map.entry("id", orderWithCustomerAndAddresses.getId()),
                Map.entry("orderline", orderWithCustomerAndAddresses.getOrderline()),
                Map.entry("receiverId", orderWithCustomerAndAddresses.getReceiverId()),
                Map.entry("warehouseAddressId", orderWithCustomerAndAddresses.getWarehouseAddressId()),
                Map.entry("deliveryDate", orderWithCustomerAndAddresses.getDeliveryDate()),

                Map.entry("customer.id", customer.getId()),
                Map.entry("customer.name", customer.getName()),
                Map.entry("customer.addressId", customer.getAddressId()),

                Map.entry("customerAddress.addressId", customerAddress.getAddressId()),
                Map.entry("customerAddress.address", customerAddress.getAddress()),
                Map.entry("customerAddress.cityId", customerAddress.getCityId()),

                Map.entry("warehouse.addressId", warehouseAddress.getAddressId()),
                Map.entry("warehouse.address", warehouseAddress.getAddress()),
                Map.entry("warehouse.cityId", warehouseAddress.getCityId())
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

        StubRow row = StubRow.of(Map.ofEntries(
                Map.entry("id", 1L),
                Map.entry("orderline", "orderLine"),
                Map.entry("receiverId", 2L),
                Map.entry("warehouseAddressId", 3L),
                Map.entry("deliveryDate", OffsetDateTime.parse("2021-05-18T15:20:30+01:00"))
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
