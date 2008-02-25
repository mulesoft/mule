/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.echo;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.model.Model;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.component.simple.EchoComponent;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.transport.soap.transformers.HttpRequestToSoapRequest;
import org.mule.util.object.PooledObjectFactory;

public class XFireEchoConfigutationBuilder extends AbstractConfigurationBuilder implements ConfigurationBuilder
{

    // @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        // Set defaults
        new DefaultsConfigurationBuilder().configure(muleContext);

        // Model
        Model model = new SedaModel();
        model.setName("model");
        muleContext.getRegistry().registerModel(model);

        // Service
        Service service = new SedaService();
        service.setName("EchoUMO");
        service.setModel(model);

        // - inbound
        InboundRouterCollection inbound = new DefaultInboundRouterCollection();
        EndpointBuilder builder = new EndpointURIEndpointBuilder(
            new URIBuilder("xfire:http://localhost:65081/services"), muleContext);
        builder.addTransformer(new HttpRequestToSoapRequest());
        inbound.addEndpoint(builder.buildInboundEndpoint());
        inbound.addEndpoint(new EndpointURIEndpointBuilder(new URIBuilder("xfire:http://localhost:65082/services"),
            muleContext).buildInboundEndpoint());
        service.setInboundRouter(inbound);

        // - component
        service.setServiceFactory(new PooledObjectFactory(EchoComponent.class));

        muleContext.getRegistry().registerService(service);
    }
}
