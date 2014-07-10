/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import java.sql.Connection;

import javax.sql.DataSource;

/**
 * Creates {@link Connection} from {@link DataSource}
 */
public interface ConnectionFactory
{

    /**
     * Creates a connection for a {@link DataSource}
     *
     * @param dataSource dataSource connecting to
     * @return a new {@link Connection}
     * @throws ConnectionCreationException in case of any problem creating the connection
     */
    Connection create(DataSource dataSource) throws ConnectionCreationException;
}
