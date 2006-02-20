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
*
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
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.routing.response.ResponseMessageRouter;
import org.mule.tck.AbstractScriptConfigBuilderTestCase;
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
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.util.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class QuickConfigurationBuilderTestCase extends AbstractScriptConfigBuilderTestCase {

    public String getConfigResources() {
        return null;
    }

    public ConfigurationBuilder getBuilder() {

        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        UMOManager m = builder.getManager();
        //Disable the admin agent
        MuleManager.getConfiguration().setServerUrl(Utility.EMPTY_STRING);
        try {
            //set global properties
            m.setProperty("doCompression", "true");

            //Set a dummy TX manager
            m.setTransactionManager(new TestTransactionManagerFactory().create());
            //register agents
            UMOAgent agent = new JmxAgent();
            agent.setName("jmxAgent");
            m.registerAgent(agent);

            //register connector
            TestConnector c = new TestConnector();
            c.setName("dummyConnector");
            c.setExceptionListener(new TestExceptionStrategy());
            SimpleRetryConnectionStrategy cs = new SimpleRetryConnectionStrategy();
            cs.setRetryCount(4);
            cs.setFrequency(3000);
            c.setConnectionStrategy(cs);
            m.registerConnector(c);

            //Endpoint identifiers
            m.registerEndpointIdentifier("AppleQueue", "test://apple.queue");
            m.registerEndpointIdentifier("Banana_Queue", "test://banana.queue");
            m.registerEndpointIdentifier("Test Queue", "test://test.queue");

            //Register transformers
            TestCompressionTransformer t = new TestCompressionTransformer();
            t.setReturnClass(String.class);
            t.setBeanProperty2(12);
            t.setContainerProperty("");
            t.setBeanProperty1("this was set from the manager properties!");
            m.registerTransformer(t);

            NoActionTransformer t2 = new NoActionTransformer();
            t2.setReturnClass(byte[].class);
            m.registerTransformer(t2);

            //Register endpoints
            JXPathFilter filter = new JXPathFilter("name");
            filter.setValue("bar");
            Map ns = new HashMap();
            ns.put("foo", "http://foo.com");
            filter.setNamespaces(ns);
            builder.registerEndpoint( "test://fruitBowlPublishQ", "fruitBowlEndpoint", false, null, filter);
            builder.registerEndpoint("Test Queue", "waterMelonEndpoint", false);
            builder.registerEndpoint("test://AppleQueue", "appleInEndpoint", true);
            builder.registerEndpoint("test://AppleResponseQueue", "appleResponseEndpoint", false);
            Map props = new HashMap();
            props.put("testGlobal", "value1");
            builder.registerEndpoint( "test://orangeQ", "orangeEndpoint",false, props);

            //Register Interceptors
            InterceptorStack stack = new InterceptorStack();
            List interceptors = new ArrayList();
            interceptors.add(new LoggingInterceptor());
            interceptors.add(new TimerInterceptor());
            stack.setInterceptors(interceptors);
            m.registerInterceptorStack("default", stack);

            //register model
            UMOModel model = new SedaModel();
            model.setName("test-model");
            TestExceptionStrategy es = new TestExceptionStrategy();
            es.addEndpoint(new MuleEndpoint("test://component.exceptions", false));
            model.setExceptionListener(es);
            model.setLifecycleAdapterFactory(new TestDefaultLifecycleAdapterFactory());
            model.setEntryPointResolver(new TestEntryPointResolver());
            m.setModel(model);

            //register components
            UMOEndpoint ep1 = m.lookupEndpoint("appleInEndpoint");
            ep1.setTransformer(m.lookupTransformer("TestCompressionTransformer"));
            UMODescriptor d = builder.createDescriptor("local:orange", "orangeComponent", null, ep1, props);
            DefaultComponentExceptionStrategy dces = new DefaultComponentExceptionStrategy();
            dces.addEndpoint(new MuleEndpoint("test://orange.exceptions", false));
            d.setExceptionListener(dces);
            //Create the inbound router
            UMOInboundMessageRouter inRouter = new InboundMessageRouter();
            inRouter.setCatchAllStrategy(new ForwardingCatchAllStrategy());
            inRouter.getCatchAllStrategy().setEndpoint(new MuleEndpoint("test://catch.all", false));
            UMOEndpoint ep2 = builder.createEndpoint("test://orange/", "Orange", true, "TestCompressionTransformer");
            ep2.setResponseTransformer(m.lookupTransformer("TestCompressionTransformer"));
            inRouter.addEndpoint(ep2);
            UMOEndpoint ep3 = m.lookupEndpoint("orangeEndpoint");
            ep3.setFilter(new PayloadTypeFilter(String.class));
            ep3.setTransformer(m.lookupTransformer("TestCompressionTransformer"));
            Map props2 = new HashMap();
            props2.put("testLocal", "value1");
            ep3.setProperties(props2);
            inRouter.addEndpoint(ep3);
            d.setInboundRouter(inRouter);

            //Response Router
            UMOResponseMessageRouter responseRouter = new ResponseMessageRouter();
            responseRouter.addEndpoint(new MuleEndpoint("test://response1", true));
            responseRouter.addEndpoint(m.lookupEndpoint("appleResponseEndpoint"));
            responseRouter.addRouter(new TestResponseAggregator());
            responseRouter.setTimeout(10001);
            d.setResponseRouter(responseRouter);

            //Interceptors
            UMOInterceptorStack stack2 = m.lookupInterceptorStack("default");
            d.setInterceptors(new ArrayList(stack2.getInterceptors()));
            d.getInterceptors().add(new TimerInterceptor());

            //properties
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

            //register components
            m.getModel().registerComponent(d);
            m.start();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        return builder;
    }
}
