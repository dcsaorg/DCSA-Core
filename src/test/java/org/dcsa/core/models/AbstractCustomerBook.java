package org.dcsa.core.models;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;

@Data
public abstract class AbstractCustomerBook {
    @Column("customer_book_id")
    private Long id;

    @Column("customer_book_name")
    private String name;

    @Column("customer_id")
    private Long customerId;

}
