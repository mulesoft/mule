/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.builders;

import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.model.seda.SedaModel;
import org.mule.management.agents.JmxAgent;
import org.mule.management.agents.RmiRegistryAgent;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.nested.NestedRouter;
import org.mule.routing.nested.NestedRouterCollection;
import org.mule.routing.response.ResponseRouterCollection;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestDefaultLifecycleAdapterFactory;
import org.mule.tck.testmodels.mule.TestEntryPointResolver;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestResponseAggregator;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.model.UMOModel;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOResponseRouterCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptSource
{
    UMOManagementContext managementContext;
    QuickConfigurationBuilder builder;


    public ScriptSource() throws Exception
    {
        // need this when running with JMX
        managementContext.setId("GroovyScriptTestCase");

        //set global properties
        managementContext.getRegistry().registerProperty("doCompression", "true");
        //disable the admin agent
        //manager.getConfiguration().setServerUrl("");

        //Set a dummy TX manager
        managementContext.setTransactionManager(new TestTransactionManagerFactory().create());

        //register agents
        RmiRegistryAgent rmiAgent = new RmiRegistryAgent();
        rmiAgent.setName("rmiAgent");
        managementContext.getRegistry().registerAgent(rmiAgent);

        JmxAgent agent = new JmxAgent();
        agent.setName("jmxAgent");
        agent.setConnectorServerUrl("service:jmx:rmi:///jndi/rmi://localhost:1099/server");
        Map p = new HashMap();
        p.put("jmx.remote.jndi.rebind", "true");
        agent.setConnectorServerProperties(p);
        managementContext.getRegistry().registerAgent(agent);

        //register connector
        TestConnector c = new TestConnector();
        c.setName("dummyConnector");
        c.setExceptionListener(new TestExceptionStrategy());
        managementContext.getRegistry().registerConnector(c);

        //Register transformers
        TestCompressionTransformer t = new TestCompressionTransformer();
        t.setReturnClass(String.class);
        t.setBeanProperty2(12);
        t.setContainerProperty("");
        t.setBeanProperty1("this was set from the manager properties!");
        managementContext.getRegistry().registerTransformer(t);

        //Register endpoints
        JXPathFilter filter = new JXPathFilter("name");
        filter.setValue("bar");
        Map ns = new HashMap();
        ns.put("foo", "http://foo.com");
        filter.setNamespaces(ns);
        builder.registerEndpoint("test://fruitBowlPublishQ", "fruitBowlEndpoint", false, null, filter);
        builder.registerEndpoint("test://AppleQueue", "appleInEndpoint", true);
        builder.registerEndpoint("test://AppleResponseQueue", "appleResponseEndpoint", false);
        builder.registerEndpoint("test://apple.queue", "AppleQueue", false);
        builder.registerEndpoint("test://banana.queue", "Banana_Queue", false);
        builder.registerEndpoint("test://test.queue", "waterMelonEndpoint", false);
        UMOEndpoint ep = new MuleEndpoint("test://test.queue2", false);
        ep.setName("testEPWithCS");
        SimpleRetryConnectionStrategy cs = new SimpleRetryConnectionStrategy();
        cs.setRetryCount(4);
        cs.setRetryFrequency(3000);
        ep.setConnectionStrategy(cs);
        builder.getManagementContext().getRegistry().registerEndpoint(ep);

        Map props = new HashMap();
        props.put("testGlobal", "value1");
        builder.registerEndpoint("test://orangeQ", "orangeEndpoint", false, props);

        //register model
        UMOModel model = new SedaModel();
        model.setName("main");
        TestExceptionStrategy es = new TestExceptionStrategy();
        es.addEndpoint(new MuleEndpoint("test://component.exceptions", false));
        model.setExceptionListener(es);
        model.setLifecycleAdapterFactory(new TestDefaultLifecycleAdapterFactory());
        model.setEntryPointResolver(new TestEntryPointResolver());
        managementContext.getRegistry().registerModel(model);

        //register components
        UMOEndpoint ep1 = managementContext.getRegistry().lookupEndpoint("appleInEndpoint");
        ep1.setTransformer(managementContext.getRegistry().lookupTransformer("TestCompressionTransformer"));
        UMODescriptor d = builder.createDescriptor(Orange.class.getName(), "orangeComponent", null, ep1, props);

        DefaultComponentExceptionStrategy dces = new DefaultComponentExceptionStrategy();
        dces.addEndpoint(new MuleEndpoint("test://orange.exceptions", false));
        d.setExceptionListener(dces);
        //Create the inbound router
        UMOInboundRouterCollection inRouter = new InboundRouterCollection();
        inRouter.setCatchAllStrategy(new ForwardingCatchAllStrategy());
        inRouter.getCatchAllStrategy().setEndpoint(new MuleEndpoint("test2://catch.all", false));
        UMOEndpoint ep2 = builder.createEndpoint("test://orange/", "Orange", true, "TestCompressionTransformer");
        ep2.setResponseTransformer(managementContext.getRegistry().lookupTransformer("TestCompressionTransformer"));
        inRouter.addEndpoint(ep2);
        UMOEndpoint ep3 = managementContext.getRegistry().lookupEndpoint("orangeEndpoint");
        ep3.setFilter(new PayloadTypeFilter(String.class));
        ep3.setTransformer(managementContext.getRegistry().lookupTransformer("TestCompressionTransformer"));
        Map props2 = new HashMap();
        props2.put("testLocal", "value1");
        ep3.setProperties(props2);
        inRouter.addEndpoint(ep3);
        d.setInboundRouter(inRouter);

        //Nested Router
        UMONestedRouterCollection nestedRouter = new NestedRouterCollection();
        NestedRouter nr1 = new NestedRouter();
        nr1.setEndpoint(new MuleEndpoint("test://do.wash", false));
        nr1.setInterface(FruitCleaner.class);
        nr1.setMethod("wash");
        nestedRouter.addRouter(nr1);
        NestedRouter nr2 = new NestedRouter();
        nr2.setEndpoint(new MuleEndpoint("test://do.polish", false));
        nr2.setInterface(FruitCleaner.class);
        nr2.setMethod("polish");
        nestedRouter.addRouter(nr2);
        d.setNestedRouter(nestedRouter);

//Response Router
        UMOResponseRouterCollection responseRouter = new ResponseRouterCollection();
        responseRouter.addEndpoint(new MuleEndpoint("test://response1", true));
        responseRouter.addEndpoint(managementContext.getRegistry().lookupEndpoint("appleResponseEndpoint"));
        responseRouter.addRouter(new TestResponseAggregator());
        responseRouter.setTimeout(10001);
        d.setResponseRouter(responseRouter);

        //properties
        Map cprops = new HashMap();
        //cprops.put("orange", new Orange());
        cprops.put("brand", "Juicy Baby!");
        cprops.put("segments", "9");
        cprops.put("radius", "4.21");
        Map nested = new HashMap();
        nested.put("prop1", "prop1");
        nested.put("prop2", "prop2");
        cprops.put("mapProperties", nested);
        List nested2 = new ArrayList();
        nested2.add("prop1");
        nested2.add("prop2");
        nested2.add("prop3");
        cprops.put("listProperties", nested2);
        List nested3 = new ArrayList();
        nested3.add("prop4");
        nested3.add("prop5");
        nested3.add("prop6");
        cprops.put("arrayProperties", nested3);
        d.setProperties(cprops);

        d.setModelName("main");

        //register components
        managementContext.getRegistry().registerService(d);
    }
}