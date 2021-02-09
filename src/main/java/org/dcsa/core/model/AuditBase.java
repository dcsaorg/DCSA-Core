package org.dcsa.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.relational.core.mapping.Column;
import reactor.util.annotation.NonNull;

import java.util.Optional;

@EqualsAndHashCode
public abstract class AuditBase implements AuditorAware<String> {

    @NonNull
    @CreatedDate
    @Column("created_date") //, updatable = false
    @Transient
    private long createdDate;

    @NonNull
    @Column("modified_date")
    @LastModifiedDate
    @Transient
    private long modifiedDate;

    @Column("created_by")
    @CreatedBy
    @Transient
    private String createdBy;

    @Column("modified_by")
    @LastModifiedBy
    @Transient
    private String modifiedBy;

    @Override
    @JsonIgnore
    public Optional<String> getCurrentAuditor() {
        // Get current user
        return Optional.empty();
    }
}
