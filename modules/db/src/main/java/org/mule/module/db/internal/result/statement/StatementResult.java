/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

/**
 * Represents a single result from a {@link java.sql.Statement} execution
 */
public interface StatementResult
{

    /**
     * @return name of the result. Non null
     */
    String getName();


    /**
     * @return value for the result. Can be null.
     */
    Object getResult();
}
