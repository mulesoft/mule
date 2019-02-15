/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection.type.resolver;

import org.mule.module.db.internal.domain.type.ResolvedDbType;

import java.sql.SQLException;

public interface TypeResolver
{
    void resolveLobs(Object[] elements, Integer index, String dataTypeName) throws SQLException;

    String resolveType(String typeName) throws SQLException;

    void resolveLobIn(Object[] attributes, Integer key, ResolvedDbType resolvedDbType) throws SQLException;
}
