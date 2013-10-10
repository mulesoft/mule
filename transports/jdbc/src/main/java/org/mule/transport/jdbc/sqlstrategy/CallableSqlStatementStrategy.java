/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.sqlstrategy;

/**
 * Implements strategy for executing simple stored procedures.  Only IN parameters
 * can be used and no OUT values can be returned from the stored procedure.
 */
public  class CallableSqlStatementStrategy extends SimpleUpdateSqlStatementStrategy
{
    protected static final String STORED_PROCEDURE_PREFIX = "{ ";
    protected static final String STORED_PROCEDURE_SUFFIX = " }";
    
    @Override
    protected String escapeStatement(String statement)
    {
        return STORED_PROCEDURE_PREFIX  + statement + STORED_PROCEDURE_SUFFIX;
    }
}
