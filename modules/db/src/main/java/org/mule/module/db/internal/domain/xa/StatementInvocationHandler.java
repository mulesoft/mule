/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.xa;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;


/**
 * Invocation handler for statements
 */
public class StatementInvocationHandler implements InvocationHandler
{

    private final Statement statement;

    public StatementInvocationHandler(Statement statement)
    {
        this.statement = statement;
    }

    @Override
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
