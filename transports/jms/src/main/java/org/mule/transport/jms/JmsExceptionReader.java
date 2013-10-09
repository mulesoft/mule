/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import org.mule.api.config.ExceptionReader;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

/**
 * This reader will ensure that the LinkedException and JMS code is not lost when
 * printing the JMSException.
 */
public class JmsExceptionReader implements ExceptionReader
{

    public String getMessage(Throwable t)
    {
        JMSException e = (JMSException)t;
        return e.getMessage() + "(JMS Code: " + e.getErrorCode() + ")";
    }

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

    public Class<?> getExceptionType()
    {
        return JMSException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @return a map of the non-stanard information stored on the exception
     */
    public Map<?, ?> getInfo(Throwable t)
    {
        JMSException e = (JMSException)t;
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("JMS Code", e.getErrorCode());
        return info;
    }

}
