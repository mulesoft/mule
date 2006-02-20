/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;
import org.mule.config.pool.CommonsPoolFactory;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.interceptors.LoggingInterceptor;
import org.mule.interceptors.TimerInterceptor;
import org.mule.providers.AbstractConnector;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.providers.service.ConnectorFactory;
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
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.model.UMOModel;
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOInboundRouter;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ObjectPool;

import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractConfigBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{
    public void testManagerConfig() throws Exception
    {
        assertNotNull(MuleManager.getInstance().getTransactionManager());
    }

    public void testConnectorConfig() throws Exception
    {
        TestConnector c = (TestConnector) MuleManager.getInstance().lookupConnector("dummyConnector");
        assertNotNull(c);
        assertNotNull(c.getExceptionListener());
        assertTrue(c.getExceptionListener() instanceof TestExceptionStrategy);
        assertNotNull(c.getConnectionStrategy());
        assertTrue(c.getConnectionStrategy() instanceof SimpleRetryConnectionStrategy);
        assertEquals(4, ((SimpleRetryConnectionStrategy) c.getConnectionStrategy()).getRetryCount());
        assertEquals(3000, ((SimpleRetryConnectionStrategy) c.getConnectionStrategy()).getFrequency());
    }

    public void testGlobalEndpointConfig()
    {
        UMOEndpoint endpoint = MuleManager.getInstance().lookupEndpoint("fruitBowlEndpoint");
        assertNotNull(endpoint);
        assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");
        assertNotNull(endpoint.getFilter());
        JXPathFilter filter = (JXPathFilter)endpoint.getFilter();
        assertEquals("name", filter.getExpression());
        assertEquals("bar", filter.getValue());
        assertEquals("http://foo.com", filter.getNamespaces().get("foo"));
    }

    public void testEndpointConfig()
    {
        String endpointString = MuleManager.getInstance().lookupEndpointIdentifier("Test Queue", null);
        assertEquals(endpointString, "test://test.queue");
        // test that endpoints have been resolved on endpoints
        UMOEndpoint endpoint = MuleManager.getInstance().lookupEndpoint("waterMelonEndpoint");
        assertNotNull(endpoint);
        assertEquals("test.queue", endpoint.getEndpointURI().getAddress());

        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("appleComponent2");
        assertNotNull(descriptor);
    }

    public void testInterceptorStacks()
    {
        UMOInterceptorStack stack = MuleManager.getInstance().lookupInterceptorStack("default");
		assertNotNull(stack);
        assertEquals(2, stack.getInterceptors().size());
        assertTrue(stack.getInterceptors().get(0) instanceof LoggingInterceptor);
        assertTrue(stack.getInterceptors().get(1) instanceof TimerInterceptor);
    }

    public void testExceptionStrategy2()
    {
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("appleComponent");
        assertNotNull(descriptor.getExceptionListener());
        assertEquals(DefaultExceptionStrategy.class, descriptor.getExceptionListener().getClass());

    }

    public void testTransformerConfig()
    {
        UMOTransformer t = MuleManager.getInstance().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertEquals(t.getReturnClass(), java.lang.String.class);
        assertNotNull(((TestCompressionTransformer) t).getContainerProperty());
    }

    public void testModelConfig() throws Exception
    {
        UMOModel model = MuleManager.getInstance().getModel();
        super.testModelConfig();
        assertTrue(model.isComponentRegistered("appleComponent"));
        assertTrue(model.isComponentRegistered("appleComponent2"));
    }


    public void testOutboundRouterConfig2()
    {
        // test outbound message router
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("appleComponent");
        assertNotNull(descriptor.getOutboundRouter());
        UMOOutboundMessageRouter router = descriptor.getOutboundRouter();
        assertNotNull(router.getCatchAllStrategy());
        assertEquals(2, router.getRouters().size());
        // check first Router
        UMOOutboundRouter route1 = (UMOOutboundRouter) router.getRouters().get(0);
        assertTrue(route1 instanceof FilteringOutboundRouter);
        //todo don't currently support "transformer" property
        //assertNotNull(((FilteringOutboundRouter) route1).getTransformer());

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

//    public void testDescriptorEndpoints()
//    {
//        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("orangeComponent");
//        assertEquals(1, descriptor.getOutboundRouter().getRouters().size());
//        UMOOutboundRouter router = (UMOOutboundRouter) descriptor.getOutboundRouter().getRouters().get(0);
//        assertEquals(1, router.getEndpoints().size());
//        UMOEndpoint endpoint = (UMOEndpoint) router.getEndpoints().get(0);
//        assertNotNull(endpoint);
//        assertEquals("appleInEndpoint", endpoint.getName());
//        assertNotNull(endpoint.getTransformer());
//        assertTrue(endpoint.getTransformer() instanceof TestCompressionTransformer);
//
//        // check the global endpoint
//        endpoint = MuleManager.getInstance().lookupEndpoint("appleInEndpoint");
//        assertNotNull(endpoint);
//        assertNull(endpoint.getTransformer());
//
//        endpoint = descriptor.getInboundEndpoint();
//        assertEquals(2, descriptor.getInboundRouter().getEndpoints().size());
//        assertNotNull(descriptor.getInboundRouter().getCatchAllStrategy());
//        assertTrue(descriptor.getInboundRouter().getCatchAllStrategy() instanceof ForwardingCatchAllStrategy);
//        assertNotNull(descriptor.getInboundRouter().getCatchAllStrategy().getEndpoint());
//        assertEquals("test://catch.all", descriptor.getInboundRouter()
//                                                   .getCatchAllStrategy()
//                                                   .getEndpoint()
//                                                   .getEndpointURI()
//                                                   .toString());
//        endpoint = descriptor.getInboundRouter().getEndpoint("orangeEndpoint");
//        assertNotNull(endpoint);
//        assertEquals("orangeEndpoint", endpoint.getName());
//        assertEquals("orangeQ", endpoint.getEndpointURI().getAddress());
//        // assertEquals(UMOEndpoint.ENDPOINT_TYPE_RECEIVER, endpoint.getType());
//        assertNotNull(endpoint.getTransformer());
//        assertTrue(endpoint.getTransformer() instanceof TestCompressionTransformer);
//
//        // check the global endpoint
//        endpoint = MuleManager.getInstance().lookupEndpoint("orangeEndpoint");
//        assertNotNull(endpoint);
//        assertNull(endpoint.getTransformer());
//    }

    public void testInboundRouterConfig2()
    {
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("appleComponent");
        assertNotNull(descriptor.getInboundRouter());
        UMOInboundMessageRouter messageRouter = descriptor.getInboundRouter();
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
        // test config
        ThreadingProfile tp = MuleManager.getConfiguration().getDefaultThreadingProfile();
        assertEquals(0, tp.getMaxBufferSize());
        assertEquals(8, tp.getMaxThreadsActive());
        assertEquals(4, tp.getMaxThreadsIdle());
        assertEquals(0, tp.getPoolExhaustedAction());
        assertEquals(60001, tp.getThreadTTL());

        // test defaults
        tp = MuleManager.getConfiguration().getComponentThreadingProfile();
        assertEquals(0, tp.getMaxBufferSize());
        assertEquals(8, tp.getMaxThreadsActive());
        assertEquals(4, tp.getMaxThreadsIdle());
        assertEquals(0, tp.getPoolExhaustedAction());
        assertEquals(60001, tp.getThreadTTL());

        // test thatvalues not set retain a default value
        AbstractConnector c = (AbstractConnector) MuleManager.getInstance().lookupConnector("dummyConnector");
        tp = c.getDispatcherThreadingProfile();
        assertEquals(2, tp.getMaxBufferSize());
        assertEquals(10, tp.getMaxThreadsActive());
        assertEquals(10, tp.getMaxThreadsIdle());
        assertEquals(4, tp.getPoolExhaustedAction());
        assertEquals(60000, tp.getThreadTTL());

        MuleDescriptor descriptor = (MuleDescriptor) MuleManager.getInstance()
                                                                .getModel()
                                                                .getDescriptor("appleComponent2");
        tp = descriptor.getThreadingProfile();
        assertEquals(6, tp.getMaxBufferSize());
        assertEquals(12, tp.getMaxThreadsActive());
        assertEquals(6, tp.getMaxThreadsIdle());
        assertEquals(4, tp.getPoolExhaustedAction());
        assertEquals(60000, tp.getThreadTTL());
    }

    public void testPoolingConfig()
    {
        // test config
        PoolingProfile pp = MuleManager.getConfiguration().getPoolingProfile();
        assertEquals(8, pp.getMaxActive());
        assertEquals(4, pp.getMaxIdle());
        assertEquals(4000, pp.getMaxWait());
        assertEquals(ObjectPool.WHEN_EXHAUSTED_GROW, pp.getExhaustedAction());
        assertEquals(1, pp.getInitialisationPolicy());
        assertTrue(pp.getPoolFactory() instanceof CommonsPoolFactory);

        // test override
        MuleDescriptor descriptor = (MuleDescriptor) MuleManager.getInstance()
                                                                .getModel()
                                                                .getDescriptor("appleComponent2");
        pp = descriptor.getPoolingProfile();

        assertEquals(5, pp.getMaxActive());
        assertEquals(5, pp.getMaxIdle());
        assertEquals(4000, pp.getMaxWait());
        assertEquals(ObjectPool.WHEN_EXHAUSTED_GROW, pp.getExhaustedAction());
        assertEquals(2, pp.getInitialisationPolicy());
    }

    public void testQueueProfileConfig()
    {
        // test config
        QueueProfile qp = MuleManager.getConfiguration().getQueueProfile();
        assertEquals(100, qp.getMaxOutstandingMessages());
        assertTrue(qp.isPersistent());

        // test inherit
        MuleDescriptor descriptor = (MuleDescriptor) MuleManager.getInstance()
                                                                .getModel()
                                                                .getDescriptor("orangeComponent");
        qp = descriptor.getQueueProfile();
        assertEquals(100, qp.getMaxOutstandingMessages());
        assertTrue(qp.isPersistent());

        // test override
        descriptor = (MuleDescriptor) MuleManager.getInstance().getModel().getDescriptor("appleComponent2");
        qp = descriptor.getQueueProfile();
        assertEquals(102, qp.getMaxOutstandingMessages());
        assertFalse(qp.isPersistent());
    }

    public void testEndpointProperties() throws Exception
    {
        // test transaction config
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("appleComponent2");
        MuleEndpoint inEndpoint = (MuleEndpoint) descriptor.getInboundRouter().getEndpoint("transactedInboundEndpoint");
        assertNotNull(inEndpoint);
        assertEquals(ConnectorFactory.ALWAYS_CREATE_CONNECTOR, inEndpoint.getCreateConnector());
        assertNotNull(inEndpoint.getProperties());
        assertEquals("Prop1", inEndpoint.getProperties().get("testEndpointProperty"));
    }

    public void testTranactionConfig() throws Exception
    {
        // test transaction config
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("appleComponent2");
        UMOEndpoint inEndpoint = descriptor.getInboundRouter().getEndpoint("transactedInboundEndpoint");
        assertNotNull(inEndpoint);
        assertNull(descriptor.getOutboundEndpoint());
        assertEquals(1, descriptor.getOutboundRouter().getRouters().size());

        UMOEndpoint outEndpoint = (UMOEndpoint) ((UMOOutboundRouter) descriptor.getOutboundRouter().getRouters().get(0)).getEndpoints()
                                                                                                                        .get(0);

        assertNotNull(outEndpoint);
        assertNotNull(inEndpoint.getTransactionConfig());
        assertEquals(UMOTransactionConfig.ACTION_ALWAYS_BEGIN, inEndpoint.getTransactionConfig().getAction());
        assertTrue(inEndpoint.getTransactionConfig().getFactory() instanceof TestTransactionFactory);
        assertNull(inEndpoint.getTransactionConfig().getConstraint());
    }

    public void testEnvironmentProperties()
    {
        Map props = MuleManager.getInstance().getProperties();
        assertNotNull(props);
        assertNotNull(props.get("doCompression"));
        assertEquals("true", props.get("doCompression"));
        assertNotNull(props.get("beanProperty1"));
        assertEquals("this was set from the manager properties!", props.get("beanProperty1"));
        assertNotNull(props.get("OS Version"));
    }
}
