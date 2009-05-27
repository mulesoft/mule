/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.transport.jdbc.sqlstrategy;

/**
 * Implements strategy for executing simple stored procedures.  Only IN parameters
 * can be used and no OUT values can be returned from the stored procedure.
 * 
 */

public  class CallableSqlStatementStrategy extends SimpleUpdateSqlStatementStrategy
{
    protected static final String STORED_PROCEDURE_PREFIX = "{ ";
    protected static final String STORED_PROCEDURE_SUFFIX = " }";
    
    protected String escapeStatement(String statement)
    {
        return STORED_PROCEDURE_PREFIX  + statement + STORED_PROCEDURE_SUFFIX;
    }
}
