/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.work;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default exception Handler used when executing work in the work manager
 */
public class DefaultWorkListener implements WorkListener
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void workAccepted(WorkEvent event)
    {
        handleWorkException(event, "workAccepted");
    }

    public void workRejected(WorkEvent event)
    {
        handleWorkException(event, "workRejected");
    }

    public void workStarted(WorkEvent event)
    {
        handleWorkException(event, "workStarted");
    }

    public void workCompleted(WorkEvent event)
    {
        handleWorkException(event, "workCompleted");
    }

    protected void handleWorkException(WorkEvent event, String type)
    {
        Throwable e;

        if (event != null && event.getException() != null)
        {
            e = event.getException();
        }
        else
        {
            return;
        }

        if (event.getException().getCause() != null)
        {
            e = event.getException().getCause();
        }

        logger.error("Work caused exception on '" + type + "'. Work being executed was: "
                     + event.getWork().toString());
        logger.error(e);
    }
}
