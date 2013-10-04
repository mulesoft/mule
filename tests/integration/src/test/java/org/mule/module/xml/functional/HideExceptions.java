/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

