/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.rmi;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.transformer.TransformerException;

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

    public RmiMessageDispatcher(UMOImmutableEndpoint endpoint)
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

    private Object[] getArgs(UMOEvent event) throws TransformerException
    {
        Object payload = event.getTransformedMessage();
        if (payload instanceof Object[])
        {
            return (Object[])payload;
        }
        else
        {
            return new Object[]{payload};
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#dispatch(org.mule.umo.UMOEvent)
     */
    protected void doDispatch(UMOEvent event) throws Exception
    {
        Object[] arguments = getArgs(event);
        if (invokedMethod == null)
        {
            invokedMethod = connector.getMethodObject(remoteObject, event);
        }
        invokedMethod.invoke(remoteObject, arguments);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#send(org.mule.umo.UMOEvent)
     */
    public UMOMessage doSend(UMOEvent event) throws Exception
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
            return new MuleMessage(connector.getMessageAdapter(result).getPayload(), Collections.EMPTY_MAP);
        }
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        throw new UnsupportedOperationException("doReceive");
    }

    protected void doDispose()
    {
        // template method
    }
}
