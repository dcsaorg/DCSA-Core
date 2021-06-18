package org.dcsa.core.models.combined;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.dcsa.core.model.ForeignKey;
import org.dcsa.core.models.AbstractCustomerBook;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("customer_book_table")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CustomerBook extends AbstractCustomerBook {

    // Shadows super class field deliberately to test that this does not cause issue.
    @Column("customer_id")
    @ForeignKey(into="customer", foreignFieldName="id")
    private Long customerId;

    @Transient
    private CustomerWithForeignKeyAddresses customer;
}
