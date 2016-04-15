/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import org.mule.module.db.internal.domain.connection.DbConnection;

/**
 * Manages types for a database instance
 */
public interface DbTypeManager
{

    /**
     * Finds a mapping for a given type ID and name
     *
     * @param connection connection used to connect to the database if required
     * @param id type ID
     * @param name type name
     * @return a type that corresponds to the given ID and name
     * @throws UnknownDbTypeException when there is no managed type with the given ID and name
     */
    DbType lookup(DbConnection connection, int id, String name) throws UnknownDbTypeException;

    /**
     * Finds a mapping for a given type name
     *
     * @param connection connection used to connect to the database if required
     * @param id type ID
     * @param name type name
     * @return a type that corresponds to the given name
     * @throws UnknownDbTypeException when there is no managed type with the given name
     */
    DbType lookup(DbConnection connection, String name) throws UnknownDbTypeException;
}
