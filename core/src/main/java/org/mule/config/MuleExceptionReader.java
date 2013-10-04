/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.api.MuleException;
import org.mule.api.config.ExceptionReader;

import java.util.Collections;
import java.util.Map;

/**
 * Grabs all information from the MuleException type
 */
public final class MuleExceptionReader implements ExceptionReader
{

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
        return MuleException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @return a map of the non-stanard information stored on the exception
     */
    public Map<?, ?> getInfo(Throwable t)
    {
        return (t instanceof MuleException ? ((MuleException) t).getInfo() : Collections.EMPTY_MAP);
    }

}
