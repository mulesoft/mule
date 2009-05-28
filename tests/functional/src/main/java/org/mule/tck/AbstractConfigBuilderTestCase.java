/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.DefaultExceptionStrategy;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.component.JavaComponent;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.InboundRouter;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.routing.InterfaceBinding;
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
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.inbound.IdempotentReceiver;
import org.mule.routing.inbound.SelectiveConsumer;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.testmodels.mule.TestCatchAllStrategy;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transformer.TransformerUtils;
import org.mule.transport.AbstractConnector;

public abstract class AbstractConfigBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{

    public AbstractConfigBuilderTestCase(boolean legacy)
    {
        super(legacy);
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

        TestConnector c = (TestConnector) muleContext.getRegistry().lookupConnector("dummyConnector");
        assertNotNull(c);
        assertNotNull(c.getExceptionListener());
        assertTrue(c.getExceptionListener() instanceof TestExceptionStrategy);
    }

    @Override
    public void testGlobalEndpointConfig() throws MuleException
    {
        super.testGlobalEndpointConfig();
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("fruitBowlEndpoint");
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

        // test that endpoints have been resolved on endpoints
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("waterMelonEndpoint");
        assertNotNull(endpoint);
        assertEquals("UTF-8-TEST", endpoint.getEncoding());
        assertEquals("test.queue", endpoint.getEndpointURI().getAddress());

        Service service = muleContext.getRegistry().lookupService("appleComponent2");
        assertNotNull(service);
    }

    public void testExceptionStrategy2()
    {
        Service service = muleContext.getRegistry().lookupService("appleComponent");
        assertNotNull(service.getExceptionListener());
        assertTrue(DefaultExceptionStrategy.class.isAssignableFrom(service.getExceptionListener().getClass()));
    }

    @Override
    public void testTransformerConfig()
    {
        super.testTransformerConfig();

        Transformer t = muleContext.getRegistry().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertEquals(t.getReturnClass(), java.lang.String.class);
        assertNotNull(((TestCompressionTransformer) t).getContainerProperty());
    }

    @Override
    public void testModelConfig() throws Exception
    {
        super.testModelConfig();        
        assertNotNull(muleContext.getRegistry().lookupService("appleComponent"));
        assertNotNull(muleContext.getRegistry().lookupService("appleComponent2"));
    }

    public void testOutboundRouterConfig2()
    {
        // test outbound message router
        Service service = muleContext.getRegistry().lookupService("appleComponent");
        assertNotNull(service.getOutboundRouter());
        OutboundRouterCollection router = service.getOutboundRouter();
        assertNotNull(router.getCatchAllStrategy());
        assertEquals(2, router.getRouters().size());
        // check first Router
        OutboundRouter route1 = (OutboundRouter) router.getRouters().get(0);
        assertTrue(route1 instanceof FilteringOutboundRouter);
        assertNotNull(((FilteringOutboundRouter) route1).getTransformers());
        assertTrue(TransformerUtils.firstOrNull(((FilteringOutboundRouter) route1).getTransformers())
                instanceof TestCompressionTransformer);

        Filter filter = ((FilteringOutboundRouter) route1).getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof PayloadTypeFilter);
        assertEquals(String.class, ((PayloadTypeFilter) filter).getExpectedType());

        // check second Router
        OutboundRouter route2 = (OutboundRouter) router.getRouters().get(1);
        assertTrue(route2 instanceof FilteringOutboundRouter);

        Filter filter2 = ((FilteringOutboundRouter) route2).getFilter();
        assertNotNull(filter2);
        assertTrue(filter2 instanceof AndFilter);
        assertEquals(2,  ((AndFilter) filter2).getFilters().size());
        Filter left = (Filter) ((AndFilter) filter2).getFilters().get(0);
        Filter right = (Filter) ((AndFilter) filter2).getFilters().get(1);
        assertNotNull(left);
        assertTrue(left instanceof RegExFilter);
        assertEquals("the quick brown (.*)", ((RegExFilter) left).getPattern());
        assertNotNull(right);
        assertTrue(right instanceof RegExFilter);
        assertEquals("(.*) brown (.*)", ((RegExFilter) right).getPattern());

        assertTrue(router.getCatchAllStrategy() instanceof TestCatchAllStrategy);
    }


    public void testInboundRouterConfig2()
    {
        Service service = muleContext.getRegistry().lookupService("appleComponent");
        assertNotNull(service.getInboundRouter());
        InboundRouterCollection messageRouter = service.getInboundRouter();
        assertNotNull(messageRouter.getCatchAllStrategy());
        assertEquals(2, messageRouter.getRouters().size());
        InboundRouter router = (InboundRouter) messageRouter.getRouters().get(0);
        assertTrue(router instanceof SelectiveConsumer);
        SelectiveConsumer sc = (SelectiveConsumer) router;

        assertNotNull(sc.getFilter());
        Filter filter = sc.getFilter();
        // check first Router
        assertTrue(filter instanceof PayloadTypeFilter);
        assertEquals(String.class, ((PayloadTypeFilter) filter).getExpectedType());

        InboundRouter router2 = (InboundRouter) messageRouter.getRouters().get(1);
        assertTrue(router2 instanceof IdempotentReceiver);
    }

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

    public void testPoolingConfig()
    {
//        //TODO RM* test config
//        PoolingProfile pp = RegistryContext.getConfiguration().getPoolingProfile();
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

    public void testQueueProfileConfig()
    {
//        // test config
//        //TODO RM*
//        QueueProfile qp = RegistryContext.getConfiguration().getQueueProfile();
//        assertEquals(100, qp.getMaxOutstandingMessages());
//        assertTrue(qp.isPersistent());

        // test inherit
        Service service = muleContext.getRegistry().lookupService("appleComponent2");
        QueueProfile qp = ((SedaService)service).getQueueProfile();
        assertEquals(102, qp.getMaxOutstandingMessages());
        assertTrue(qp.isPersistent());

        // test override
//        descriptor = (MuleDescriptor)muleContext.getModel().getDescriptor("appleComponent2");
//        qp = descriptor.getQueueProfile();
//        assertEquals(102, qp.getMaxOutstandingMessages());
//        assertFalse(qp.isPersistent());
    }

    public void testEndpointProperties() throws Exception
    {
        // test transaction config
        Service service = muleContext.getRegistry().lookupService("appleComponent2");
        InboundEndpoint inEndpoint = service.getInboundRouter().getEndpoint(
                "transactedInboundEndpoint");
        assertNotNull(inEndpoint);
        assertNotNull(inEndpoint.getProperties());
        assertEquals("Prop1", inEndpoint.getProperties().get("testEndpointProperty"));
    }

    public void testTransactionConfig() throws Exception
    {
        // test transaction config
        Service apple = muleContext.getRegistry().lookupService("appleComponent2");
        InboundEndpoint inEndpoint = apple.getInboundRouter().getEndpoint("transactedInboundEndpoint");
        assertNotNull(inEndpoint);
        assertEquals(1, apple.getOutboundRouter().getRouters().size());
        assertNotNull(inEndpoint.getTransactionConfig());
        assertEquals(TransactionConfig.ACTION_ALWAYS_BEGIN, inEndpoint.getTransactionConfig().getAction());
        assertTrue(inEndpoint.getTransactionConfig().getFactory() instanceof TestTransactionFactory);
        assertNull(inEndpoint.getTransactionConfig().getConstraint());

        OutboundRouter outRouter = (OutboundRouter) apple.getOutboundRouter().getRouters().get(0);
        OutboundEndpoint outEndpoint = (OutboundEndpoint) outRouter.getEndpoints().get(0);
        assertNotNull(outEndpoint);
    }

    public void testEnvironmentProperties()
    {
        assertEquals("true", muleContext.getRegistry().lookupObject("doCompression"));
        assertEquals("this was set from the manager properties!", muleContext.getRegistry().lookupObject("beanProperty1"));
        assertNotNull(muleContext.getRegistry().lookupObject("OS_Version"));
    }


    public void testBindngProxyCreation()
    {
        //Test that the proxy object was created and set on the service object
        Service orange = muleContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(orange);
        assertTrue(orange.getComponent() instanceof JavaComponent);
        InterfaceBinding r = (InterfaceBinding) ((JavaComponent) orange.getComponent()).getBindingCollection().getRouters().get(0);
        assertNotNull(r);

        //TODO Grab an instance of the service object itself and test that the proxy has been injected
    }
    
    public void testMuleConfiguration()
    {
        assertTrue(muleContext.getConfiguration().isDefaultSynchronousEndpoints());
    }

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

    public void testInterceptors()
    {
        Service service = muleContext.getRegistry().lookupService("orangeComponent");
        InterceptorStack globalInterceptorStack = (InterceptorStack) muleContext.getRegistry().lookupObject(
            "testInterceptorStack");
        AbstractComponent component = (AbstractComponent) service.getComponent();
        assertEquals(3, component.getInterceptors().size());
        assertEquals(LoggingInterceptor.class, component.getInterceptors().get(0).getClass());
        assertEquals(globalInterceptorStack, component.getInterceptors().get(1));
        assertEquals(TimerInterceptor.class, component.getInterceptors().get(2).getClass());
    }
    
}
