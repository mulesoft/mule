/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.model.seda.SedaModel;
import org.mule.interceptors.InterceptorStack;
import org.mule.interceptors.LoggingInterceptor;
import org.mule.interceptors.TimerInterceptor;
import org.mule.management.agents.JmxAgent;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.registry.UMORegistry;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.nested.NestedRouter;
import org.mule.routing.nested.NestedRouterCollection;
import org.mule.routing.response.ResponseRouterCollection;
import org.mule.tck.AbstractScriptConfigBuilderTestCase;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestDefaultLifecycleAdapterFactory;
import org.mule.tck.testmodels.mule.TestEntryPointResolver;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestResponseAggregator;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;
import org.mule.transformers.NoActionTransformer;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.model.UMOModel;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOResponseRouterCollection;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickConfigurationBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{

    public String getConfigResources()
    {
        return null;
    }

    public ConfigurationBuilder getBuilder()
    {

        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        UMOManager m = builder.getManager();
        UMORegistry r = MuleManager.getRegistry();
        // Disable the admin agent
        //MuleManager.getConfiguration().setServerUrl(StringUtils.EMPTY);
        try
        {
            // set global properties
            m.setProperty("doCompression", "true");

            // Set a dummy TX manager
            m.setTransactionManager(new TestTransactionManagerFactory().create());
            // register agents
            UMOAgent agent = new JmxAgent();
            agent.setName("jmxAgent");
            r.registerAgent(agent);

            // register connector
            TestConnector c = new TestConnector();
            c.setName("dummyConnector");
            c.setExceptionListener(new TestExceptionStrategy());
            SimpleRetryConnectionStrategy cs = new SimpleRetryConnectionStrategy();
            cs.setRetryCount(4);
            cs.setFrequency(3000);
            c.setConnectionStrategy(cs);
            r.registerConnector(c);

            // Endpoint identifiers
            r.registerEndpointIdentifier("AppleQueue", "test://apple.queue");
            r.registerEndpointIdentifier("Banana_Queue", "test://banana.queue");
            r.registerEndpointIdentifier("Test Queue", "test://test.queue");

            // Register transformers
            TestCompressionTransformer t = new TestCompressionTransformer();
            t.setReturnClass(String.class);
            t.setBeanProperty2(12);
            t.setContainerProperty("");
            t.setBeanProperty1("this was set from the manager properties!");
            r.registerTransformer(t);

            NoActionTransformer t2 = new NoActionTransformer();
            t2.setReturnClass(byte[].class);
            r.registerTransformer(t2);

            // Register endpoints
            JXPathFilter filter = new JXPathFilter("name");
            filter.setExpectedValue("bar");
            Map ns = new HashMap();
            ns.put("foo", "http://foo.com");
            filter.setNamespaces(ns);
            builder.registerEndpoint("test://fruitBowlPublishQ", "fruitBowlEndpoint", false, null, filter);
            builder.registerEndpoint("Test Queue", "waterMelonEndpoint", false);
            builder.registerEndpoint("test://AppleQueue", "appleInEndpoint", true);
            builder.registerEndpoint("test://AppleResponseQueue", "appleResponseEndpoint", false);
            Map props = new HashMap();
            props.put("testGlobal", "value1");
            builder.registerEndpoint("test://orangeQ", "orangeEndpoint", false, props);

            // Register Interceptors
            InterceptorStack stack = new InterceptorStack();
            List interceptors = new ArrayList();
            interceptors.add(new LoggingInterceptor());
            interceptors.add(new TimerInterceptor());
            stack.setInterceptors(interceptors);
            r.registerInterceptorStack("default", stack);

            // register model
            UMOModel model = new SedaModel();
            model.setName("main");
            TestExceptionStrategy es = new TestExceptionStrategy();
            es.addEndpoint(new MuleEndpoint("test://component.exceptions", false));
            model.setExceptionListener(es);
            model.setLifecycleAdapterFactory(new TestDefaultLifecycleAdapterFactory());
            model.setEntryPointResolver(new TestEntryPointResolver());
            r.registerModel(model);

            // register components
            UMOEndpoint ep1 = r.lookupEndpoint("appleInEndpoint");
            ep1.setTransformer(r.lookupTransformer("TestCompressionTransformer"));
            UMODescriptor d = builder.createDescriptor("orange", "orangeComponent", null, ep1, props);
            d.setContainer("descriptor");
            DefaultComponentExceptionStrategy dces = new DefaultComponentExceptionStrategy();
            dces.addEndpoint(new MuleEndpoint("test://orange.exceptions", false));
            d.setExceptionListener(dces);
            // Create the inbound router
            UMOInboundRouterCollection inRouter = new InboundRouterCollection();
            inRouter.setCatchAllStrategy(new ForwardingCatchAllStrategy());
            inRouter.getCatchAllStrategy().setEndpoint(new MuleEndpoint("test://catch.all", false));
            UMOEndpoint ep2 = builder.createEndpoint("test://orange/", "Orange", true,
                "TestCompressionTransformer");
            ep2.setResponseTransformer(r.lookupTransformer("TestCompressionTransformer"));
            inRouter.addEndpoint(ep2);
            UMOEndpoint ep3 = r.lookupEndpoint("orangeEndpoint");
            ep3.setFilter(new PayloadTypeFilter(String.class));
            ep3.setTransformer(r.lookupTransformer("TestCompressionTransformer"));
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
            
            // Response Router
            UMOResponseRouterCollection responseRouter = new ResponseRouterCollection();
            responseRouter.addEndpoint(new MuleEndpoint("test://response1", true));
            responseRouter.addEndpoint(r.lookupEndpoint("appleResponseEndpoint"));
            responseRouter.addRouter(new TestResponseAggregator());
            responseRouter.setTimeout(10001);
            d.setResponseRouter(responseRouter);

            // Interceptors
            UMOInterceptorStack stack2 = r.lookupInterceptorStack("default");
            d.setInterceptors(new ArrayList(stack2.getInterceptors()));
            d.getInterceptors().add(new TimerInterceptor());

            // properties
            Map cprops = new HashMap();
            cprops.put("orange", new Orange());
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

            // register components
            UMOModel mainModel = r.lookupModel("main");
            r.registerComponent(d, mainModel.getName());
            if (StringUtils.isBlank(m.getId()))
            {
                // if running with JMX agent, manager ID is mandatory
                m.setId("" + System.currentTimeMillis());
            }
            m.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
        return builder;
    }
}
