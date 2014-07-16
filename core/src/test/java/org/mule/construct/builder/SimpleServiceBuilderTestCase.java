/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct.builder;

import static org.junit.Assert.assertEquals;

import org.mule.component.AbstractJavaComponent;
import org.mule.component.DefaultJavaComponent;
import org.mule.component.SimpleCallableJavaComponent;
import org.mule.component.simple.EchoComponent;
import org.mule.construct.SimpleService;
import org.mule.construct.SimpleService.Type;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.services.SimpleMathsComponent;
import org.mule.transformer.compression.GZipCompressTransformer;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.StringAppendTransformer;

import org.junit.Test;

public class SimpleServiceBuilderTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testFullConfiguration() throws Exception
    {
        SimpleService simpleService = new SimpleServiceBuilder().name("test-simple-service-full")
            .inboundAddress("test://foo")
            .transformers(new StringAppendTransformer("bar"))
            .responseTransformers(new ObjectToByteArray(), new GZipCompressTransformer())
            .component(EchoComponent.class)
            .type(Type.DIRECT)
            .exceptionStrategy(new DefaultMessagingExceptionStrategy(muleContext))
            .build(muleContext);

        assertEquals("test-simple-service-full", simpleService.getName());
        assertEquals(EchoComponent.class,
            ((AbstractJavaComponent) simpleService.getComponent()).getObjectType());
    }

    @Test
    public void testShortConfiguration() throws Exception
    {
        SimpleService simpleService = new SimpleServiceBuilder().name("test-simple-service-short")
            .inboundEndpoint(getTestInboundEndpoint("test"))
            .component(new EchoComponent())
            .build(muleContext);

        assertEquals("test-simple-service-short", simpleService.getName());
        assertEquals(EchoComponent.class,
            ((SimpleCallableJavaComponent) simpleService.getComponent()).getObjectType());
    }

    @Test
    public void testPojoComponentConfiguration() throws Exception
    {
        SimpleMathsComponent pojoComponent = new SimpleMathsComponent();

        SimpleService simpleService = new SimpleServiceBuilder().name("test-simple-service-pojo-component")
            .inboundEndpoint(getTestInboundEndpoint("test"))
            .component(pojoComponent)
            .build(muleContext);

        assertEquals("test-simple-service-pojo-component", simpleService.getName());
        assertEquals(pojoComponent, ((DefaultJavaComponent) simpleService.getComponent()).getObjectFactory()
            .getInstance(muleContext));
    }
}
