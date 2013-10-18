/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.registry.RegistryMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MimeTypeParseException;

import org.ibeans.api.CallInterceptor;
import org.ibeans.api.IBeanInvocationData;
import org.ibeans.api.IBeanInvoker;
import org.ibeans.api.IBeansException;
import org.ibeans.api.channel.CHANNEL;
import org.ibeans.impl.DefaultIBeanInvoker;
import org.ibeans.impl.InvokeAnnotationHandler;
import org.ibeans.impl.TemplateAnnotationHandler;
import org.ibeans.impl.support.util.Utils;
import org.ibeans.spi.ErrorFilterFactory;
import org.ibeans.spi.ExpressionParser;
import org.ibeans.spi.IBeansPlugin;

/**
 * The entry-point for Mule to integrate with IBeans
 */
public class MuleIBeansPlugin implements IBeansPlugin<MuleRequestMessage, MuleResponseMessage>
{
    private MuleContext muleContext;

    private Map<String, Object> properties;
    private MuleExpressionParser expressionParser;
    private MuleCallAnnotationHandler callAnnotationHandler;
    private TemplateAnnotationHandler templateAnnotationHandler;
    private InvokeAnnotationHandler invokeAnnotationHandler;
    private MuleResponseTransformInterceptor responseTransformInterceptor;
    private List<ErrorFilterFactory> errorFilterFactories;

    public MuleIBeansPlugin(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        callAnnotationHandler = new MuleCallAnnotationHandler(muleContext);
        expressionParser = new MuleExpressionParser(muleContext);
        properties = new RegistryMap(muleContext.getRegistry());
        templateAnnotationHandler = new TemplateAnnotationHandler(this);
        invokeAnnotationHandler = new InvokeAnnotationHandler(this);
        responseTransformInterceptor = new MuleResponseTransformInterceptor(muleContext, expressionParser);

        errorFilterFactories = new ArrayList<ErrorFilterFactory>();
        errorFilterFactories.add(new ExpressionErrorFilterFactory(muleContext));
    }

    public CallInterceptor getResponseTransformInterceptor() throws IBeansException
    {
        return responseTransformInterceptor;
    }

    public IBeanInvoker<MuleCallAnnotationHandler, TemplateAnnotationHandler, InvokeAnnotationHandler> getIBeanInvoker() throws IBeansException
    {
        return new DefaultIBeanInvoker<MuleCallAnnotationHandler, TemplateAnnotationHandler, InvokeAnnotationHandler>(callAnnotationHandler, templateAnnotationHandler, invokeAnnotationHandler);
    }

    public IBeanInvoker<MuleMockCallAnnotationHandler, TemplateAnnotationHandler, InvokeAnnotationHandler> getMockIBeanInvoker(Object mock) throws IBeansException
    {
        return new DefaultIBeanInvoker<MuleMockCallAnnotationHandler, TemplateAnnotationHandler, InvokeAnnotationHandler>(new MuleMockCallAnnotationHandler(muleContext, mock, this), templateAnnotationHandler, invokeAnnotationHandler);
    }

    public List<ErrorFilterFactory> getErrorFilterFactories()
    {
        return errorFilterFactories;
    }

    public Map getProperties()
    {
        return properties;
    }

    public ExpressionParser getExpressionParser()
    {
        return expressionParser;
    }

    public void addInterceptors(LinkedList<CallInterceptor> interceptors)
    {
        //nothing to do
    }

    public MuleRequestMessage createRequest(IBeanInvocationData data) throws IBeansException
    {
        MuleRequestMessage request;
        Object payload = (data.getPayloads().size() == 1 ? data.getPayloads().get(0) : data.getPayloads());

        MuleMessage message = new DefaultMuleMessage(payload, muleContext);

        //We need to scrub any null header values since Mule does not allow Null headers
        for (Map.Entry<String, Object> entry : data.getHeaderParams().entrySet())
        {
            if (entry.getValue() != null)
            {
                message.setOutboundProperty(entry.getKey(), entry.getValue());
            }
        }

        //TODO (requires API change)
        // message.setInvocationProperty(MuleProperties.MULE_METHOD_PROPERTY, XXX);

        //Set the URI params so the correct URI can be constructed for this invocation
        message.setOutboundProperty(CHANNEL.URI_PARAM_PROPERTIES, data.getUriParams());

        //Add any attachments
        for (DataSource dataSource : data.getAttachments())
        {
            try
            {
                message.addOutboundAttachment(dataSource.getName(), new DataHandler(dataSource));
            }
            catch (Exception e)
            {
                throw new IBeansException(e);
            }
        }

        //Add the properties to the invocation scope
        for (String key : data.getPropertyParams().keySet())
        {
            message.setInvocationProperty(key, data.getPropertyParams().get(key));
        }

        request = new MuleRequestMessage(data, message);

        //TODO It may be useful to set the Method invoked on the request, In Mule,
        //Some transports such as Axis, RMI and EJB can use the method information
        //Not doing this for now since the scope is HTTP only

        //Set the request timeout, the default -1 means it will not timeout
        request.setTimeout(Utils.getInt(data.getPropertyParams().get(CHANNEL.TIMEOUT), -1));
        return request;
    }

    public MuleResponseMessage createResponse(Object payload, Map<String, Object> headers, Map<String, DataHandler> attachments) throws IBeansException
    {
        MuleMessage message = new DefaultMuleMessage(payload, headers, new HashMap<String, Object>(), attachments, muleContext);
        try
        {
            return new MuleResponseMessage(message);
        }
        catch (MimeTypeParseException e)
        {
            throw new IBeansException(e);
        }
    }
}
