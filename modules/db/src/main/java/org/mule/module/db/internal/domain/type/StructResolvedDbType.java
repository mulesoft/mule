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
 * Defines a structured data type for {@link Struct}
 */
public class StructResolvedDbType extends ResolvedDbType
{

    private final Class<?> mappedClass;

    /**
     * Creates a new instance
     *
     * @param id identifier for the type
     * @param name type name. Non Empty.
     * @param mappedClass fully qualified name of the class used to map values of this type
     *                    from the database to Java representations.
     */
    public StructResolvedDbType(int id, String name, Class<?> mappedClass)
    {
        super(id, name);
        this.mappedClass = mappedClass;
    }

    public Class<?> getMappedClass()
    {
        return mappedClass;
    }

    @Override
    public Object getParameterValue(CallableStatement statement, int index) throws SQLException
    {
        if (mappedClass == null)
        {
            return statement.getObject(index);
        }
        else
        {
            return statement.getObject(index, mappedClass);
        }
    }
}
