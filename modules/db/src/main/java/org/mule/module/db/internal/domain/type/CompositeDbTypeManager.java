/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import org.mule.module.db.internal.domain.connection.DbConnection;

import java.util.List;

/**
 * Uses {@link DbTypeManager} instances to manage DB types
 */
public class CompositeDbTypeManager implements DbTypeManager
{

    private List<DbTypeManager> typeManagers;

    /**
     * Creates a composed DB type manager
     *
     * @param typeManagers sorted type managers used to resolve DB types.
     */
    public CompositeDbTypeManager(List<DbTypeManager> typeManagers)
    {
        this.typeManagers = typeManagers;
    }

    @Override

    /**
     * Finds a mapping for a given type ID and name using each composed
     * {@link DbTypeManager} until a type is found or there are no more
     * managers to use.
     *
     * @param connection connection used to connect to the database if required
     * @param id type ID
     * @param name type name
     * @return a type that corresponds to the given ID and name
     * @throws UnknownDbTypeException when there is no managed type with the given ID and name
     */
    public DbType lookup(DbConnection connection, int id, String name) throws UnknownDbTypeException
    {
        for (DbTypeManager typeManager : typeManagers)
        {
            try
            {
                return typeManager.lookup(connection, id, name);
            }
            catch (UnknownDbTypeException e)
            {
                // Ignore and continue
            }
        }

        throw new UnknownDbTypeException(id, name);
    }

    /**
     * Finds a mapping for a given type name using each composed
     * {@link DbTypeManager} until a type is found or there are no more
     * managers to use.
     *
     * @param connection connection used to connect to the database if required
     * @param name type name
     * @return a type that corresponds to the given name
     * @throws UnknownDbTypeException when there is no managed type with the given ID and name
     */
    @Override
    public DbType lookup(DbConnection connection, String name) throws UnknownDbTypeException
    {
        for (DbTypeManager typeManager : typeManagers)
        {
            try
            {
                return typeManager.lookup(connection, name);
            }
            catch (UnknownDbTypeException e)
            {
                // Ignore and continue
            }
        }

        throw new UnknownDbTypeException(name);
    }
}
