/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jdbc.xa;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;


public class StatementInvocationHandler implements InvocationHandler
{

    private Statement statement;
    private ConnectionWrapper connectionWrapper;

    public StatementInvocationHandler(ConnectionWrapper connectionWrapper, Statement statement)
    {
        this.connectionWrapper = connectionWrapper;
        this.statement = statement;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     *      java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (method.getName().startsWith("execute"))
        {
            connectionWrapper.enlist();
        }
        try
        {
            return method.invoke(statement, args);
        }
        catch (InvocationTargetException ex)
        {
            throw ex.getCause();
        }
    }

}
