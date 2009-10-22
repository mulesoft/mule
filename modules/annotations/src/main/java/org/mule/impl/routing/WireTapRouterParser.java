/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.routing;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.RouterAnnotationParser;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.Router;
import org.mule.config.annotations.converters.PropertiesConverter;
import org.mule.config.annotations.routing.WireTap;
import org.mule.impl.endpoint.AnnotatedEndpointBuilder;
import org.mule.impl.endpoint.AnnotatedEndpointData;
import org.mule.impl.endpoint.MEP;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Map;

/**
 * Parses a {@link org.mule.config.annotations.routing.WireTap} annotation into a Mule {@link org.mule.routing.inbound.WireTap}
 * and registers it with the service it is configured on.
 */
public class WireTapRouterParser implements RouterAnnotationParser, MuleContextAware
{
    protected MuleContext muleContext;
    private AnnotatedEndpointBuilder builder;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        try
        {
            this.builder = new AnnotatedEndpointBuilder(muleContext);
        }
        catch (MuleException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Router parseRouter(Annotation annotation) throws MuleException
    {
        WireTap wireTap = (WireTap) annotation;

        AnnotatedEndpointData epd = new AnnotatedEndpointData(MEP.InOnly);
        epd.setEncoding(wireTap.encoding());
        epd.setProperties((Map) new PropertiesConverter().convert(wireTap.properties(), muleContext));
        epd.setConnectorName(wireTap.connectorName());
        epd.setAddress(wireTap.endpoint());
        epd.setFilter(wireTap.filter());
        epd.setTransformers(wireTap.transformers());
        org.mule.routing.inbound.WireTap wireTapRouter = new org.mule.routing.inbound.WireTap();
        OutboundEndpoint endpoint = (OutboundEndpoint) builder.processEndpoint(epd);
        wireTapRouter.setEndpoint(endpoint);
        return wireTapRouter;
    }

    public boolean supports(Annotation annotation, Class clazz, Member member)
    {
        return annotation instanceof WireTap;
    }
}
