/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.component.simple;

import org.mule.api.MuleEventContext;
import org.mule.api.component.simple.LogService;
import org.mule.api.lifecycle.Callable;
import org.mule.util.StringMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>LogComponent</code> simply logs the content (or content length if it is a
 * large message)
 */
public class LogComponent implements Callable, LogService
{
    private static Log logger = LogFactory.getLog(LogComponent.class);

    public Object onCall(MuleEventContext context) throws Exception
    {
        String contents = context.getMessageAsString();
        String msg = "Message received in service: " + context.getFlowConstruct().getName();
        msg = StringMessageUtils.getBoilerPlate(msg + ". Content is: '"
                        + StringMessageUtils.truncate(contents, 100, true) + "'");
        log(msg);
        return context.getMessage();
    }

    public void log(String message)
    {
        logger.info(message);
    }
}
