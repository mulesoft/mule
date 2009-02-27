/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.rmi;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.AbstractMessageDispatcher;

import java.lang.reflect.Method;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.util.Collections;

/**
 * <code>RmiMessageDispatcher</code> will send transformed mule events over
 * RMI-JRMP.
 */
public class RmiMessageDispatcher extends AbstractMessageDispatcher
{
    private final RmiConnector connector;
    protected volatile Remote remoteObject;
    protected volatile Method invokedMethod;

    public RmiMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (RmiConnector)endpoint.getConnector();
    }

    protected void doConnect() throws Exception
    {
        if (remoteObject == null)
        {
            // Shouldn't all this be in the connector?
            String rmiPolicyPath = connector.getSecurityPolicy();
            System.setProperty("java.security.policy", rmiPolicyPath);

            // Set security manager
            if (System.getSecurityManager() == null)
            {
                System.setSecurityManager(new RMISecurityManager());
            }

            remoteObject = connector.getRemoteObject(endpoint);
        }
    }

    protected void doDisconnect() throws Exception
    {
        remoteObject = null;
        invokedMethod = null;
    }

    private Object[] getArgs(MuleEvent event) throws TransformerException
    {
        Object payload = event.transformMessage();
        if (payload instanceof Object[])
        {
            return (Object[])payload;
        }
        else
        {
            return new Object[]{payload};
        }
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        Object[] arguments = getArgs(event);
        if (invokedMethod == null)
        {
            invokedMethod = connector.getMethodObject(remoteObject, event);
        }
        invokedMethod.invoke(remoteObject, arguments);
    }

    public MuleMessage doSend(MuleEvent event) throws Exception
    {
        if (invokedMethod == null)
        {
            invokedMethod = connector.getMethodObject(remoteObject, event);
        }

        Object[] arguments = getArgs(event);
        Object result = invokedMethod.invoke(remoteObject, arguments);

        if (result == null)
        {
            return null;
        }
        else
        {
            return new DefaultMuleMessage(connector.getMessageAdapter(result).getPayload(), Collections.EMPTY_MAP);
        }
    }

    protected void doDispose()
    {
        // template method
    }
}
