/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.MuleException;
import org.mule.RegistryContext;
import org.mule.config.ThreadingProfile;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.container.ContainerKeyPair;
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
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestCatchAllStrategy;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.model.UMOModel;
import org.mule.umo.routing.UMOInboundRouter;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;

public abstract class AbstractConfigBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{

    // @Override
    public void testManagerConfig() throws Exception
    {
        assertNotNull(managementContext.getTransactionManager());
    }

    // @Override
    public void testConnectorConfig() throws Exception
    {
        TestConnector c = (TestConnector)managementContext.getRegistry().lookupConnector("dummyConnector");
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
        UMOEndpoint endpoint = managementContext.getRegistry().lookupEndpoint("fruitBowlEndpoint");
        assertNotNull(endpoint);
        assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");
        assertNotNull(endpoint.getFilter());
        JXPathFilter filter = (JXPathFilter)endpoint.getFilter();
        assertEquals("name", filter.getExpression());
        assertEquals("bar", filter.getExpectedValue());
        assertNotNull(filter.getNamespaces());
        assertEquals("http://foo.com", filter.getNamespaces().get("foo"));
    }

    // @Override
    public void testEndpointConfig()
    {
        // test that endpoints have been resolved on endpoints
        UMOEndpoint endpoint = managementContext.getRegistry().lookupEndpoint("waterMelonEndpoint");
        assertNotNull(endpoint);
        assertEquals("test.queue", endpoint.getEndpointURI().getAddress());

        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("appleComponent2");
        assertNotNull(descriptor);
    }

    public void testExceptionStrategy2()
    {
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("appleComponent");
        assertNotNull(descriptor.getExceptionListener());
        assertEquals(DefaultExceptionStrategy.class, descriptor.getExceptionListener().getClass());
    }

    // @Override
    public void testTransformerConfig()
    {
        UMOTransformer t = managementContext.getRegistry().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertEquals(t.getReturnClass(), java.lang.String.class);
        assertNotNull(((TestCompressionTransformer)t).getContainerProperty());
    }

    // @Override
    public void testModelConfig() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        super.testModelConfig();
        assertTrue(model.isComponentRegistered("appleComponent"));
        assertTrue(model.isComponentRegistered("appleComponent2"));
    }

    public void testOutboundRouterConfig2()
    {
        // test outbound message router
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("appleComponent");
        assertNotNull(descriptor.getOutboundRouter());
        UMOOutboundRouterCollection router = descriptor.getOutboundRouter();
        assertNotNull(router.getCatchAllStrategy());
        assertEquals(2, router.getRouters().size());
        // check first Router
        UMOOutboundRouter route1 = (UMOOutboundRouter)router.getRouters().get(0);
        assertTrue(route1 instanceof FilteringOutboundRouter);
        assertNotNull(((FilteringOutboundRouter) route1).getTransformer());
        assertTrue(((FilteringOutboundRouter) route1).getTransformer() instanceof TestCompressionTransformer);

        UMOFilter filter = ((FilteringOutboundRouter)route1).getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof PayloadTypeFilter);
        assertEquals(String.class, ((PayloadTypeFilter)filter).getExpectedType());

        // check second Router
        UMOOutboundRouter route2 = (UMOOutboundRouter)router.getRouters().get(1);
        assertTrue(route2 instanceof FilteringOutboundRouter);

        UMOFilter filter2 = ((FilteringOutboundRouter)route2).getFilter();
        assertNotNull(filter2);
        assertTrue(filter2 instanceof AndFilter);
        UMOFilter left = ((AndFilter)filter2).getLeftFilter();
        UMOFilter right = ((AndFilter)filter2).getRightFilter();
        assertNotNull(left);
        assertTrue(left instanceof RegExFilter);
        assertEquals("the quick brown (.*)", ((RegExFilter)left).getExpression());
        assertNotNull(right);
        assertTrue(right instanceof RegExFilter);
        assertEquals("(.*) brown (.*)", ((RegExFilter)right).getExpression());

        assertTrue(router.getCatchAllStrategy() instanceof TestCatchAllStrategy);
    }


    public void testInboundRouterConfig2()
    {
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("appleComponent");
        assertNotNull(descriptor.getInboundRouter());
        UMOInboundRouterCollection messageRouter = descriptor.getInboundRouter();
        assertNotNull(messageRouter.getCatchAllStrategy());
        assertEquals(2, messageRouter.getRouters().size());
        UMOInboundRouter router = (UMOInboundRouter)messageRouter.getRouters().get(0);
        assertTrue(router instanceof SelectiveConsumer);
        SelectiveConsumer sc = (SelectiveConsumer)router;

        assertNotNull(sc.getFilter());
        UMOFilter filter = sc.getFilter();
        // check first Router
        assertTrue(filter instanceof PayloadTypeFilter);
        assertEquals(String.class, ((PayloadTypeFilter)filter).getExpectedType());

        UMOInboundRouter router2 = (UMOInboundRouter)messageRouter.getRouters().get(1);
        assertTrue(router2 instanceof IdempotentReceiver);
    }

    public void testThreadingConfig() throws MuleException
    {
        // test config
        ThreadingProfile tp = RegistryContext.getConfiguration().getDefaultThreadingProfile();
        assertEquals(ThreadingProfile.DEFAULT_MAX_BUFFER_SIZE, tp.getMaxBufferSize());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_ACTIVE, tp.getMaxThreadsActive());
        assertEquals(4, tp.getMaxThreadsIdle());
        assertEquals(ThreadingProfile.WHEN_EXHAUSTED_WAIT, tp.getPoolExhaustedAction());
        assertEquals(60001, tp.getThreadTTL());

        // test defaults
        tp = RegistryContext.getConfiguration().getDefaultComponentThreadingProfile();
        assertEquals(ThreadingProfile.DEFAULT_MAX_BUFFER_SIZE, tp.getMaxBufferSize());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_ACTIVE, tp.getMaxThreadsActive());
        assertEquals(4, tp.getMaxThreadsIdle());
        assertEquals(ThreadingProfile.WHEN_EXHAUSTED_WAIT, tp.getPoolExhaustedAction());
        assertEquals(60001, tp.getThreadTTL());

        // test that values not set retain a default value
        AbstractConnector c = (AbstractConnector)managementContext.getRegistry().lookupConnector("dummyConnector");
        tp = c.getDispatcherThreadingProfile();
        assertEquals(2, tp.getMaxBufferSize());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_ACTIVE, tp.getMaxThreadsActive());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_IDLE, tp.getMaxThreadsIdle());
        assertEquals(ThreadingProfile.DEFAULT_POOL_EXHAUST_ACTION, tp.getPoolExhaustedAction());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREAD_TTL, tp.getThreadTTL());

        MuleDescriptor descriptor = (MuleDescriptor)managementContext.getRegistry().lookupModel("main").getDescriptor(
            "appleComponent2");
        tp = descriptor.getThreadingProfile();
        assertEquals(6, tp.getMaxBufferSize());
        assertEquals(12, tp.getMaxThreadsActive());
        assertEquals(6, tp.getMaxThreadsIdle());
        assertEquals(ThreadingProfile.DEFAULT_POOL_EXHAUST_ACTION, tp.getPoolExhaustedAction());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREAD_TTL, tp.getThreadTTL());
    }

    public void testPoolingConfig()
    {
        //TODO RM* test config
//        PoolingProfile pp = RegistryContext.getConfiguration().getPoolingProfile();
//        assertEquals(8, pp.getMaxActive());
//        assertEquals(4, pp.getMaxIdle());
//        assertEquals(4000, pp.getMaxWait());
//        assertEquals(ObjectPool.WHEN_EXHAUSTED_GROW, pp.getExhaustedAction());
//        assertEquals(1, pp.getInitialisationPolicy());
//        assertTrue(pp.getPoolFactory() instanceof CommonsPoolFactory);

        // test override
        MuleDescriptor descriptor = (MuleDescriptor)managementContext.getRegistry().lookupModel("main").getDescriptor(
            "appleComponent2");
//        PoolingProfile pp = descriptor.getPoolingProfile();
//
//        assertEquals(5, pp.getMaxActive());
//        assertEquals(5, pp.getMaxIdle());
//        assertEquals(4000, pp.getMaxWait());
//        assertEquals(ObjectPool.WHEN_EXHAUSTED_GROW, pp.getExhaustedAction());
//        assertEquals(2, pp.getInitialisationPolicy());
    }

    public void testQueueProfileConfig()
    {
        // test config
        //TODO RM*
//        QueueProfile qp = RegistryContext.getConfiguration().getQueueProfile();
//        assertEquals(100, qp.getMaxOutstandingMessages());
//        assertTrue(qp.isPersistent());

        // test inherit
        MuleDescriptor descriptor = (MuleDescriptor)managementContext.getRegistry().lookupModel("main").getDescriptor(
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
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("appleComponent2");
        MuleEndpoint inEndpoint = (MuleEndpoint)descriptor.getInboundRouter().getEndpoint(
            "transactedInboundEndpoint");
        assertNotNull(inEndpoint);
        assertEquals(TransportFactory.NEVER_CREATE_CONNECTOR, inEndpoint.getCreateConnector());
        assertNotNull(inEndpoint.getProperties());
        assertEquals("Prop1", inEndpoint.getProperties().get("testEndpointProperty"));
    }

    public void testTransactionConfig() throws Exception
    {
        // test transaction config
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("appleComponent2");
        UMOEndpoint inEndpoint = descriptor.getInboundRouter().getEndpoint("transactedInboundEndpoint");
        assertNotNull(inEndpoint);
        assertEquals(1, descriptor.getOutboundRouter().getRouters().size());

        UMOEndpoint outEndpoint = (UMOEndpoint)((UMOOutboundRouter)descriptor.getOutboundRouter()
            .getRouters()
            .get(0)).getEndpoints().get(0);

        assertNotNull(outEndpoint);
        assertNotNull(inEndpoint.getTransactionConfig());
        assertEquals(UMOTransactionConfig.ACTION_ALWAYS_BEGIN, inEndpoint.getTransactionConfig().getAction());
        assertTrue(inEndpoint.getTransactionConfig().getFactory() instanceof TestTransactionFactory);
        assertNull(inEndpoint.getTransactionConfig().getConstraint());
    }

    public void testEnvironmentProperties()
    {
        Map props = managementContext.getRegistry().getProperties();
        assertNotNull(props);
        assertNotNull(props.get("doCompression"));
        assertEquals("true", props.get("doCompression"));
        assertNotNull(props.get("beanProperty1"));
        assertEquals("this was set from the manager properties!", props.get("beanProperty1"));
        assertNotNull(props.get("OS Version"));
    }

    public void testObjectReferences() throws UMOException
    {
        MuleDescriptor descriptor = (MuleDescriptor)managementContext.getRegistry().lookupModel("main").getDescriptor(
            "orangeComponent");
        assertEquals(new ContainerKeyPair(null, "orange"), descriptor.getImplementation());
        assertEquals(Orange.class, descriptor.getImplementationClass());
    }

    public void testNestedRouterProxyCreation() throws ObjectNotFoundException
    {
        //Test that the proxy object was created and set on the service object
        Orange orange = (Orange)managementContext.getRegistry().getContainerContext().getComponent("orange");
        assertNotNull(orange);
        assertNotNull(orange.getCleaner());
    }
}
