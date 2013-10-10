/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;

import org.apache.axis.handlers.soap.SOAPService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AxisInitialisationCallback</code> is invoked when an Axis component is
 * created from its descriptor.
 */
public class AxisInitialisationCallback implements InitialisationCallback
{
    protected static final Log logger = LogFactory.getLog(AxisInitialisationCallback.class);

    private SOAPService service;
    private boolean invoked = false;

    public AxisInitialisationCallback(SOAPService service)
    {
        this.service = service;
    }

    public void initialise(Object component) throws InitialisationException
    {
        // only call this once
        if (invoked)
        {
            return;
        }
        if (component instanceof AxisInitialisable)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Calling axis initialisation for component: " + component.getClass().getName());
            }
            ((AxisInitialisable)component).initialise(service);
        }
        invoked = true;
    }
}
