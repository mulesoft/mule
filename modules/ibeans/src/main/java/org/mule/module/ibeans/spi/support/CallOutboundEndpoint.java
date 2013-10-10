/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi.support;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.param.Payload;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.endpoint.AnnotatedEndpointData;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.transport.AbstractConnector;
import org.mule.transport.service.TransportFactory;
import org.mule.util.BeanUtils;
import org.mule.util.TemplateParser;
import org.mule.util.UriParamFilter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ibeans.annotation.Call;
import org.ibeans.annotation.param.HeaderParam;
import org.ibeans.api.channel.CHANNEL;

/**
 * <p>
 * A dynamic outbound endpoint defined when using the {@link Call} annotation. A
 * CallOutboundEndpoint is generated when the call method has a one or more payloads
 * defined using {@link Payload} annotation or one or more headers defined using the
 * {@link HeaderParam} annotation. annotations.
 * <p/>
 * <p>
 * The endpoint scheme is the only part of the URI that cannot be replaced at
 * runtime.
 * </p>
 *
 * @see CallRequestEndpoint
 */
public class CallOutboundEndpoint extends org.mule.endpoint.DynamicOutboundEndpoint
{
    public static final String NULL_PARAM = "null.param";

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(CallOutboundEndpoint.class);

    private static final long serialVersionUID = 1861985949279708638L;

    //The parser used to parse the @Call uri template
    protected TemplateParser parser = TemplateParser.createCurlyBracesStyleParser();

    private UriParamFilter filter = new UriParamFilter();

    public CallOutboundEndpoint(MuleContext context, AnnotatedEndpointData epData) throws MalformedEndpointException
    {
        super(createBuilder(context, epData), epData.getAddress());
    }

    private synchronized static EndpointBuilder createBuilder(MuleContext context, AnnotatedEndpointData epData)
    {
        try
        {
            String address = epData.getAddress();
            int i = address.indexOf(":/");
            String scheme;
            if (i > -1)
            {
                scheme = address.substring(0, i);
                address = scheme + "://dynamic";
                //This is used for creating the connector, since we don't know if the actual URI address is a vaild URI.
                EndpointURI tempUri = new MuleEndpointURI(address, context);
                AbstractConnector cnn = null;

                if (epData.getConnectorName() != null)
                {
                    cnn = (AbstractConnector) context.getRegistry().lookupConnector(epData.getConnectorName());
                }
                if (cnn == null)
                {
                    cnn = (AbstractConnector) new TransportFactory(context).createConnector(tempUri);
                    if (epData.getConnectorName() != null)
                    {
                        cnn.setName(epData.getConnectorName());
                    }
                    context.getRegistry().registerConnector(cnn);
                }

                //This allows connector properties to be set as properties on the endpoint
                Map props = epData.getProperties();
                if (props == null)
                {
                    props = new HashMap();
                }
                else
                {
                    BeanUtils.populateWithoutFail(cnn, props, false);
                }
                EndpointBuilder builder = context.getEndpointFactory().getEndpointBuilder(address);
                builder.setConnector(cnn);
                builder.setName(epData.getName());
                builder.setProperties(props);

                return builder;


            }
            else
            {
                throw new IllegalArgumentException("When defining a dynamic endpoint the endpoint scheme must be set i.e. http://{dynamic}");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void validateUriTemplate(String uri) throws MalformedEndpointException
    {
        //TODO
    }

    @Override
    protected String parseURIString(String uri, MuleMessage message)
    {
        //We do additional processing here to parse the URI template
        Map<String, Object> props = getPropertiesForUriTemplate(message);

        String newUriString = parser.parse(props, uri);
        //Remove optional params completely if null
        newUriString = filter.filterParamsByValue(newUriString, NULL_PARAM);

        return super.parseURIString(newUriString, message);
    }

    protected Map<String, Object> getPropertiesForUriTemplate(MuleMessage message)
    {
        Map<String, Object> props = (Map) message.getOutboundProperty(CHANNEL.URI_PARAM_PROPERTIES);
        if (props == null)
        {
            throw new IllegalStateException(CHANNEL.URI_PARAM_PROPERTIES + " not set on message");
        }
        return props;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent result = super.process(event);
        if (result != null)
        {
            result.getMessage().setProperty(CHANNEL.CALL_URI_PROPERTY, getEndpointURIForMessage(event).getUri().toString(), PropertyScope.OUTBOUND);
        }
        return result;
    }
}
