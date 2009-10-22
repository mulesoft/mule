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

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.expression.PropertyConverter;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.config.annotations.endpoints.ChannelType;
import org.mule.config.annotations.i18n.AnnotationsMessages;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.impl.registry.ConfigurableTransportFactory;
import org.mule.impl.registry.RegistryMap;
import org.mule.transport.service.TransportFactory;
import org.mule.util.TemplateParser;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This is a wrapper builder that can process {@link AnnotatedEndpointData} objects (Annotaiton config data)
 * and turn them into {@link org.mule.api.endpoint.EndpointBuilder} or {@link org.mule.api.endpoint.ImmutableEndpoint} objects.
 * <p/>
 * THis is an internal class that should only be used the Annotaiton parser code.
 */
public class AnnotatedEndpointBuilder
{
    protected TemplateParser parser = TemplateParser.createAntStyleParser();

    protected RegistryMap regMap;
    protected MuleContext muleContext;
    protected TransportFactory transportFactory;

    public AnnotatedEndpointBuilder(MuleContext muleContext) throws MuleException
    {
        this.muleContext = muleContext;
        this.transportFactory = new ConfigurableTransportFactory(muleContext);
        regMap = new RegistryMap(muleContext.getRegistry());
    }

    protected String getPropertyValue(String key)
    {
        return parser.parse(new RegistryMap(muleContext.getRegistry()), key);
    }

    protected EndpointBuilder getEndpointBuilder(AnnotatedEndpointData epData) throws MuleException
    {
        String uri;
        if (MuleEndpointURI.isMuleUri(epData.getAddress()))
        {
            uri = epData.getAddress();
        }
        else
        {
            uri = getPropertyValue(epData.getAddress());
        }

        EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointFactory()
                .getEndpointBuilder(uri);
        endpointBuilder.setMuleContext(muleContext);

        return endpointBuilder;
    }

    public ImmutableEndpoint processEndpoint(AnnotatedEndpointData epData) throws MuleException
    {
        ImmutableEndpoint endpoint;
        EndpointBuilder endpointBuilder = getEndpointBuilder(epData);

        if (epData.getProperties() != null)
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
            endpointBuilder.setFilter(filter);
        }

        if (epData.getEncoding() != null)
        {
            endpointBuilder.setEncoding(getPropertyValue(epData.getEncoding()));
        }

        if (epData.getConnectorName() != null)
        {
            endpointBuilder.setConnector(muleContext.getRegistry().lookupConnector(getPropertyValue(epData.getConnectorName())));
        }
        else if (epData.getConnector() != null)
        {
            endpointBuilder.setConnector(epData.getConnector());
        }
        else
        {
            //We always create a new connecotr for annotations when one has not been configured
            MuleEndpointURI uri = new MuleEndpointURI(getPropertyValue(epData.getAddress()), muleContext);

            Connector connector = transportFactory.createConnector(uri);
            //The ibeans transport factory will not always create a new connector, check before registering
            if (muleContext.getRegistry().lookupConnector(connector.getName()) == null)
            {
                muleContext.getRegistry().registerConnector(connector);
            }
            endpointBuilder.setConnector(connector);

        }

        if (epData.getName() != null)
        {
            endpointBuilder.setName(getPropertyValue(epData.getName()));
        }


        endpointBuilder.setSynchronous(epData.isSynchronous());

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

    public Object convertProperty(Class type, String property)
    {
        String prop = getPropertyValue(property);
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
