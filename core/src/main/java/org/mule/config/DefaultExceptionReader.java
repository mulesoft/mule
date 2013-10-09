/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config;

import org.mule.api.config.ExceptionReader;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the default exception reader used if there is no specific one registered
 * for the current exception.
 */
public final class DefaultExceptionReader implements ExceptionReader
{

    private Map<?, ?> info = new HashMap<Object, Object>();

    public String getMessage(Throwable t)
    {
        return t.getMessage();
    }

    public Throwable getCause(Throwable t)
    {
        return t.getCause();
    }

    public Class<?> getExceptionType()
    {
        return Throwable.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @return a map of the non-stanard information stored on the exception
     */
    public Map<?, ?> getInfo(Throwable t)
    {
        return info;
    }
}
