/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ibean;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.NullPayload;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * <code>IBeansMessageReceiver</code> TODO document
 */
public class IBeansMessageReceiver extends  AbstractPollingMessageReceiver
{
    private Object ibean;
    private Method ibeanMethod;
    private Object[] callParams;
    private String methodName;

    public IBeansMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint)
            throws MuleException
    {
        super(connector, service, endpoint);
        setFrequency(60);
        setTimeUnit(TimeUnit.SECONDS);

        List<?> state = (List)endpoint.getProperty(IBeansConnector.STATE_PARAMS_PROPERTY);
        if(state==null)
        {
            state  = Collections.emptyList();
        }

        ibean = ((IBeansConnector)connector).createIbean(endpoint.getEndpointURI(), state);
        //Note that the address has already been validated by the {@link IBeansEndpointURIBuilder}
        String address = endpoint.getEndpointURI().getAddress();
        methodName = address.substring(address.indexOf(".")+1);

        List params = (List)endpoint.getProperty(IBeansConnector.CALL_PARAMS_PROPERTY);
        if(params==null) params = Collections.emptyList();

        ExpressionManager em = connector.getMuleContext().getExpressionManager();        
        MuleMessage defaultMessage = new DefaultMuleMessage(NullPayload.getInstance(), endpoint.getProperties(), connector.getMuleContext());

        callParams = new Object[params.size()];
        int i = 0;
        for (Object param : params)
        {
            if(param instanceof String && em.isExpression(param.toString()))
            {
                param = em.parse(param.toString(), defaultMessage);
            }
            callParams[i++] = param;
        }

    }
    
    public void poll() throws Exception
    {
        if(ibeanMethod==null)
        {
            ibeanMethod = getMethod();
        }
        ibeanMethod.invoke(ibean, callParams);
    }

    protected Method getMethod() throws NoSuchMethodException
    {
        Class<?>[] paramTypes = new Class<?>[callParams.length];
        int i = 0;
        for (Object param : callParams)
        {
            paramTypes[i++] = param.getClass();
        }
        return ibean.getClass().getMethod(methodName, paramTypes);
    }
}
