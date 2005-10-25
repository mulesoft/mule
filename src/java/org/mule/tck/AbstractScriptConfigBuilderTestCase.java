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

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.MuleProperties;
import org.mule.impl.AbstractExceptionListener;
import org.mule.impl.MuleDescriptor;
import org.mule.interceptors.LoggingInterceptor;
import org.mule.interceptors.TimerInterceptor;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestDefaultLifecycleAdapterFactory;
import org.mule.tck.testmodels.mule.TestEntryPointResolver;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestResponseAggregator;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.routing.UMOResponseRouter;
import org.mule.umo.transformer.UMOTransformer;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractScriptConfigBuilderTestCase extends NamedTestCase
{
    protected static boolean initialised = false;

    protected AbstractScriptConfigBuilderTestCase()
    {
        if (MuleManager.isInstanciated())
            MuleManager.getInstance().dispose();
        initialised = false;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        if (!initialised) {
            System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "true");

            if (MuleManager.isInstanciated())
                MuleManager.getInstance().dispose();
            ConfigurationBuilder configBuilder = getConfigBuilder();
            configBuilder.configure(getConfigResource());
            initialised = true;
            System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "false");

        }
    }

    public void testManagerConfig() throws Exception
    {
        assertEquals("true", MuleManager.getInstance().getProperty("doCompression"));
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

        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("orangeComponent");
        UMOEndpoint ep = descriptor.getInboundRouter().getEndpoint("Orange");
        assertNotNull(ep);
        assertNotNull(ep.getResponseTransformer());
        assertTrue(ep.getResponseTransformer() instanceof TestCompressionTransformer);
    }

    public void testInterceptorStacks()
    {
        UMOInterceptorStack stack = MuleManager.getInstance().lookupInterceptorStack("default");
		assertNotNull(stack);
        assertEquals(2, stack.getInterceptors().size());
        assertTrue(stack.getInterceptors().get(0) instanceof LoggingInterceptor);
        assertTrue(stack.getInterceptors().get(1) instanceof TimerInterceptor);
    }

    public void testExceptionStrategy()
    {
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("orangeComponent");
        assertNotNull(MuleManager.getInstance().getModel().getExceptionListener());
        assertNotNull(descriptor.getExceptionListener());

        assertTrue(((AbstractExceptionListener) descriptor.getExceptionListener()).getEndpoints().size() > 0);
        UMOEndpoint ep = (UMOEndpoint) ((AbstractExceptionListener) descriptor.getExceptionListener()).getEndpoints()
                                                                                                      .get(0);

        assertEquals("test://orange.exceptions", ep.getEndpointURI().toString());
    }

    public void testTransformerConfig()
    {
        UMOTransformer t = MuleManager.getInstance().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertEquals(t.getReturnClass(), String.class);
        assertNotNull(((TestCompressionTransformer) t).getContainerProperty());
    }

    public void testModelConfig() throws Exception
    {
        UMOModel model = MuleManager.getInstance().getModel();
        assertNotNull(model);
        assertEquals("test-model", model.getName());
        assertTrue(model.getEntryPointResolver() instanceof TestEntryPointResolver);
        assertTrue(model.getExceptionListener() instanceof TestExceptionStrategy);

        assertTrue(((AbstractExceptionListener) model.getExceptionListener()).getEndpoints().size() > 0);
        UMOEndpoint ep = (UMOEndpoint) ((AbstractExceptionListener) model.getExceptionListener()).getEndpoints().get(0);

        assertEquals("test://component.exceptions", ep.getEndpointURI().toString());

        assertTrue(model.getLifecycleAdapterFactory() instanceof TestDefaultLifecycleAdapterFactory);

        assertTrue(model.isComponentRegistered("orangeComponent"));
    }

    public void testPropertiesConfig() throws Exception
    {
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("orangeComponent");

        Map props = descriptor.getProperties();
        assertNotNull(props);
        assertEquals("9", props.get("segments"));
        assertEquals("4.21", props.get("radius"));
        assertEquals("Juicy Baby!", props.get("brand"));

        assertNotNull(props.get("listProperties"));
        List list = (List) props.get("listProperties");
        assertEquals(3, list.size());
        assertEquals("prop1", list.get(0));
        assertEquals("prop2", list.get(1));
        assertEquals("prop3", list.get(2));

        assertNotNull(props.get("arrayProperties"));
        list = (List) props.get("arrayProperties");
        assertEquals(3, list.size());
        assertEquals("prop4", list.get(0));
        assertEquals("prop5", list.get(1));
        assertEquals("prop6", list.get(2));

        assertNotNull(props.get("mapProperties"));
        props = (Map) props.get("mapProperties");
        assertEquals("prop1", props.get("prop1"));
        assertEquals("prop2", props.get("prop2"));

        assertEquals(7, descriptor.getProperties().size());
        assertEquals(2, descriptor.getInboundRouter().getEndpoints().size());
        // assertNotNull(descriptor.getInboundEndpoint());
        // assertNotNull(descriptor.getOutboundEndpoint());
    }

    public void testOutboundRouterConfig()
    {
        // test outbound message router
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("orangeComponent");
        assertNotNull(descriptor.getOutboundRouter());
        UMOOutboundMessageRouter router = descriptor.getOutboundRouter();
        assertNull(router.getCatchAllStrategy());
        assertEquals(1, router.getRouters().size());
        // check first Router
        UMOOutboundRouter route1 = (UMOOutboundRouter) router.getRouters().get(0);
        assertTrue(route1 instanceof OutboundPassThroughRouter);
        assertEquals(1, route1.getEndpoints().size());
    }

    public void testDescriptorEndpoints()
    {
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("orangeComponent");
        assertEquals(1, descriptor.getOutboundRouter().getRouters().size());
        UMOOutboundRouter router = (UMOOutboundRouter) descriptor.getOutboundRouter().getRouters().get(0);
        assertEquals(1, router.getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint) router.getEndpoints().get(0);
        assertNotNull(endpoint);
        assertEquals("appleInEndpoint", endpoint.getName());
        assertNotNull(endpoint.getTransformer());
        assertTrue(endpoint.getTransformer() instanceof TestCompressionTransformer);

        // check the global endpoint
        endpoint = MuleManager.getInstance().lookupEndpoint("appleInEndpoint");
        assertNotNull(endpoint);
        assertNull(endpoint.getTransformer());

        endpoint = descriptor.getInboundEndpoint();
        assertEquals(2, descriptor.getInboundRouter().getEndpoints().size());
        assertNotNull(descriptor.getInboundRouter().getCatchAllStrategy());
        assertTrue(descriptor.getInboundRouter().getCatchAllStrategy() instanceof ForwardingCatchAllStrategy);
        assertNotNull(descriptor.getInboundRouter().getCatchAllStrategy().getEndpoint());
        assertEquals("test://catch.all", descriptor.getInboundRouter()
                                                   .getCatchAllStrategy()
                                                   .getEndpoint()
                                                   .getEndpointURI()
                                                   .toString());
        endpoint = descriptor.getInboundRouter().getEndpoint("orangeEndpoint");
        assertNotNull(endpoint);
        assertEquals("orangeEndpoint", endpoint.getName());
        assertEquals("orangeQ", endpoint.getEndpointURI().getAddress());
        assertNotNull(endpoint.getTransformer());
        assertTrue(endpoint.getTransformer() instanceof TestCompressionTransformer);

        // check the global endpoint
        endpoint = MuleManager.getInstance().lookupEndpoint("orangeEndpoint");
        assertNotNull(endpoint);
        assertNull(endpoint.getTransformer());
    }

    public void testInboundRouterConfig()
    {
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("orangeComponent");
        assertNotNull(descriptor.getInboundRouter());
        UMOInboundMessageRouter messageRouter = descriptor.getInboundRouter();
        assertNotNull(messageRouter.getCatchAllStrategy());
        assertEquals(0, messageRouter.getRouters().size());
        assertTrue(messageRouter.getCatchAllStrategy() instanceof ForwardingCatchAllStrategy);
        assertEquals(2, messageRouter.getEndpoints().size());
    }

    public void testResponseRouterConfig()
    {
        UMODescriptor descriptor = MuleManager.getInstance().getModel().getDescriptor("orangeComponent");
        assertNotNull(descriptor.getResponseRouter());
        UMOResponseMessageRouter messageRouter = descriptor.getResponseRouter();
        assertNull(messageRouter.getCatchAllStrategy());
        assertEquals(10001, messageRouter.getTimeout());
        assertEquals(1, messageRouter.getRouters().size());
        UMOResponseRouter router = (UMOResponseRouter) messageRouter.getRouters().get(0);
        assertTrue(router instanceof TestResponseAggregator);
        assertNotNull(messageRouter.getEndpoints());
        assertEquals(2, messageRouter.getEndpoints().size());
        UMOEndpoint ep = (UMOEndpoint) messageRouter.getEndpoints().get(0);
        assertEquals("response1", ep.getEndpointURI().getAddress());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
        ep = (UMOEndpoint) messageRouter.getEndpoints().get(1);
        assertEquals("AppleResponseQueue", ep.getEndpointURI().getAddress());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
    }

    public void testObjectReferences() throws UMOException
    {
        MuleDescriptor descriptor = (MuleDescriptor) MuleManager.getInstance()
                                                                .getModel()
                                                                .getDescriptor("orangeComponent");
        assertEquals("local:orange", descriptor.getImplementation());
        assertNotNull(descriptor.getProperties().get("orange"));
        assertEquals(Orange.class, descriptor.getImplementationClass());
    }

    public void testAgentConfiguration() throws UMOException
    {
        UMOAgent agent = MuleManager.getInstance().unregisterAgent("jmxAgent");
        assertNotNull(agent);
    }

    // leave this last
    public void testTearDown() throws Exception
    {
        if (MuleManager.isInstanciated())
            MuleManager.getInstance().dispose();
        initialised = false;
    }

    public abstract String getConfigResource();

    public abstract ConfigurationBuilder getConfigBuilder();

}
