/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.config.ExceptionReader;

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

    public Class getExceptionType()
    {
        return JMSException.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @return a map of the non-stanard information stored on the exception
     */
    public Map getInfo(Throwable t)
    {
        JMSException e = (JMSException)t;
        Map info = new HashMap();
        info.put("JMS Code", e.getErrorCode());
        return info;
    }

}
