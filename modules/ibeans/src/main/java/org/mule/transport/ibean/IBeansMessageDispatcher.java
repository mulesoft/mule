/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ibean;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.module.ibeans.i18n.IBeansMessages;
import org.mule.transport.AbstractMessageDispatcher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ibeans.impl.IBeansNotationHelper;

/**
 * <code>IBeansMessageDispatcher</code> TODO document
 */
public class IBeansMessageDispatcher extends AbstractMessageDispatcher
{

    private Object ibean;
    //We only cache the method name and resolve it for every request.  this allows for invoking overloaded methods
    //depending on the payload parameters of the event
    private String method;
    private String ibeanName;

    public IBeansMessageDispatcher(OutboundEndpoint endpoint) throws MuleException
    {
        super(endpoint);
        List state = (List)endpoint.getProperty(IBeansConnector.STATE_PARAMS_PROPERTY);
        if(state==null)
        {
            state  = Collections.emptyList();
        }
        IBeansConnector cnn = (IBeansConnector)getConnector();
        ibean = cnn.createIbean(endpoint.getEndpointURI(), state);
        ibeanName = IBeansNotationHelper.getIBeanShortID(ibean.getClass().getInterfaces()[0]);
        //Note that the address has already been validated by the {@link IBeansEndpointURIBuilder}
        String address = endpoint.getEndpointURI().getAddress();
        method = address.substring(address.indexOf(".")+1);

    }

    @Override
    public void doDispatch(MuleEvent event) throws Exception
    {
        doSend(event);
    }

    @Override
    public MuleMessage doSend(MuleEvent event) throws Exception
    {
        Object payload = event.getMessage().getPayload();
        Object[] params;
        //Lets create the params array from the request
        if(payload.getClass().isArray())
        {
            params = (Object[])payload;
        }
        else if (payload instanceof ArrayList)
        {
            params = ((ArrayList) payload).toArray();
        }
        else
        {
            params = new Object[]{payload};
        }

        //Create an array of types we can use to look up the override method if there is one, or validate the parameters
        //against the expected parameters for the IBean method
        Class<?>[] types = new Class[params.length];

        for (int i = 0; i < params.length; i++)
        {
             types[i] = params[i].getClass();
        }

        Method callMethod;
        //The method property can be set in the Invocation scope to override the method set on the endpoint
        String methodName = event.getMessage().getInvocationProperty(MuleProperties.MULE_METHOD_PROPERTY, method);
            try
            {
                callMethod = ibean.getClass().getMethod(methodName, types);
            }
            catch (Throwable e)
            {
                throw new DispatchException(IBeansMessages.ibeanMethodNotFound(ibeanName, methodName, types), event, this, e);
            }

        Object result = callMethod.invoke(ibean, params);
        return new DefaultMuleMessage(result, event.getMessage(), event.getMuleContext());
    }
}

