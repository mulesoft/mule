/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi.support;

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.endpoint.DynamicOutboundEndpoint;
import org.mule.endpoint.DynamicURIInboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.service.TransportFactory;
import org.mule.transport.service.TransportFactoryException;
import org.mule.util.TemplateParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A dynamic request endpoint is used in conjunction with the {@link org.ibeans.annotation.Call} annotation when there are no {@link org.ibeans.annotation.param.Body},
 * {@link org.ibeans.annotation.param.BodyParam} or {@link org.ibeans.annotation.param.HeaderParam} annotations
 * on a method and allows a dynamic {@link org.mule.api.endpoint.InboundEndpoint} to be created.  This endpoint is then used via the Mule {@link org.mule.api.transport.MessageRequester}
 * interface to make a specific request to a transport for a message.
 */
public class DynamicRequestEndpoint extends DynamicURIInboundEndpoint
{
    public static final String EVAL_PARAM_PROPERTY = "eval.param";
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(DynamicRequestEndpoint.class);
    private static final long serialVersionUID = 8861985949279708638L;

    protected TemplateParser parser = TemplateParser.createCurlyBracesStyleParser();

    /**
     * The URI template used to construct the actual URI to send the message to.
     */
    protected String uriTemplate;

    private EndpointBuilder builder;

   public DynamicRequestEndpoint(MuleContext muleContext, EndpointBuilder builder, String uriTemplate) throws MalformedEndpointException
    {
        super(new NullInboundEndpoint(muleContext));
        this.builder = builder;
        this.uriTemplate = uriTemplate;
        validateUriTemplate(uriTemplate);
    }

    protected void validateUriTemplate(String uri) throws MalformedEndpointException
    {
        if (uri.indexOf(":") > uri.indexOf(ExpressionManager.DEFAULT_EXPRESSION_PREFIX))
        {
            throw new MalformedEndpointException(CoreMessages.dynamicEndpointsMustSpecifyAScheme(), uri);
        }
    }

    protected Map<String, Object> getPropertiesForTemplate(MuleMessage message)
    {
        Map<String, Object> props = new HashMap<String, Object>();
        // Also add the endpoint properties so that users can set fallback values
        // when the property is not set on the event
        props.putAll(this.getProperties());
        for (String propertyKey : message.getOutboundPropertyNames())
        {
            props.put(propertyKey, message.getOutboundProperty(propertyKey));
        }
        return props;
    }

    protected EndpointURI getEndpointURIForMessage(MuleEvent event) throws MessagingException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Uri before parsing is: " + uriTemplate);
        }

        Map<String, Object> props = getPropertiesForTemplate(event.getMessage());

        String newUriString = parser.parse(props, uriTemplate);
        Object evalParam = props.get(EVAL_PARAM_PROPERTY);
        if (evalParam != null)
        {
            newUriString = parseURIString(newUriString, new DefaultMuleMessage(evalParam, getMuleContext()));
        }
        else
        {
            newUriString = parseURIString(newUriString, event.getMessage());
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Uri after parsing is: " + newUriString);
        }

        try
        {
            setEndpointURI(new MuleEndpointURI(newUriString, getMuleContext()));

            if (!newUriString.startsWith(getEndpointURI().getFullScheme()))
            {
                throw new MessagingException(CoreMessages.schemeCannotChangeForRouter(
                        this.getEndpointURI().getScheme(), getEndpointURI().getScheme()), event);
            }
            getEndpointURI().initialise();
            return getEndpointURI();
        }
        catch (Exception e)
        {
            throw new MessagingException(
                    CoreMessages.templateCausedMalformedEndpoint(uriTemplate, newUriString),
                    event, e);
        }

    }

    protected String parseURIString(String uri, MuleMessage message)
    {
        return this.getMuleContext().getExpressionManager().parse(uri, message, true);
    }

    public MuleMessage request(long timeout, MuleEvent event) throws Exception
    {
        EndpointURI uri = getEndpointURIForMessage(event);

        if (endpoint instanceof NullInboundEndpoint)
        {
            builder.setURIBuilder(new URIBuilder(uri));
            endpoint = builder.buildInboundEndpoint();
        }
        InboundEndpoint inboundEndpoint = new DynamicURIInboundEndpoint(endpoint, uri);

        if (event.getMessage().getInvocationProperty(MuleProperties.MULE_CREDENTIALS_PROPERTY) != null)
        {
            inboundEndpoint.getProperties().put(MuleProperties.MULE_CREDENTIALS_PROPERTY, event.getMessage().getInvocationProperty(MuleProperties.MULE_CREDENTIALS_PROPERTY));
        }
        return super.request(timeout);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }

        DynamicRequestEndpoint that = (DynamicRequestEndpoint) o;

        return !(uriTemplate != null ? !uriTemplate.equals(that.uriTemplate) : that.uriTemplate != null);

    }

    @Override
    public int hashCode()
    {
        int result = 0;
        result = 31 * result + (uriTemplate != null ? uriTemplate.hashCode() : 0);
        return result;
    }

    protected static class NullInboundEndpoint extends DefaultInboundEndpoint implements InboundEndpoint
    {
        NullInboundEndpoint(MuleContext muleContext)
        {
            super(createDynamicConnector(muleContext), null, null, new HashMap(), null, true, MessageExchangePattern.ONE_WAY, 0, "started", null, null, muleContext, null, null, null, null, null, true, null);
        }

        @Override
        public MessageProcessor createMessageProcessorChain(FlowConstruct flowContruct) throws MuleException
        {
            throw new UnsupportedOperationException("createMessageProcessorChain");
        }

        public List<String> getResponseProperties()
        {
            return Collections.emptyList();
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new UnsupportedOperationException("process");
        }

        static Connector createDynamicConnector(MuleContext muleContext)
        {
            try
            {
                return new TransportFactory(muleContext).createConnector(DynamicOutboundEndpoint.DYNAMIC_URI_PLACEHOLDER);
            }
            catch (TransportFactoryException e)
            {
                //This should never happen
                throw new MuleRuntimeException(e);
            }
        }
    }


}