/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.config;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.annotations.meta.Channel;
import org.mule.api.annotations.meta.ChannelType;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.endpoint.AbstractEndpointAnnotationParser;
import org.mule.config.endpoint.AnnotatedEndpointData;
import org.mule.module.ibeans.i18n.IBeansMessages;
import org.mule.module.ibeans.spi.support.CallOutboundEndpoint;
import org.mule.module.ibeans.spi.support.CallRequestEndpoint;

import java.beans.ExceptionListener;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.ibeans.annotation.Call;
import org.ibeans.api.CallException;
import org.ibeans.api.ExceptionListenerAware;

/**
 * The parser responsible for parsing {@link org.ibeans.annotation.Call} annotations.
 */
public class CallAnnotationParser extends AbstractEndpointAnnotationParser
{
    protected AnnotatedEndpointData createEndpointData(Annotation annotation) throws MuleException
    {
        Call call = (Call) annotation;
        AnnotatedEndpointData epd = new AnnotatedEndpointData(MessageExchangePattern.REQUEST_RESPONSE, ChannelType.Outbound, call);
        epd.setAddress(call.uri());
        epd.setProperties(AnnotatedEndpointData.convert(call.properties()));
        return epd;
    }

    protected String getIdentifier()
    {
        return Call.class.getAnnotation(Channel.class).identifer();
    }

    @Override
    public boolean supports(Annotation annotation, Class clazz, Member member)
    {
        //You cannot use the @Call annotation on an implementation class
        boolean supports = clazz.isInterface();
        if (supports)
        {
            supports = annotation instanceof Call;  
        }
        if (supports)
        {
            //Allow services to extend an exception listener that the user can plug in
            if (ExceptionListenerAware.class.isAssignableFrom(clazz))
            {
                supports = true;
            }
            else
            {
                Class[] exceptionTypes = ((Method) member).getExceptionTypes();
                boolean hasValidExceptionType = false;
                for (int i = 0; i < exceptionTypes.length; i++)
                {
                    Class exceptionType = exceptionTypes[i];
                    hasValidExceptionType = exceptionType.equals(Exception.class) || exceptionType.isAssignableFrom(CallException.class) || clazz.isAssignableFrom(ExceptionListener.class);
                }
                if (!hasValidExceptionType)
                {
                    throw new IllegalArgumentException(IBeansMessages.illegalCallMethod((Method)member).getMessage());
                }
            }
        }
        return supports;
    }

    public OutboundEndpoint parseOutboundEndpoint(Annotation annotation, Map metaInfo) throws MuleException
    {
        AnnotatedEndpointData data = createEndpointData(annotation);
        if (data.getConnectorName() == null)
        {
            data.setConnectorName((String) metaInfo.get("connectorName"));
        }
        return new CallOutboundEndpoint(muleContext, data);
    }

    public InboundEndpoint parseInboundEndpoint(Annotation annotation, Map metaInfo) throws MuleException
    {
        AnnotatedEndpointData data = createEndpointData(annotation);
        if (data.getConnectorName() == null)
        {
            data.setConnectorName((String) metaInfo.get("connectorName"));
        }
        return new CallRequestEndpoint(muleContext, data);
    }
}
