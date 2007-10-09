/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.direct;

import org.mule.impl.MuleMessage;
import org.mule.impl.model.AbstractComponent;
import org.mule.impl.model.MuleProxy;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;

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
        catch (UMOException e)
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

    protected UMOMessage doSend(UMOEvent event) throws UMOException
    {

        Object obj = proxy.onCall(event);
        if (obj instanceof UMOMessage)
        {
            return (UMOMessage) obj;
        }
        else
        {
            return new MuleMessage(obj, event.getMessage());
        }
    }

    protected void doDispatch(UMOEvent event) throws UMOException
    {
        proxy.onCall(event);
    }

    protected void doStop() throws UMOException
    {
        proxy.stop();
    }

    protected void doStart() throws UMOException
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
