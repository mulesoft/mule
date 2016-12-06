/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Struct;

/**
 * Defines a structured data type for {@link Struct} mapped to a Java class.
 */
public class MappedStructResolvedDbType<T> extends AbstractStructuredDbType
{

    private final Class<T> mappedClass;

    /**
     * Creates a new instance
     *
     * @param id          identifier for the type
     * @param name        type name. Non Empty.
     * @param mappedClass class used to map values of this type from the database to Java representations.
     */
    public MappedStructResolvedDbType(int id, String name, Class<T> mappedClass)
    {
        super(id, name);
        this.mappedClass = mappedClass;
    }

    public Class<T> getMappedClass()
    {
        return mappedClass;
    }

    @Override
    public T getParameterValue(CallableStatement statement, int index) throws SQLException
    {
        return (T) statement.getObject(index);
    }
}
