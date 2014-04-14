/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import org.mule.module.db.internal.domain.connection.DbConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a way to statically resolve {@link DbType} using a predefined
 * set of types.
 */
public class StaticDbTypeManager implements DbTypeManager
{

    private Map<String, DbType> vendorTypes = new HashMap<String, DbType>();

    public StaticDbTypeManager(List<DbType> vendorTypes)
    {
        for (DbType vendorType : vendorTypes)
        {
            this.vendorTypes.put(vendorType.getName(), vendorType);
        }
    }

    @Override
    public DbType lookup(DbConnection connection, int id, String name) throws UnknownDbTypeException
    {
        throw new UnknownDbTypeException(id, name);
    }

    @Override
    public DbType lookup(DbConnection connection, String name) throws UnknownDbTypeException
    {
        if (vendorTypes.containsKey(name))
        {
            return vendorTypes.get(name);
        }
        else
        {
            throw new UnknownDbTypeException(name);
        }
    }
}
