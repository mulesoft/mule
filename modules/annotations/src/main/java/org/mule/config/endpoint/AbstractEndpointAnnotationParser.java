/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.endpoint;

import org.mule.api.EndpointAnnotationParser;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.annotations.meta.Channel;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * TODO
 */
public abstract class AbstractEndpointAnnotationParser implements EndpointAnnotationParser, MuleContextAware
{
    public static final String ENDPOINT_BUILDER_POSTFIX = ".builder";

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    protected AnnotatedEndpointHelper getEndpointHelper() throws MuleException
    {
        AnnotatedEndpointHelper helper = muleContext.getRegistry().lookupObject(AnnotatedEndpointHelper.class);
        if (helper == null)
        {
            helper = new AnnotatedEndpointHelper(muleContext);
        }
        return helper;
    }

    public OutboundEndpoint parseOutboundEndpoint(Annotation annotation, Map metaInfo) throws MuleException
    {
        return (OutboundEndpoint) getEndpointHelper().processEndpoint(createEndpointData(annotation));
    }

    public InboundEndpoint parseInboundEndpoint(Annotation annotation, Map metaInfo) throws MuleException
    {
        return (InboundEndpoint) getEndpointHelper().processEndpoint(createEndpointData(annotation));
    }

    public boolean supports(Annotation annotation, Class clazz, Member member)
    {
        Channel channel = annotation.annotationType().getAnnotation(Channel.class);
        return channel != null && channel.identifer().equals(getIdentifier());
    }

    protected Properties convertProperties(String[] properties)
    {
        if(properties==null || properties.length==0)
        {
            return null;
        }
        
        Properties props = new Properties();
        for (String property : properties)
        {
            StringTokenizer st = new StringTokenizer(property, "=");
            if(st.hasMoreTokens())
            {
                props.setProperty(st.nextToken().trim(), st.nextToken().trim());
            }
        }
        return props;
    }

    protected <T> T lookupConfig(String location, Class<T> type) throws ConfigurationException
    {
        if (StringUtils.isEmpty(location))
        {
            return null;
        }
        Object o = muleContext.getRegistry().lookupObject(location + ENDPOINT_BUILDER_POSTFIX);
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
