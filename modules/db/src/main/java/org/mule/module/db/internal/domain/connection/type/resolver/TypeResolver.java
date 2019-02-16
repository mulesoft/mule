/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection.type.resolver;

import org.mule.module.db.internal.domain.type.ResolvedDbType;

import java.sql.SQLException;

/**
 * Resolve data type of the struct and arrays elements
 *
 * @since 3.10.0
 */
public interface TypeResolver
{
    /**
     * This method changes {@param elements} entity replacing
     * some elements with clob or blob instances.
     *
     * @param  elements of the array or struct
     * @param  index of each element to be replaced with an clob or blob instance
     * @param  dataTypeName of the attribute that is going to be replaced
     * @throws SQLException when there is an error creating a clob or clob
     */
    void resolveLobs(Object[] elements, Integer index, String dataTypeName) throws SQLException;

    /**
     * This method resolves the attribute data type.
     *
     * @param  typeName of the array or struct
     * @return the attribute data type
     * @throws SQLException when there is an error obtaining the data type from database
     */
    String resolveType(String typeName) throws SQLException;

    /**
     * This method calls the method that instantiates clob and blob classes
     * according to {@param attributes} structure.
     * This method produces side effect in the array or struct.
     *
     * @param  attributes of the array or struct
     * @param  index of the array or struct
     * @param  resolvedDbType of the array or struct
     * @throws SQLException when there is an error creating a clob or clob
     */
    void resolveLobIn(Object[] attributes, Integer index, ResolvedDbType resolvedDbType) throws SQLException;
}
