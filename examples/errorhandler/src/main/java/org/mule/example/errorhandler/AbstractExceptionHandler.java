/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.errorhandler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>AbstractExceptionListener</code> TODO (document class)
 *
 */
public abstract class AbstractExceptionHandler implements ExceptionHandler
{
    protected Set<Class<? extends Throwable>> registry = new HashSet<Class<? extends Throwable>>();

    private String endpointName;

    protected ErrorManager errorManager = null;

    @Override
    public void registerException(Class<? extends Throwable> exceptionClass)
    {
        registry.add(exceptionClass);
    }

    @Override
    public Iterator<Class<? extends Throwable>> getRegisteredClasses()
    {
        return registry.iterator();
    }

    @Override
    public void unRegisterException(Class<? extends Throwable> exceptionClass)
    {
        registry.remove(exceptionClass);
    }

    @Override
    public boolean isRegisteredFor(Class<? extends Throwable> exceptionClass)
    {
        Class<? extends Throwable> aClass = null;
        for (Iterator<Class<? extends Throwable>> i = getRegisteredClasses(); i.hasNext();)
        {
            aClass = i.next();
            if (aClass.isAssignableFrom(exceptionClass))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onException(ErrorMessage message) throws HandlerException
    {
        Throwable t = null;

        try
        {
            t = message.getException().toException();
        }
        catch (Exception e)
        {
            throw new HandlerException(LocaleMessage.unretrievedException(e), e);
        }

        if (!isRegisteredFor(t.getClass()))
        {
            throw new HandlerException(LocaleMessage.unhandledException(t.getClass(), this.getClass()));
        }
        processException(message, t);
    }

    protected abstract void processException(ErrorMessage message, Throwable t) throws HandlerException;

    @Override
    public ErrorManager getErrorManager()
    {
        return errorManager;
    }

    @Override
    public void setErrorManager(ErrorManager errorManager)
    {
        this.errorManager = errorManager;
    }

    public String getEndpointName()
    {
        return endpointName;
    }

    public void setEndpointName(String endpointName)
    {
        this.endpointName = endpointName;
    }
}
