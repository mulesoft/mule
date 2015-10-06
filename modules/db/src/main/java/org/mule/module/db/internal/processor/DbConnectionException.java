/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import org.mule.api.LocatedMuleException;
import org.mule.module.db.internal.domain.database.DbConfig;

import java.sql.SQLException;

/**
 * <code>DbConnectionException</code> is an exception thrown when an error connecting to a DB occurs, regardless of the message.
 *
 */
public class DbConnectionException extends LocatedMuleException
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = 8687956514569428383L;

    public DbConnectionException(SQLException e, DbConfig dbConfig)
    {
        super(e, dbConfig);
    }

}
