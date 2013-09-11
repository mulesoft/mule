/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.component.InterfaceBinding;
import org.mule.api.component.JavaComponent;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.filter.Filter;
import org.mule.api.service.Service;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.component.AbstractComponent;
import org.mule.component.PooledJavaComponent;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.interceptor.InterceptorStack;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.interceptor.TimerInterceptor;
import org.mule.model.seda.SedaService;
import org.mule.routing.IdempotentMessageFilter;
import org.mule.routing.MessageFilter;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.testmodels.mule.TestCatchAllStrategy;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.AbstractConnector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
    public void testConnectorConfig() throws Exception
    {
        super.testConnectorConfig();

        MessagingExceptionHandler es = muleContext.getRegistry().lookupModel("main").getExceptionListener();
        assertNotNull(es);
        assertTrue(es.getClass().getName(), es instanceof TestExceptionStrategy);
    }

    @Override
    public void testGlobalEndpointConfig() throws MuleException
    {
        super.testGlobalEndpointConfig();
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("fruitBowlEndpoint");
        assertNotNull(endpoint);
        assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");

        MessagePropertyFilter filter = (MessagePropertyFilter)endpoint.getFilter();
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

        Service service = muleContext.getRegistry().lookupService("appleComponent2");
        assertNotNull(service);
    }

    @Test
    public void testExceptionStrategy2()
    {
        Service service = muleContext.getRegistry().lookupService("appleComponent");
        assertNotNull(service.getExceptionListener());
        assertTrue(service.getExceptionListener() instanceof MessagingExceptionHandler);
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

    @Override
    public void testModelConfig() throws Exception
    {
        super.testModelConfig();        
        assertNotNull(muleContext.getRegistry().lookupService("appleComponent"));
        assertNotNull(muleContext.getRegistry().lookupService("appleComponent2"));
    }

    @Test
    public void testOutboundRouterConfig2()
    {
        // test outbound message router
        Service service = muleContext.getRegistry().lookupService("appleComponent");
        assertNotNull(service.getOutboundMessageProcessor());
        OutboundRouterCollection router = (OutboundRouterCollection) service.getOutboundMessageProcessor();
        assertNotNull(router.getCatchAllStrategy());
        assertEquals(2, router.getRoutes().size());
        // check first Router
        OutboundRouter route1 = (OutboundRouter) router.getRoutes().get(0);
        assertTrue(route1 instanceof FilteringOutboundRouter);
        assertEquals(1, route1.getRoutes().size());
        ImmutableEndpoint ep = (ImmutableEndpoint) route1.getRoutes().get(0);
        
        assertNotNull(ep.getTransformers());
        assertTrue(TransformerUtils.firstOrNull(ep.getTransformers()) instanceof TestCompressionTransformer);

        Filter filter = ((FilteringOutboundRouter) route1).getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof PayloadTypeFilter);
        assertEquals(String.class, ((PayloadTypeFilter) filter).getExpectedType());

        // check second Router
        OutboundRouter route2 = (OutboundRouter) router.getRoutes().get(1);
        assertTrue(route2 instanceof FilteringOutboundRouter);

        Filter filter2 = ((FilteringOutboundRouter) route2).getFilter();
        assertNotNull(filter2);
        assertTrue(filter2 instanceof AndFilter);
        assertEquals(2,  ((AndFilter) filter2).getFilters().size());
        Filter left = ((AndFilter) filter2).getFilters().get(0);
        Filter right = ((AndFilter) filter2).getFilters().get(1);
        assertNotNull(left);
        assertTrue(left instanceof RegExFilter);
        assertEquals("the quick brown (.*)", ((RegExFilter) left).getPattern());
        assertNotNull(right);
        assertTrue(right instanceof RegExFilter);
        assertEquals("(.*) brown (.*)", ((RegExFilter) right).getPattern());

        assertTrue(router.getCatchAllStrategy() instanceof TestCatchAllStrategy);
    }


    @Test
    public void testInboundRouterConfig2()
    {
        Service service = muleContext.getRegistry().lookupService("appleComponent");
        assertNotNull(service.getMessageSource());
        ServiceCompositeMessageSource messageRouter = (ServiceCompositeMessageSource) service.getMessageSource();
        assertNotNull(messageRouter.getCatchAllStrategy());
        assertEquals(2, messageRouter.getMessageProcessors().size());
        MessageProcessor router = messageRouter.getMessageProcessors().get(0);
        assertTrue(router instanceof MessageFilter);
        MessageFilter sc = (MessageFilter) router;

        assertNotNull(sc.getFilter());
        Filter filter = sc.getFilter();
        // check first Router
        assertTrue(filter instanceof PayloadTypeFilter);
        assertEquals(String.class, ((PayloadTypeFilter) filter).getExpectedType());

        MessageProcessor router2 = messageRouter.getMessageProcessors().get(1);
        assertTrue(router2 instanceof IdempotentMessageFilter);
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
        Service service = muleContext.getRegistry().lookupService("appleComponent2");
        assertTrue("service must be SedaService to get threading profile", service instanceof SedaService);
        tp = ((SedaService) service).getThreadingProfile();
        // these values are configured
        assertEquals(componentMaxBufferSize, tp.getMaxBufferSize());
        assertEquals(componentMaxThreadsActive, tp.getMaxThreadsActive());
        assertEquals(componentMaxThreadsIdle, tp.getMaxThreadsIdle());
        assertEquals(componentThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
        // this value is inherited
        assertEquals(defaultThreadTTL, tp.getThreadTTL());
    }

    @Test
    public void testPoolingConfig()
    {
//        //TODO RM* test config
//        PoolingProfile pp = muleContext.getConfiguration().getPoolingProfile();
//        assertEquals(10, pp.getMaxActive());
//        assertEquals(5, pp.getMaxIdle());
//        assertEquals(10001, pp.getMaxWait());
//        assertEquals(ObjectPool.WHEN_EXHAUSTED_WAIT, pp.getExhaustedAction());
//        assertEquals(PoolingProfile.INITIALISE_ONE, pp.getInitialisationPolicy());
//        assertTrue(pp.getPoolFactory() instanceof CommonsPoolFactory);

        // test per-descriptor overrides
        Service service = muleContext.getRegistry().lookupService("appleComponent2");
        PoolingProfile pp = ((PooledJavaComponent)service.getComponent()).getPoolingProfile();

        assertEquals(9, pp.getMaxActive());
        assertEquals(6, pp.getMaxIdle());
        assertEquals(4002, pp.getMaxWait());
        assertEquals(PoolingProfile.WHEN_EXHAUSTED_FAIL, pp.getExhaustedAction());
        assertEquals(PoolingProfile.INITIALISE_ALL, pp.getInitialisationPolicy());
    }

    @Test
    public void testQueueProfileConfig()
    {
//        // test config
//        //TODO RM*
//        QueueProfile qp = muleContext.getConfiguration().getQueueProfile();
//        assertEquals(100, qp.getMaxOutstandingMessages());
//        assertTrue(qp.isPersistent());

        // test inherit
        Service service = muleContext.getRegistry().lookupService("appleComponent2");
        QueueProfile qp = ((SedaService)service).getQueueProfile();
        assertEquals(102, qp.getMaxOutstandingMessages());
        //assertTrue(qp.isPersistent());

        // test override
//        descriptor = (MuleDescriptor)muleContext.getModel().getDescriptor("appleComponent2");
//        qp = descriptor.getQueueProfile();
//        assertEquals(102, qp.getMaxOutstandingMessages());
//        assertFalse(qp.isPersistent());
    }

    @Test
    public void testEndpointProperties() throws Exception
    {
        // test transaction config
        Service service = muleContext.getRegistry().lookupService("appleComponent2");
        InboundEndpoint inEndpoint = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoint(
                "transactedInboundEndpoint");
        assertNotNull(inEndpoint);
        assertNotNull(inEndpoint.getProperties());
        assertEquals("Prop1", inEndpoint.getProperties().get("testEndpointProperty"));
    }

    @Test
    public void testTransactionConfig() throws Exception
    {
        // test transaction config
        Service apple = muleContext.getRegistry().lookupService("appleComponent2");
        InboundEndpoint inEndpoint = ((ServiceCompositeMessageSource) apple.getMessageSource()).getEndpoint("transactedInboundEndpoint");
        assertNotNull(inEndpoint);
        assertEquals(1, ((OutboundRouterCollection) apple.getOutboundMessageProcessor()).getRoutes().size());
        assertNotNull(inEndpoint.getTransactionConfig());
        assertEquals(TransactionConfig.ACTION_ALWAYS_BEGIN, inEndpoint.getTransactionConfig().getAction());
        assertTrue(inEndpoint.getTransactionConfig().getFactory() instanceof TestTransactionFactory);
        assertNull(inEndpoint.getTransactionConfig().getConstraint());

        OutboundRouter outRouter = (OutboundRouter) ((OutboundRouterCollection)apple.getOutboundMessageProcessor()).getRoutes().get(0);
        MessageProcessor outEndpoint = outRouter.getRoutes().get(0);
        assertNotNull(outEndpoint);
    }

    @Test
    public void testEnvironmentProperties()
    {
        assertEquals("true", muleContext.getRegistry().lookupObject("doCompression"));
        assertEquals("this was set from the manager properties!", muleContext.getRegistry().lookupObject("beanProperty1"));
        assertNotNull(muleContext.getRegistry().lookupObject("OS_Version"));
    }


    @Test
    public void testBindngProxyCreation()
    {
        //Test that the proxy object was created and set on the service object
        Service orange = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(orange);
        assertTrue(orange.getComponent() instanceof JavaComponent);
        InterfaceBinding r = ((JavaComponent) orange.getComponent()).getInterfaceBindings().get(0);
        assertNotNull(r);

        //TODO Grab an instance of the service object itself and test that the proxy has been injected
    }
    
    @Test
    public void testMuleConfiguration()
    {
        assertEquals(10,muleContext.getConfiguration().getDefaultResponseTimeout());
        assertEquals(20,muleContext.getConfiguration().getDefaultTransactionTimeout());
        assertEquals(30,muleContext.getConfiguration().getShutdownTimeout());
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
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        AbstractComponent component = (AbstractComponent) service.getComponent();
        assertEquals(3, component.getInterceptors().size());
        assertEquals(LoggingInterceptor.class, component.getInterceptors().get(0).getClass());
        assertEquals(InterceptorStack.class, component.getInterceptors().get(1).getClass());
        assertEquals(TimerInterceptor.class, component.getInterceptors().get(2).getClass());
    }
    
}
