package com.company.ai.help.desk.entity;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * Custom Hibernate UserType for pgvector PGvector type.
 * Handles proper serialization/deserialization between Java PGvector and PostgreSQL vector.
 */
public class PGvectorType implements UserType<PGvector> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<PGvector> returnedClass() {
        return PGvector.class;
    }

    @Override
    public boolean equals(PGvector x, PGvector y) {
        if (x == null && y == null) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return Arrays.equals(x.toArray(), y.toArray());
    }

    @Override
    public int hashCode(PGvector x) {
        return x == null ? 0 : Arrays.hashCode(x.toArray());
    }

    @Override
    public PGvector nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        String value = rs.getString(position);
        if (rs.wasNull() || value == null) {
            return null;
        }
        try {
            return new PGvector(value);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create PGvector from value: " + value, e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, PGvector value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, value);
        }
    }

    @Override
    public PGvector deepCopy(PGvector value) {
        if (value == null) {
            return null;
        }
        return new PGvector(value.toArray());
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(PGvector value) {
        if (value == null) {
            return null;
        }
        // Return the string representation for serialization
        return value.toString();
    }

    @Override
    public PGvector assemble(Serializable cached, Object owner) {
        if (cached == null) {
            return null;
        }
        try {
            return new PGvector((String) cached);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to assemble PGvector from cached value", e);
        }
    }

    @Override
    public PGvector replace(PGvector detached, PGvector managed, Object owner) {
        return deepCopy(detached);
    }
}

