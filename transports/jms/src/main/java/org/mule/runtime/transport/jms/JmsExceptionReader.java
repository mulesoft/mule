/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.config.ExceptionReader;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

/**
 * This reader will ensure that the LinkedException and JMS code is not lost when
 * printing the JMSException.
 */
public class JmsExceptionReader implements ExceptionReader
{

    @Override
    public String getMessage(Throwable t)
    {
        JMSException e = (JMSException)t;
        return e.getMessage() + "(JMS Code: " + e.getErrorCode() + ")";
    }

    @Override
    public Throwable getCause(Throwable t)
    {
        JMSException e = (JMSException)t;
        Throwable cause = e.getLinkedException();
        if (cause == null)
        {
            cause = e.getCause();
        }
        return cause;
    }

    @Override
    public Class<?> getExceptionType()
    {
        return JMSException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @return a map of the non-stanard information stored on the exception
     */
    @Override
    public Map<?, ?> getInfo(Throwable t)
    {
        JMSException e = (JMSException)t;
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("JMS Code", e.getErrorCode());
        return info;
    }

}
