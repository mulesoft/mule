/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.annotations.meta.ChannelType;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.expression.PropertyConverter;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.AnnotationsMessages;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.registry.RegistryMap;
import org.mule.routing.MessageFilter;
import org.mule.transport.AbstractConnector;
import org.mule.transport.service.TransportFactory;
import org.mule.util.TemplateParser;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This is a wrapper helper that can process {@link AnnotatedEndpointData} objects (Annotaiton config data)
 * and turn them into {@link org.mule.api.endpoint.EndpointBuilder} or {@link org.mule.api.endpoint.ImmutableEndpoint} objects.
 * <p/>
 * THis is an internal class that should only be used by the Annotation parser code.
 */
public class AnnotatedEndpointHelper
{
    protected TemplateParser parser = TemplateParser.createAntStyleParser();

    protected RegistryMap regMap;
    protected MuleContext muleContext;
    protected TransportFactory transportFactory;

    public AnnotatedEndpointHelper(MuleContext muleContext) throws MuleException
    {
        this.muleContext = muleContext;
        this.transportFactory = new ConfigurableTransportFactory(muleContext);
        regMap = new RegistryMap(muleContext.getRegistry());
    }

    protected String parsePlaceholderValues(String key)
    {
        return parser.parse(regMap, key);
    }

    protected EndpointBuilder getEndpointBuilder(AnnotatedEndpointData epData) throws MuleException
    {
        String uri = parsePlaceholderValues(epData.getAddress());

        EndpointBuilder endpointBuilder = muleContext.getEndpointFactory().getEndpointBuilder(uri);
        endpointBuilder.setMuleContext(muleContext);

        return endpointBuilder;
    }

    public ImmutableEndpoint processEndpoint(AnnotatedEndpointData epData) throws MuleException
    {
        preprocessEndpointData(epData);

        ImmutableEndpoint endpoint;
        EndpointBuilder endpointBuilder = getEndpointBuilder(epData);

        if (epData.getProperties() != null && epData.getProperties().size() > 0)
        {
            endpointBuilder.setProperties(epData.getProperties());
        }

        if (epData.getTransformers() != null)
        {
            List<Transformer> transformers = (List) convertProperty(List.class, epData.getTransformers());
            endpointBuilder.setTransformers(transformers);
        }

        if (epData.getFilter() != null)
        {
            Filter filter = (Filter) convertProperty(Filter.class, epData.getFilter());
            endpointBuilder.addMessageProcessor(new MessageFilter(filter));

        }

        if (epData.getEncoding() != null)
        {
            endpointBuilder.setEncoding(parsePlaceholderValues(epData.getEncoding()));
        }

        AbstractConnector connector;
        if (epData.getConnectorName() != null)
        {
            connector = (AbstractConnector) muleContext.getRegistry().lookupConnector(parsePlaceholderValues(epData.getConnectorName()));
        }
        else if (epData.getConnector() != null)
        {
            connector = (AbstractConnector) epData.getConnector();
        }
        else
        {
            //We always create a new connecotr for annotations when one has not been configured
            MuleEndpointURI uri = new MuleEndpointURI(parsePlaceholderValues(epData.getAddress()), muleContext);

            connector = (AbstractConnector) transportFactory.createConnector(uri);
            //The ibeans transport factory will not always create a new connector, check before registering
            if (muleContext.getRegistry().lookupConnector(connector.getName()) == null)
            {
                muleContext.getRegistry().registerConnector(connector);
            }
        }
        endpointBuilder.setConnector(connector);

        //Set threading for this connector. Note we simplify by setting all profiles with a single value 'threads'
        //that can be set by the user
        String threadsString = (String) epData.getProperties().get("threads");
        if (threadsString != null)
        {
            int threads = Integer.valueOf(threadsString);
            connector.setMaxDispatchersActive(threads);
            connector.setMaxRequestersActive(threads);
            connector.getReceiverThreadingProfile().setMaxThreadsActive(threads);
            connector.getReceiverThreadingProfile().setMaxThreadsIdle(threads);
        }

        if (epData.getName() != null)
        {
            endpointBuilder.setName(parsePlaceholderValues(epData.getName()));
        }

        endpointBuilder.setExchangePattern(epData.getMep());

        if (epData.getType() == ChannelType.Inbound)
        {
            endpoint = endpointBuilder.buildInboundEndpoint();
        }
        else if (epData.getType() == ChannelType.Outbound)
        {
            endpoint = endpointBuilder.buildOutboundEndpoint();
        }
        else
        {
            throw new IllegalArgumentException("Channel type not recognised: " + epData.getType());
        }
        //TODO: not sure where to put this yet
        if (epData.getName() != null)
        {
            muleContext.getRegistry().registerEndpointBuilder(epData.getName(), endpointBuilder);
        }
        return endpoint;
    }

    /**
     * This method can be overridden to process endpoints before they get built. This may be useful in environments
     * where the characteristics of the endpoint change depending on the deployed environment
     *
     * @param data the endpoint data to process
     */
    protected void preprocessEndpointData(AnnotatedEndpointData data)
    {
        //no=op
    }

    public Object convertProperty(Class type, String property)
    {
        String prop = parsePlaceholderValues(property);
        Collection c = muleContext.getRegistry().lookupObjects(PropertyConverter.class);
        for (Iterator iterator = c.iterator(); iterator.hasNext();)
        {
            PropertyConverter converter = (PropertyConverter) iterator.next();
            if (converter.getType().equals(type))
            {
                return converter.convert(prop, muleContext);
            }
        }
        throw new IllegalArgumentException(AnnotationsMessages.noPropertyConverterForType(type).getMessage());
    }

}
