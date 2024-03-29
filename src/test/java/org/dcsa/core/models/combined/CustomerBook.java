package org.dcsa.core.models.combined;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.dcsa.core.model.ForeignKey;
import org.dcsa.core.models.AbstractCustomerBook;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table("customer_book_table")
public class CustomerBook extends AbstractCustomerBook {
    @Id
    @Column("customer_book_id")
    private Long id;

    @Column("customer_book_name")
    private String name;

    @Column("customer_id")
    @ForeignKey(into="customer", foreignFieldName="id")
    private Long customerId;

    @Transient
    private CustomerWithForeignKeyAddresses customer;
}
