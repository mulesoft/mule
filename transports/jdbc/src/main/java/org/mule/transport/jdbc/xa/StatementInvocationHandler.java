/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.xa;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;


public class StatementInvocationHandler implements InvocationHandler
{

    private Statement statement;

    public StatementInvocationHandler(ConnectionWrapper connectionWrapper, Statement statement)
    {
        this.statement = statement;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
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
