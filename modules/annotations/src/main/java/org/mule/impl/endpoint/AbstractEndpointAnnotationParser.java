/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.endpoint;

import org.mule.api.EndpointAnnotationParser;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.annotations.converters.PropertiesConverter;
import org.mule.config.annotations.endpoints.Channel;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Map;

/**
 * TODO
 */
public abstract class AbstractEndpointAnnotationParser implements EndpointAnnotationParser, MuleContextAware
{
    protected MuleContext muleContext;
    private PropertiesConverter converter = new PropertiesConverter();

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    protected AnnotatedEndpointBuilder getEndpointBuilder() throws MuleException
    {
        AnnotatedEndpointBuilder builder = muleContext.getRegistry().lookupObject(AnnotatedEndpointBuilder.class);
        if (builder == null)
        {
            builder = new AnnotatedEndpointBuilder(muleContext);
        }
        return builder;
    }

    public OutboundEndpoint parseOutboundEndpoint(Annotation annotation, Map metaInfo) throws MuleException
    {
        return (OutboundEndpoint) getEndpointBuilder().processEndpoint(createEndpointData(annotation));
    }

    public InboundEndpoint parseInboundEndpoint(Annotation annotation, Map metaInfo) throws MuleException
    {
        return (InboundEndpoint) getEndpointBuilder().processEndpoint(createEndpointData(annotation));
    }

    public boolean supports(Annotation annotation, Class clazz, Member member)
    {
        Channel channel = annotation.annotationType().getAnnotation(Channel.class);
        return channel != null && channel.identifer().equals(getIdentifier());
    }

    protected Map convertProperties(String properties)
    {
        return (Map) converter.convert(properties, muleContext);
    }

    protected <T> T lookupConfig(String location, Class<T> type) throws ConfigurationException
    {
        if (StringUtils.isEmpty(location))
        {
            return null;
        }
        Object o = muleContext.getRegistry().lookupObject(location + ".builder");
        if (o == null)
        {
            o = muleContext.getRegistry().lookupObject(location);
            if (o == null)
            {
                return null;
                //throw new ConfigurationException(CoreMessages.objectNotFound(location));
            }
        }
        if (type.isInstance(o))
        {
            return (T) o;
        }
        else
        {
            throw new ConfigurationException(CoreMessages.objectNotOfCorrectType(o.getClass(), type));
        }
    }

    protected abstract String getIdentifier();

    protected abstract AnnotatedEndpointData createEndpointData(Annotation annotation) throws MuleException;
}
