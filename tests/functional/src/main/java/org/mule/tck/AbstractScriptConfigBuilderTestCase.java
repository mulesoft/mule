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

import org.mule.impl.AbstractExceptionListener;
import org.mule.management.agents.JmxAgent;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestDefaultLifecycleAdapterFactory;
import org.mule.tck.testmodels.mule.TestEntryPointResolver;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestResponseAggregator;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.model.UMOModel;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouter;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.routing.UMOResponseRouter;
import org.mule.umo.routing.UMOResponseRouterCollection;
import org.mule.umo.transformer.UMOTransformer;

import java.util.List;
import java.util.Map;

public abstract class AbstractScriptConfigBuilderTestCase extends FunctionalTestCase
{

    protected AbstractScriptConfigBuilderTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    public void testManagerConfig() throws Exception
    {
        assertEquals("true", managementContext.getRegistry().getProperty("doCompression"));
        assertNotNull(managementContext.getTransactionManager());
    }


    public void testConnectorConfig() throws Exception
    {
        TestConnector c = (TestConnector)managementContext.getRegistry().lookupConnector("dummyConnector");
        assertNotNull(c);
        assertNotNull(c.getExceptionListener());
        assertTrue(c.getExceptionListener() instanceof TestExceptionStrategy);
    }

    public void testGlobalEndpointConfig()
    {
        UMOEndpoint endpoint = managementContext.getRegistry().lookupEndpoint("fruitBowlEndpoint");
        assertNotNull(endpoint);
        assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");
        assertNotNull(endpoint.getFilter());
        JXPathFilter filter = (JXPathFilter)endpoint.getFilter();
        assertEquals("name", filter.getExpression());
        assertEquals("bar", filter.getExpectedValue());
        assertEquals("http://foo.com", filter.getNamespaces().get("foo"));

        UMOEndpoint ep = managementContext.getRegistry().lookupEndpoint("testEPWithCS");
        assertNotNull(ep);
        assertNotNull(ep.getConnectionStrategy());
        assertTrue(ep.getConnectionStrategy() instanceof SimpleRetryConnectionStrategy);
        assertEquals(4, ((SimpleRetryConnectionStrategy)ep.getConnectionStrategy()).getRetryCount());
        assertEquals(3000, ((SimpleRetryConnectionStrategy)ep.getConnectionStrategy()).getFrequency());
    }

    public void testEndpointConfig()
    {
        // test that endpoints have been resolved on endpoints
        UMOEndpoint endpoint = managementContext.getRegistry().lookupEndpoint("waterMelonEndpoint");
        assertNotNull(endpoint);
        assertEquals("test.queue", endpoint.getEndpointURI().getAddress());

        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("orangeComponent");
        UMOEndpoint ep = descriptor.getInboundRouter().getEndpoint("Orange");
        assertNotNull(ep);
        assertNotNull(ep.getResponseTransformer());
        assertTrue(ep.getResponseTransformer() instanceof TestCompressionTransformer);
    }

    public void testExceptionStrategy()
    {
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("orangeComponent");
        assertNotNull(managementContext.getRegistry().lookupModel("main").getExceptionListener());
        assertNotNull(descriptor.getExceptionListener());

        assertTrue(((AbstractExceptionListener)descriptor.getExceptionListener()).getEndpoints().size() > 0);
        UMOEndpoint ep = (UMOEndpoint)((AbstractExceptionListener)descriptor.getExceptionListener()).getEndpoints()
            .get(0);

        assertEquals("test://orange.exceptions", ep.getEndpointURI().toString());
    }

    public void testTransformerConfig()
    {
        UMOTransformer t = managementContext.getRegistry().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertEquals(t.getReturnClass(), String.class);
        assertNotNull(((TestCompressionTransformer)t).getContainerProperty());
    }

    public void testModelConfig() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        assertNotNull(model);
        assertEquals("main", model.getName());
        assertTrue(model.getEntryPointResolver() instanceof TestEntryPointResolver);
        assertTrue(model.getExceptionListener() instanceof TestExceptionStrategy);

        assertTrue(((AbstractExceptionListener)model.getExceptionListener()).getEndpoints().size() > 0);
        UMOEndpoint ep = (UMOEndpoint)((AbstractExceptionListener)model.getExceptionListener()).getEndpoints()
            .get(0);

        assertEquals("test://component.exceptions", ep.getEndpointURI().toString());

        assertTrue(model.getLifecycleAdapterFactory() instanceof TestDefaultLifecycleAdapterFactory);

        assertTrue(model.isComponentRegistered("orangeComponent"));
    }

    public void testPropertiesConfig() throws Exception
    {
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("orangeComponent");

        Map props = descriptor.getProperties();
        assertNotNull(props);
        assertEquals("9", props.get("segments"));
        assertEquals("4.21", props.get("radius"));
        assertEquals("Juicy Baby!", props.get("brand"));

        assertNotNull(props.get("listProperties"));
        List list = (List)props.get("listProperties");
        assertEquals(3, list.size());
        assertEquals("prop1", list.get(0));
        assertEquals("prop2", list.get(1));
        assertEquals("prop3", list.get(2));

        assertNotNull(props.get("arrayProperties"));
        list = (List)props.get("arrayProperties");
        assertEquals(3, list.size());
        assertEquals("prop4", list.get(0));
        assertEquals("prop5", list.get(1));
        assertEquals("prop6", list.get(2));

        assertNotNull(props.get("mapProperties"));
        props = (Map)props.get("mapProperties");
        assertEquals("prop1", props.get("prop1"));
        assertEquals("prop2", props.get("prop2"));

        assertEquals(6, descriptor.getProperties().size());
    }

    public void testOutboundRouterConfig()
    {
        // test outbound message router
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("orangeComponent");
        assertNotNull(descriptor.getOutboundRouter());
        UMOOutboundRouterCollection router = descriptor.getOutboundRouter();
        assertNull(router.getCatchAllStrategy());
        assertEquals(1, router.getRouters().size());
        // check first Router
        UMOOutboundRouter route1 = (UMOOutboundRouter)router.getRouters().get(0);
        assertTrue(route1 instanceof OutboundPassThroughRouter);
        assertEquals(1, route1.getEndpoints().size());
    }

    public void testNestedRouterConfig() throws ObjectNotFoundException
    {
        // test outbound message router
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("orangeComponent");
        assertNotNull(descriptor.getNestedRouter());
        UMONestedRouterCollection router = descriptor.getNestedRouter();
        assertEquals(2, router.getRouters().size());
        // check first Router
        UMONestedRouter route1 = (UMONestedRouter)router.getRouters().get(0);
        assertEquals(FruitCleaner.class, route1.getInterface());
        assertEquals("wash", route1.getMethod());
        assertNotNull(route1.getOutboundRouter());
        assertEquals(1, route1.getOutboundRouter().getEndpoints().size());
        // check second Router
        UMONestedRouter route2 = (UMONestedRouter)router.getRouters().get(1);
        assertEquals(FruitCleaner.class, route2.getInterface());
        assertEquals("polish", route2.getMethod());
        assertNotNull(route2.getOutboundRouter());        
        assertEquals(1, route2.getOutboundRouter().getEndpoints().size());

    }
    
    public void testDescriptorEndpoints()
    {
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("orangeComponent");
        assertEquals(1, descriptor.getOutboundRouter().getRouters().size());
        UMOOutboundRouter router = (UMOOutboundRouter)descriptor.getOutboundRouter().getRouters().get(0);
        assertEquals(1, router.getEndpoints().size());
        UMOEndpoint endpoint = (UMOEndpoint)router.getEndpoints().get(0);
        assertNotNull(endpoint);
        assertEquals("appleInEndpoint", endpoint.getName());
        assertNotNull(endpoint.getTransformer());
        assertTrue(endpoint.getTransformer() instanceof TestCompressionTransformer);

        // check the global endpoint
        endpoint = managementContext.getRegistry().lookupEndpoint("appleInEndpoint");
        assertNotNull(endpoint);
        assertNull(endpoint.getTransformer());

        assertEquals(2, descriptor.getInboundRouter().getEndpoints().size());
        assertNotNull(descriptor.getInboundRouter().getCatchAllStrategy());
        assertTrue(descriptor.getInboundRouter().getCatchAllStrategy() instanceof ForwardingCatchAllStrategy);
        assertNotNull(descriptor.getInboundRouter().getCatchAllStrategy().getEndpoint());
        assertEquals("test2://catch.all", descriptor.getInboundRouter()
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
        endpoint = managementContext.getRegistry().lookupEndpoint("orangeEndpoint");
        assertNotNull(endpoint);
        assertNull(endpoint.getTransformer());
    }

    public void testInboundRouterConfig()
    {
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("orangeComponent");
        assertNotNull(descriptor.getInboundRouter());
        UMOInboundRouterCollection messageRouter = descriptor.getInboundRouter();
        assertNotNull(messageRouter.getCatchAllStrategy());
        assertEquals(0, messageRouter.getRouters().size());
        assertTrue(messageRouter.getCatchAllStrategy() instanceof ForwardingCatchAllStrategy);
        assertEquals(2, messageRouter.getEndpoints().size());
    }

    public void testResponseRouterConfig()
    {
        UMODescriptor descriptor = managementContext.getRegistry().lookupModel("main").getDescriptor("orangeComponent");
        assertNotNull(descriptor.getResponseRouter());
        UMOResponseRouterCollection messageRouter = descriptor.getResponseRouter();
        assertNull(messageRouter.getCatchAllStrategy());
        assertEquals(10001, messageRouter.getTimeout());
        assertEquals(1, messageRouter.getRouters().size());
        UMOResponseRouter router = (UMOResponseRouter)messageRouter.getRouters().get(0);
        assertTrue(router instanceof TestResponseAggregator);
        assertNotNull(messageRouter.getEndpoints());
        assertEquals(2, messageRouter.getEndpoints().size());
        UMOEndpoint ep = (UMOEndpoint)messageRouter.getEndpoints().get(0);
        assertEquals("response1", ep.getEndpointURI().getAddress());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
        ep = (UMOEndpoint)messageRouter.getEndpoints().get(1);
        assertEquals("AppleResponseQueue", ep.getEndpointURI().getAddress());
        assertEquals(UMOEndpoint.ENDPOINT_TYPE_RESPONSE, ep.getType());
    }

    public void testAgentConfiguration() throws UMOException
    {
        JmxAgent agent = (JmxAgent)managementContext.getRegistry().lookupAgent("jmxAgent");
        assertNotNull(agent);
        //TODO RM* Add this back in. Currently failing because of a JMX issue where the AllStatistics MBean is registered twice
//        assertNotNull(agent.getConnectorServerUrl());
//        assertEquals("service:jmx:rmi:///jndi/rmi://localhost:1099/server", agent.getConnectorServerUrl());
//        assertNotNull(agent.getConnectorServerProperties());
//        assertEquals("true", agent.getConnectorServerProperties().get("jmx.remote.jndi.rebind"));
    }
}
