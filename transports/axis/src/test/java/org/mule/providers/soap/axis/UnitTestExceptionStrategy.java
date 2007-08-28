/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.umo.UMOMessage;

import java.util.ArrayList;
import java.util.List;

public class UnitTestExceptionStrategy extends DefaultComponentExceptionStrategy
{
    /**
     * Record all exceptions that this ExceptionStrategy handles so Unit Test
     * can query them and make their assertions.
     */
    private List messagingExceptions = null;
    
    public UnitTestExceptionStrategy()
    {
        super();
        messagingExceptions = new ArrayList();
    }
    
    protected void logFatal(UMOMessage message, Throwable t)
    {
        logger.debug("logFatal", t);
    }

    protected void logException(Throwable t)
    {
        logger.debug("logException", t);
    }

    public void handleMessagingException(UMOMessage message, Throwable t)
    {
        messagingExceptions.add(t);
        super.handleMessagingException(message, t);
    }
    
    public List getMessagingExceptions()
    {
        return messagingExceptions;
    }
}


