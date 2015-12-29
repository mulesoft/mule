/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.transformer.Transformer;
import org.mule.component.AbstractComponent;
import org.mule.component.PooledJavaComponent;
import org.mule.config.PoolingProfile;
import org.mule.construct.Flow;
import org.mule.interceptor.InterceptorStack;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.interceptor.TimerInterceptor;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.AbstractConnector;

import org.junit.Test;

public abstract class AbstractConfigBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{

    public AbstractConfigBuilderTestCase(boolean legacy)
    {
        super(legacy);
    }

    @Override
    protected boolean isGracefulShutdown()
    {
        return true;
    }

    @Override
    public void testManagerConfig() throws Exception
    {
        super.testManagerConfig();

        assertNotNull(muleContext.getTransactionManager());
    }

    @Override
    public void testGlobalEndpointConfig() throws MuleException
    {
        super.testGlobalEndpointConfig();
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("fruitBowlEndpoint");
        assertNotNull(endpoint);
        assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");

        MessagePropertyFilter filter = (MessagePropertyFilter) endpoint.getFilter();
        assertNotNull(filter);
        assertEquals("foo=bar", filter.getPattern());
    }

    @Override
    public void testEndpointConfig() throws MuleException
    {
        super.testEndpointConfig();

        // test that targets have been resolved on targets
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("waterMelonEndpoint");
        assertNotNull(endpoint);
        assertEquals("UTF-8-TEST", endpoint.getEncoding());
        assertEquals("test.queue", endpoint.getEndpointURI().getAddress());

        FlowConstruct service = muleContext.getRegistry().lookupFlowConstruct("appleComponent2");
        assertNotNull(service);
    }

    @Test
    public void testExceptionStrategy2()
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent");
        assertNotNull(flow.getExceptionListener());
        assertTrue(flow.getExceptionListener() instanceof MessagingExceptionHandler);
    }

    @Override
    public void testTransformerConfig()
    {
        super.testTransformerConfig();

        Transformer t = muleContext.getRegistry().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertEquals(t.getReturnDataType(), DataTypeFactory.STRING);
        assertNotNull(((TestCompressionTransformer) t).getContainerProperty());
    }

    @Test
    public void testThreadingConfig() throws DefaultMuleException
    {
        // expected default values from the configuration;
        // these should differ from the programmatic values!

        // globals
        int defaultMaxBufferSize = 42;
        int defaultMaxThreadsActive = 16;
        int defaultMaxThreadsIdle = 3;
        // WAIT is 0, RUN is 4
        int defaultThreadPoolExhaustedAction = ThreadingProfile.WHEN_EXHAUSTED_WAIT;
        int defaultThreadTTL = 60001;

        // for the connector
        int connectorMaxBufferSize = 2;

        // for the service
        int componentMaxBufferSize = 6;
        int componentMaxThreadsActive = 12;
        int componentMaxThreadsIdle = 6;
        int componentThreadPoolExhaustedAction = ThreadingProfile.WHEN_EXHAUSTED_DISCARD;

        // test default config
        ThreadingProfile tp = muleContext.getDefaultThreadingProfile();
        assertEquals(defaultMaxBufferSize, tp.getMaxBufferSize());
        assertEquals(defaultMaxThreadsActive, tp.getMaxThreadsActive());
        assertEquals(defaultMaxThreadsIdle, tp.getMaxThreadsIdle());
        assertEquals(defaultThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
        assertEquals(defaultThreadTTL, tp.getThreadTTL());

        // test service threading profile defaults
        tp = muleContext.getDefaultServiceThreadingProfile();
        assertEquals(defaultMaxBufferSize, tp.getMaxBufferSize());
        assertEquals(defaultMaxThreadsActive, tp.getMaxThreadsActive());
        assertEquals(defaultMaxThreadsIdle, tp.getMaxThreadsIdle());
        assertEquals(defaultThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
        assertEquals(defaultThreadTTL, tp.getThreadTTL());

        // test that unset values retain a default value
        AbstractConnector c = (AbstractConnector) muleContext.getRegistry().lookupConnector("dummyConnector");
        tp = c.getDispatcherThreadingProfile();
        // this value is configured
        assertEquals(connectorMaxBufferSize, tp.getMaxBufferSize());
        // these values are inherited
        assertEquals(defaultMaxThreadsActive, tp.getMaxThreadsActive());
        assertEquals(defaultMaxThreadsIdle, tp.getMaxThreadsIdle());
        assertEquals(defaultThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
        assertEquals(defaultThreadTTL, tp.getThreadTTL());

        // test per-service values
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent2");
        AsynchronousProcessingStrategy processingStrategy = (AsynchronousProcessingStrategy) flow.getProcessingStrategy();
        // these values are configured
        assertEquals(componentMaxBufferSize, processingStrategy.getMaxBufferSize().intValue());
        assertEquals(componentMaxThreadsActive, processingStrategy.getMaxThreads().intValue());
        assertEquals(componentThreadPoolExhaustedAction, processingStrategy.getPoolExhaustedAction().intValue());
        // this value is inherited
        assertEquals(defaultThreadTTL, tp.getThreadTTL());
    }

    @Test
    public void testPoolingConfig()
    {
        // test per-descriptor overrides
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent2");
        PoolingProfile pp = ((PooledJavaComponent) flow.getMessageProcessors().get(0)).getPoolingProfile();

        assertEquals(9, pp.getMaxActive());
        assertEquals(6, pp.getMaxIdle());
        assertEquals(4002, pp.getMaxWait());
        assertEquals(PoolingProfile.WHEN_EXHAUSTED_FAIL, pp.getExhaustedAction());
        assertEquals(PoolingProfile.INITIALISE_ALL, pp.getInitialisationPolicy());
    }

    @Test
    public void testEndpointProperties() throws Exception
    {
        // test transaction config
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent2");
        InboundEndpoint inEndpoint = (InboundEndpoint) ((CompositeMessageSource) flow.getMessageSource()).getSources().get(1);
        assertNotNull(inEndpoint);
        assertNotNull(inEndpoint.getProperties());
        assertEquals("Prop1", inEndpoint.getProperties().get("testEndpointProperty"));
    }

    @Test
    public void testEnvironmentProperties()
    {
        assertEquals("true", muleContext.getRegistry().lookupObject("doCompression"));
        assertEquals("this was set from the manager properties!", muleContext.getRegistry().lookupObject("beanProperty1"));
        assertNotNull(muleContext.getRegistry().lookupObject("OS_Version"));
    }

    @Test
    public void testMuleConfiguration()
    {
        assertEquals(10, muleContext.getConfiguration().getDefaultResponseTimeout());
        assertEquals(20, muleContext.getConfiguration().getDefaultTransactionTimeout());
        assertEquals(30, muleContext.getConfiguration().getShutdownTimeout());
    }

    @Test
    public void testGlobalInterceptorStack()
    {
        InterceptorStack interceptorStack = (InterceptorStack) muleContext.getRegistry().lookupObject(
                "testInterceptorStack");
        assertNotNull(interceptorStack);
        assertEquals(3, interceptorStack.getInterceptors().size());
        assertEquals(LoggingInterceptor.class, interceptorStack.getInterceptors().get(0).getClass());
        assertEquals(TimerInterceptor.class, interceptorStack.getInterceptors().get(1).getClass());
        assertEquals(LoggingInterceptor.class, interceptorStack.getInterceptors().get(2).getClass());
    }

    @Test
    public void testInterceptors()
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("orangeComponent");
        AbstractComponent component = (AbstractComponent) flow.getMessageProcessors().get(0);
        assertEquals(3, component.getInterceptors().size());
        assertEquals(LoggingInterceptor.class, component.getInterceptors().get(0).getClass());
        assertEquals(InterceptorStack.class, component.getInterceptors().get(1).getClass());
        assertEquals(TimerInterceptor.class, component.getInterceptors().get(2).getClass());
    }

}
