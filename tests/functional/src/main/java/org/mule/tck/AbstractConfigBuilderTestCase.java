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

import org.mule.MuleException;
import org.mule.RegistryContext;
import org.mule.config.ThreadingProfile;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.providers.service.TransportFactory;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.inbound.IdempotentReceiver;
import org.mule.routing.inbound.SelectiveConsumer;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.testmodels.mule.TestCatchAllStrategy;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOFilter;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.model.UMOModel;
import org.mule.umo.routing.UMOInboundRouter;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouter;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.transformer.UMOTransformer;

public abstract class AbstractConfigBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{

    public AbstractConfigBuilderTestCase(boolean legacy)
    {
        super(legacy);
    }


    // @Override
    public void testManagerConfig() throws Exception
    {
        super.testManagerConfig();

        assertNotNull(managementContext.getTransactionManager());
    }

    // @Override
    public void testConnectorConfig() throws Exception
    {
        super.testConnectorConfig();

        TestConnector c = (TestConnector) managementContext.getRegistry().lookupConnector("dummyConnector");
        assertNotNull(c);
        assertNotNull(c.getExceptionListener());
        assertTrue(c.getExceptionListener() instanceof TestExceptionStrategy);
        //TODO RM* Move to the endpoint
//        assertNotNull(c.getConnectionStrategy());
//        assertTrue(c.getConnectionStrategy() instanceof SimpleRetryConnectionStrategy);
//        assertEquals(4, ((SimpleRetryConnectionStrategy)c.getConnectionStrategy()).getRetryCount());
//        assertEquals(3000, ((SimpleRetryConnectionStrategy)c.getConnectionStrategy()).getFrequency());
    }

    // @Override
    public void testGlobalEndpointConfig()
    {
        super.testGlobalEndpointConfig();

        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpoint("fruitBowlEndpoint", managementContext);
        assertNotNull(endpoint);
        assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");
        assertNotNull(endpoint.getFilter());
        JXPathFilter filter = (JXPathFilter) endpoint.getFilter();
        assertEquals("name", filter.getPattern());
        assertEquals("bar", filter.getExpectedValue());
        assertNotNull(filter.getNamespaces());
        assertEquals("http://foo.com", filter.getNamespaces().get("foo"));
    }

    // @Override
    public void testEndpointConfig()
    {
        super.testEndpointConfig();

        // test that endpoints have been resolved on endpoints
        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpoint("waterMelonEndpoint", managementContext);
        assertNotNull(endpoint);
        assertEquals("test.queue", endpoint.getEndpointURI().getAddress());

        UMODescriptor descriptor = managementContext.getRegistry().lookupService("appleComponent2");
        assertNotNull(descriptor);
    }

    public void testExceptionStrategy2()
    {
        UMODescriptor descriptor = managementContext.getRegistry().lookupService("appleComponent");
        assertNotNull(descriptor.getExceptionListener());
        assertTrue(DefaultExceptionStrategy.class.isAssignableFrom(descriptor.getExceptionListener().getClass()));
    }

    // @Override
    public void testTransformerConfig()
    {
        super.testTransformerConfig();

        UMOTransformer t = managementContext.getRegistry().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertEquals(t.getReturnClass(), java.lang.String.class);
        assertNotNull(((TestCompressionTransformer) t).getContainerProperty());
    }

    // @Override
    public void testModelConfig() throws Exception
    {
        super.testModelConfig();

        UMOModel model = managementContext.getRegistry().lookupModel("main");
        super.testModelConfig();
        assertTrue(model.isComponentRegistered("appleComponent"));
        assertTrue(model.isComponentRegistered("appleComponent2"));
    }

    public void testOutboundRouterConfig2()
    {
        // test outbound message router
        UMODescriptor descriptor = managementContext.getRegistry().lookupService("appleComponent");
        assertNotNull(descriptor.getOutboundRouter());
        UMOOutboundRouterCollection router = descriptor.getOutboundRouter();
        assertNotNull(router.getCatchAllStrategy());
        assertEquals(2, router.getRouters().size());
        // check first Router
        UMOOutboundRouter route1 = (UMOOutboundRouter) router.getRouters().get(0);
        assertTrue(route1 instanceof FilteringOutboundRouter);
        assertNotNull(((FilteringOutboundRouter) route1).getTransformers());
        assertTrue(TransformerUtils.firstOrNull(((FilteringOutboundRouter) route1).getTransformers())
                instanceof TestCompressionTransformer);

        UMOFilter filter = ((FilteringOutboundRouter) route1).getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof PayloadTypeFilter);
        assertEquals(String.class, ((PayloadTypeFilter) filter).getExpectedType());

        // check second Router
        UMOOutboundRouter route2 = (UMOOutboundRouter) router.getRouters().get(1);
        assertTrue(route2 instanceof FilteringOutboundRouter);

        UMOFilter filter2 = ((FilteringOutboundRouter) route2).getFilter();
        assertNotNull(filter2);
        assertTrue(filter2 instanceof AndFilter);
        UMOFilter left = ((AndFilter) filter2).getLeftFilter();
        UMOFilter right = ((AndFilter) filter2).getRightFilter();
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
        UMODescriptor descriptor = managementContext.getRegistry().lookupService("appleComponent");
        assertNotNull(descriptor.getInboundRouter());
        UMOInboundRouterCollection messageRouter = descriptor.getInboundRouter();
        assertNotNull(messageRouter.getCatchAllStrategy());
        assertEquals(2, messageRouter.getRouters().size());
        UMOInboundRouter router = (UMOInboundRouter) messageRouter.getRouters().get(0);
        assertTrue(router instanceof SelectiveConsumer);
        SelectiveConsumer sc = (SelectiveConsumer) router;

        assertNotNull(sc.getFilter());
        UMOFilter filter = sc.getFilter();
        // check first Router
        assertTrue(filter instanceof PayloadTypeFilter);
        assertEquals(String.class, ((PayloadTypeFilter) filter).getExpectedType());

        UMOInboundRouter router2 = (UMOInboundRouter) messageRouter.getRouters().get(1);
        assertTrue(router2 instanceof IdempotentReceiver);
    }

    public void testThreadingConfig() throws MuleException
    {
        // expected default values from the configuration;
        // these should differ from the programmatic values!

        // globals
        int defaultMaxBufferSize = 42;
        int defaultMaxThreadsActive = 16;
        int defaultMaxThreadsIdle = 3;
        int defaultThreadPoolExhaustedAction = ThreadingProfile.WHEN_EXHAUSTED_WAIT;
        int defaultThreadTTL = 60001;

        // for the connector
        int connectorMaxBufferSize = 2;

        // for the component
        int componentMaxBufferSize = 6;
        int componentMaxThreadsActive = 12;
        int componentMaxThreadsIdle = 6;
        int componentThreadPoolExhaustedAction = ThreadingProfile.WHEN_EXHAUSTED_DISCARD;

        // test default config
        ThreadingProfile tp = RegistryContext.getConfiguration().getDefaultThreadingProfile();
        assertEquals(defaultMaxBufferSize, tp.getMaxBufferSize());
        assertEquals(defaultMaxThreadsActive, tp.getMaxThreadsActive());
        assertEquals(defaultMaxThreadsIdle, tp.getMaxThreadsIdle());
        assertEquals(defaultThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
        assertEquals(defaultThreadTTL, tp.getThreadTTL());

        // test component threading profile defaults
        tp = RegistryContext.getConfiguration().getDefaultComponentThreadingProfile();
        assertEquals(defaultMaxBufferSize, tp.getMaxBufferSize());
        assertEquals(defaultMaxThreadsActive, tp.getMaxThreadsActive());
        assertEquals(defaultMaxThreadsIdle, tp.getMaxThreadsIdle());
        assertEquals(defaultThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
        assertEquals(defaultThreadTTL, tp.getThreadTTL());

        // test that unset values retain a default value
        AbstractConnector c = (AbstractConnector) managementContext.getRegistry().lookupConnector(
            "dummyConnector");
        tp = c.getDispatcherThreadingProfile();
        // this value is configured
        assertEquals(connectorMaxBufferSize, tp.getMaxBufferSize());
        // these values are inherited
        assertEquals(defaultMaxThreadsActive, tp.getMaxThreadsActive());
        assertEquals(defaultMaxThreadsIdle, tp.getMaxThreadsIdle());
        // MULE-2469
//        assertEquals(defaultThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
//        assertEquals(defaultThreadTTL, tp.getThreadTTL());

        // test per-component values
        MuleDescriptor descriptor = (MuleDescriptor) managementContext.getRegistry().lookupService(
            "appleComponent2");
        tp = descriptor.getThreadingProfile();
        // these values are configured
        assertEquals(componentMaxBufferSize, tp.getMaxBufferSize());
        assertEquals(componentMaxThreadsActive, tp.getMaxThreadsActive());
        assertEquals(componentMaxThreadsIdle, tp.getMaxThreadsIdle());
        assertEquals(componentThreadPoolExhaustedAction, tp.getPoolExhaustedAction());
        // this value is inherited
        // MULE-2469
//         assertEquals(defaultThreadTTL, tp.getThreadTTL());
    }

    public void testPoolingConfig()
    {
        //TODO RM* test config
//        PoolingProfile pp = RegistryContext.getConfiguration().getPoolingProfile();
//        assertEquals(10, pp.getMaxActive());
//        assertEquals(5, pp.getMaxIdle());
//        assertEquals(10001, pp.getMaxWait());
//        assertEquals(ObjectPool.WHEN_EXHAUSTED_WAIT, pp.getExhaustedAction());
//        assertEquals(PoolingProfile.INITIALISE_ONE, pp.getInitialisationPolicy());
//        assertTrue(pp.getPoolFactory() instanceof CommonsPoolFactory);

        // test per-descriptor overrides
        MuleDescriptor descriptor = (MuleDescriptor) managementContext.getRegistry().lookupService(
                "appleComponent2");
//        PoolingProfile pp = descriptor.getPoolingProfile();
//
//        assertEquals(9, pp.getMaxActive());
//        assertEquals(6, pp.getMaxIdle());
//        assertEquals(4002, pp.getMaxWait());
//        assertEquals(ObjectPool.WHEN_EXHAUSTED_FAIL, pp.getExhaustedAction());
//        assertEquals(PoolingProfile.INITIALISE_ALL, pp.getInitialisationPolicy());
    }

    public void testQueueProfileConfig()
    {
        // test config
        //TODO RM*
//        QueueProfile qp = RegistryContext.getConfiguration().getQueueProfile();
//        assertEquals(100, qp.getMaxOutstandingMessages());
//        assertTrue(qp.isPersistent());

        // test inherit
        MuleDescriptor descriptor = (MuleDescriptor) managementContext.getRegistry().lookupService(
                "orangeComponent");
//        QueueProfile qp = descriptor.getQueueProfile();
//        assertEquals(100, qp.getMaxOutstandingMessages());
//        assertTrue(qp.isPersistent());
//
//        // test override
//        descriptor = (MuleDescriptor)managementContext.getModel().getDescriptor("appleComponent2");
//        qp = descriptor.getQueueProfile();
//        assertEquals(102, qp.getMaxOutstandingMessages());
//        assertFalse(qp.isPersistent());
    }

    public void testEndpointProperties() throws Exception
    {
        // test transaction config
        UMODescriptor descriptor = managementContext.getRegistry().lookupService("appleComponent2");
        MuleEndpoint inEndpoint = (MuleEndpoint) descriptor.getInboundRouter().getEndpoint(
                "transactedInboundEndpoint");
        assertNotNull(inEndpoint);
        assertEquals(TransportFactory.NEVER_CREATE_CONNECTOR, inEndpoint.getCreateConnector());
        assertNotNull(inEndpoint.getProperties());
        assertEquals("Prop1", inEndpoint.getProperties().get("testEndpointProperty"));
    }

// TODO MULE-2185 Transaction config needs some work
//    public void testTransactionConfig() throws Exception
//    {
//        // test transaction config
//        UMODescriptor descriptor = managementContext.getRegistry().lookupService("appleComponent2");
//        UMOEndpoint inEndpoint = descriptor.getInboundRouter().getEndpoint("transactedInboundEndpoint");
//        assertNotNull(inEndpoint);
//        assertEquals(1, descriptor.getOutboundRouter().getRouters().size());
//
//        UMOEndpoint outEndpoint = (UMOEndpoint) ((UMOOutboundRouter) descriptor.getOutboundRouter()
//                .getRouters()
//                .get(0)).getEndpoints().get(0);
//
//        assertNotNull(outEndpoint);
//        assertNotNull(inEndpoint.getTransactionConfig());
//        assertEquals(UMOTransactionConfig.ACTION_ALWAYS_BEGIN, inEndpoint.getTransactionConfig().getAction());
//        assertTrue(inEndpoint.getTransactionConfig().getFactory() instanceof TestTransactionFactory);
//        assertNull(inEndpoint.getTransactionConfig().getConstraint());
//    }

    public void testEnvironmentProperties()
    {
        assertEquals("true", managementContext.getRegistry().lookupObject("doCompression"));
        assertEquals("this was set from the manager properties!", managementContext.getRegistry().lookupObject("beanProperty1"));
        assertNotNull(managementContext.getRegistry().lookupObject("OS_Version"));
    }


    public void testNestedRouterProxyCreation() throws ObjectNotFoundException
    {
        //Test that the proxy object was created and set on the service object
        UMODescriptor orange = managementContext.getRegistry().lookupService("orangeComponent");
        assertNotNull(orange);
        UMONestedRouter r = (UMONestedRouter) orange.getNestedRouter().getRouters().get(0);
        assertNotNull(r);

        //TODO Grab an instance of the service object itself and test that the proxy has been injected
    }
}
