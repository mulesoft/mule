/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.rmi;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;

import java.lang.reflect.Method;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;

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
        Object payload = event.getMessage().getPayload();
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
            invokedMethod = connector.getMethodObject(remoteObject, event, getEndpoint());
        }
        invokedMethod.invoke(remoteObject, arguments);
    }

    public MuleMessage doSend(MuleEvent event) throws Exception
    {
        if (invokedMethod == null)
        {
            invokedMethod = connector.getMethodObject(remoteObject, event, getEndpoint());
        }

        Object[] arguments = getArgs(event);
        Object result = invokedMethod.invoke(remoteObject, arguments);

        if (result == null)
        {
            return createMuleMessage(NullPayload.getInstance());
        }
        else
        {
            return createMuleMessage(result);
        }
    }

    protected void doDispose()
    {
        // template method
    }
}
