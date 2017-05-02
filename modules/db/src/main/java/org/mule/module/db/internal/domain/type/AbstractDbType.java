/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * Base class for {@link DbType}
 */
public abstract class AbstractDbType implements DbType
{

    protected final int id;
    protected final String name;

    public AbstractDbType(int id, String name)
    {
        this.id = id;
        this.name = name;
    }

    @Override
    public int getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void registerOutParameter(CallableStatement statement, int index) throws SQLException
    {
        statement.registerOutParameter(index, id);
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (!AbstractDbType.class.isAssignableFrom(obj.getClass()))
        {
            return false;
        }
        AbstractDbType other = (AbstractDbType) obj;
        if (id != other.id)
        {
            return false;
        }
        if (name == null)
        {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
        {
            return false;
        }
        return true;
    }    
}
