package com.iitsm.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * Common identity for every persistent entity: a database-generated id, an optimistic-locking
 * version, and id-based equality (two objects are the same entity if they share an id).
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    public Long getId() {
        return id;
    }

    public boolean isNew() {
        return id == null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) {
            return false;
        }
        BaseEntity other = (BaseEntity) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        // Constant per class so the hash doesn't change when the id is assigned on save.
        return org.hibernate.Hibernate.getClass(this).hashCode();
    }
}
