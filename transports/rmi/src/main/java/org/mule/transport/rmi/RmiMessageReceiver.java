/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.rmi;

import org.mule.RequestContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.ConnectException;
import org.mule.transport.rmi.i18n.RmiMessages;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.util.List;

import org.apache.commons.collections.MapUtils;

/**
 * Will repeatedly call a method on a Remote object. If the method takes parameters A
 * List of objects can be specified on the endpoint called
 * <code>methodArgumentTypes</code>, If this property is ommitted it is assumed
 * that the method takes no parameters
 */

public class RmiMessageReceiver extends AbstractPollingMessageReceiver
{
    protected RmiConnector connector;

    protected Remote remoteObject;

    protected Method invokeMethod;

    protected Object[] methodArguments = null;

    public RmiMessageReceiver(Connector connector,
                              FlowConstruct flowConstruct,
                              InboundEndpoint endpoint,
                              long frequency) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        this.setFrequency(frequency);
        this.connector = (RmiConnector) connector;
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doConnect() throws Exception
    {
        connector.initSecurity();

        // Get methodName
        String methodName = MapUtils.getString(endpoint.getEndpointURI().getParams(),
                MuleProperties.MULE_METHOD_PROPERTY, null);
        if (null == methodName)
        {
            methodName = (String) endpoint.getProperty(MuleProperties.MULE_METHOD_PROPERTY);

            if (null == methodName)
            {
                throw new ConnectException(RmiMessages.messageParamServiceMethodNotSet(), this);
            }
        }

        // Get remoteObject
        remoteObject = connector.getRemoteObject(getEndpoint());

        // Set methodArguments
        List<Object> args = (List<Object>) endpoint.getProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAMS_LIST);
        Class[] argTypes = new Class[]{};
        if (args == null)
        {
            logger.info(RmiConnector.PROPERTY_SERVICE_METHOD_PARAMS_LIST
                    + " not set on endpoint, assuming method call has no arguments");
            methodArguments = ClassUtils.NO_ARGS;
        }
        else
        {
            argTypes = connector.getArgTypes(endpoint.getProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES), RequestContext.getEvent());
            methodArguments = new Object[args.size()];
            methodArguments = args.toArray(methodArguments);
        }
        
        // Set invokeMethod
        invokeMethod = remoteObject.getClass().getMethod(methodName, argTypes);
    }

    @Override
    protected void doDisconnect()
    {
        invokeMethod = null;
        remoteObject = null;
    }

    public void poll()
    {
        try
        {
            Object result = invokeMethod.invoke(remoteObject, getMethodArguments());

            if (null != result)
            {
                routeMessage(createMuleMessage(result));
            }
        }
        catch (Exception e)
        {
            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
        }
    }

    /**
     * Returns the method arguments to use when invoking the method on the Remote
     * object. This method can be overloaded to enable dynamic method arguments
     */
    protected Object[] getMethodArguments()
    {
        return methodArguments;
    }
}
