/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model.direct;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.MuleProxy;
import org.mule.component.AbstractComponent;

import java.util.List;

/**
 * A direct component invokes the service component directly without any threading, 
 * even when the invocation is asynchronous
 */
public class DirectComponent extends AbstractComponent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8590955440156945732L;

    protected List interceptorList = null;
    protected MuleProxy proxy;
    protected Object pojoService;

    public DirectComponent()
    {
        super();
    }

    protected void doInitialise() throws InitialisationException
    {
        try
        {
            pojoService = getOrCreateService();
            proxy = createComponentProxy(pojoService);
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected void doDispose()
    {
        try
        {
            serviceFactory.release(pojoService);
        }
        catch (Exception e)
        {
            logger.warn(e);
        }
        
        //proxy.dispose();
    }

    protected MuleMessage doSend(MuleEvent event) throws MuleException
    {

        Object obj = proxy.onCall(event);
        if (obj instanceof MuleMessage)
        {
            return (MuleMessage) obj;
        }
        else
        {
            return new DefaultMuleMessage(obj, event.getMessage());
        }
    }

    protected void doDispatch(MuleEvent event) throws MuleException
    {
        proxy.onCall(event);
    }

    protected void doStop() throws MuleException
    {
        proxy.stop();
    }

    protected void doStart() throws MuleException
    {
        proxy.start();
    }

    protected void doPause()
    {
        proxy.suspend();
    }

    protected void doResume()
    {
        proxy.resume();
    }
}
