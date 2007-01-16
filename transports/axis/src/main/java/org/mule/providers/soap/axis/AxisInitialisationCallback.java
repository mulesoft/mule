/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.impl.InitialisationCallback;
import org.mule.umo.lifecycle.InitialisationException;

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
