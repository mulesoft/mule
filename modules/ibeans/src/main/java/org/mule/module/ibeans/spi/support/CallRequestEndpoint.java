/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi.support;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.transformer.Transformer;
import org.mule.config.endpoint.AnnotatedEndpointData;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractConnector;
import org.mule.util.UriParamFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ibeans.api.channel.CHANNEL;

/**
 * A dynamic inbound endpoint used for request calls defined using the {@link org.ibeans.annotation.Call} annotation.
 * Note that call requests look the same as normal call endpoints except request calls do not define any headers or payload.
 * <p/>
 * The endpoint scheme is the only part of the URI that cannot be replaced at runtime.
 *
 * @see CallOutboundEndpoint
 */
public class CallRequestEndpoint extends DynamicRequestEndpoint
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(CallRequestEndpoint.class);

    private static final long serialVersionUID = 1861985949279708458L;

    //This is a hack to create a ref to the transformers collection, then we add the transformers once the endpoint type has been
    //determined
    private static List<Transformer> transformers = new ArrayList<Transformer>();
    private static List<Transformer> responseTransformers = new ArrayList<Transformer>();
    
    private UriParamFilter filter = new UriParamFilter();

    public CallRequestEndpoint(MuleContext context, AnnotatedEndpointData epData) throws MalformedEndpointException
    {
        super( context, createInboundBuilder(context, epData), epData.getAddress());
    }

    @Override
    protected void validateUriTemplate(String uri) throws MalformedEndpointException
    {
        if (uri.indexOf(parser.getStyle().getPrefix()) > -1 && uri.indexOf(":") > uri.indexOf(parser.getStyle().getPrefix()))
        {
            throw new MalformedEndpointException(CoreMessages.dynamicEndpointsMustSpecifyAScheme(), uri);
        }
    }

    private static EndpointBuilder createInboundBuilder(MuleContext context, AnnotatedEndpointData epData)
    {
        try
        {
            EndpointBuilder builder = context.getEndpointFactory().getEndpointBuilder("dynamic://null");
            builder.setExchangePattern(epData.getMep());
            builder.setConnector(epData.getConnector());
            builder.setName(epData.getName());
            builder.setProperties(epData.getProperties() == null ? new HashMap() : epData.getProperties());

            return builder;
        }
        catch (MuleException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String parseURIString(String uri, MuleMessage message)
    {
        //We do additional processing here to parse the URI template
        Map<String, Object> props = getPropertiesForTemplate(message);

        String newUriString = parser.parse(props, uri);
        //Remove optional params completely if null
        newUriString = filter.filterParamsByValue(newUriString, CallOutboundEndpoint.NULL_PARAM);

        return super.parseURIString(newUriString, message);
    }

    @Override
    protected Map<String, Object> getPropertiesForTemplate(MuleMessage message)
    {
        Map<String, Object> props = (Map) message.findPropertyInAnyScope(CHANNEL.URI_PARAM_PROPERTIES, null);
        if (props == null)
        {
            throw new IllegalStateException(CHANNEL.URI_PARAM_PROPERTIES + " not set on message");
        }
        return props;
    }

    @Override
    public List getTransformers()
    {
        if (transformers.size() == 0)
        {
            try
            {
                transformers.addAll(((AbstractConnector)getConnector()).getDefaultInboundTransformers(this));
                for (Transformer tran : transformers)
                {
                    tran.setEndpoint(this);
                    tran.setMuleContext(getMuleContext());
                    tran.initialise();
                }
            }
            catch (MuleException e)
            {
                throw new RuntimeException(e);
            }
        }
        return transformers;
    }

    @Override
    public List getResponseTransformers()
    {
        if (responseTransformers.size() == 0)
        {
            try
            {
                responseTransformers.addAll(((AbstractConnector)getConnector()).getDefaultResponseTransformers(this));
                for (Transformer tran : responseTransformers)
                {
                    tran.setEndpoint(this);
                    tran.setMuleContext(getMuleContext());
                    tran.initialise();
                }
            }
            catch (MuleException e)
            {
                throw new RuntimeException(e);
            }
        }
        return transformers;
    }
}
