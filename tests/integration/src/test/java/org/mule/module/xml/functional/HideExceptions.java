/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HideExceptions implements MessagingExceptionHandler
{
    protected transient Log logger = LogFactory.getLog(getClass());

    public MuleEvent handleException(Exception exception, MuleEvent event)
    {
        logger.debug("Hiding exception: " + exception);
        logger.debug("(see config for test - some exceptions expected)");
        return null;
    }

}

