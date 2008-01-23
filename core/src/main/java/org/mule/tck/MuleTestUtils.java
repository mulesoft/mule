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

import org.mule.DefaultMuleContext;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleSession;
import org.mule.api.component.Component;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionFactory;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.model.seda.SedaComponent;
import org.mule.model.seda.SedaModel;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.mule.TestAgent;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.transport.AbstractConnector;
import org.mule.util.ClassUtils;
import org.mule.util.object.ObjectFactory;
import org.mule.util.object.SingletonObjectFactory;

import com.mockobjects.dynamic.Mock;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for creating test and Mock Mule objects
 */
public final class MuleTestUtils
{
    public static Endpoint getTestEndpoint(String name, String type, MuleContext context) throws Exception
    {
        Map props = new HashMap();
        props.put("name", name);
        props.put("type", type);
        props.put("endpointURI", new MuleEndpointURI("test://test"));
        props.put("connector", "testConnector");
        // need to build endpoint this way to avoid depenency to any endpoint jars
        AbstractConnector connector = null;
        connector = (AbstractConnector)ClassUtils.loadClass("org.mule.tck.testmodels.mule.TestConnector",
            AbstractMuleTestCase.class).newInstance();

        connector.setName("testConnector");
        connector.setMuleContext(context);
        context.applyLifecycle(connector);

        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test://test", context);
        endpointBuilder.setConnector(connector);
        endpointBuilder.setName(name);
        if (ImmutableEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type))
        {
            return (Endpoint) context.getRegistry().lookupEndpointFactory().getInboundEndpoint(endpointBuilder);
        }
        else if (ImmutableEndpoint.ENDPOINT_TYPE_SENDER.equals(type))
        {
            return (Endpoint) context.getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointBuilder);
        }
        else
        {
            throw new IllegalArgumentException("The endpoint type: " + type + "is not recognized.");

        }
    }
    
    public static Endpoint getTestSchemeMetaInfoEndpoint(String name, String type, String protocol, MuleContext context)
        throws Exception
    {
        // need to build endpoint this way to avoid depenency to any endpoint jars
        AbstractConnector connector = null;
        connector = (AbstractConnector) ClassUtils.loadClass("org.mule.tck.testmodels.mule.TestConnector",
            AbstractMuleTestCase.class).newInstance();

        connector.setName("testConnector");
        connector.setMuleContext(context);
        context.applyLifecycle(connector);
        connector.registerSupportedProtocol(protocol);

        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test:" + protocol + "://test", context);
        endpointBuilder.setConnector(connector);
        endpointBuilder.setName(name);
        if (ImmutableEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type))
        {
            return (Endpoint) context.getRegistry().lookupEndpointFactory().getInboundEndpoint(endpointBuilder);
        }
        else if (ImmutableEndpoint.ENDPOINT_TYPE_SENDER.equals(type))
        {
            return (Endpoint) context.getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointBuilder);
        }
        else
        {
            throw new IllegalArgumentException("The endpoint type: " + type + "is not recognized.");

        }
    }

    /** Supply no component, no endpoint */
    public static MuleEvent getTestEvent(Object data, MuleContext context) throws Exception
    {
        return getTestEvent(data, getTestComponent(context), context);
    }

    /** Supply component but no endpoint */
    public static MuleEvent getTestEvent(Object data, Component component, MuleContext context) throws Exception
    {
        return getTestEvent(data, component, getTestEndpoint("test1", Endpoint.ENDPOINT_TYPE_SENDER, context), context);
    }

    /** Supply endpoint but no component */
    public static MuleEvent getTestEvent(Object data, ImmutableEndpoint endpoint, MuleContext context) throws Exception
    {
        return getTestEvent(data, getTestComponent(context), endpoint, context);
    }

    public static MuleEvent getTestEvent(Object data, Component component, ImmutableEndpoint endpoint, MuleContext context) throws Exception
    {
        MuleSession session = getTestSession(component);
        return new DefaultMuleEvent(new DefaultMuleMessage(data, new HashMap()), endpoint, session, true);
    }

    public static MuleEventContext getTestEventContext(Object data, MuleContext context) throws Exception
    {
        try
        {
            MuleEvent event = getTestEvent(data, context);
            RequestContext.setEvent(event);
            return RequestContext.getEventContext();
        }
        finally
        {
            RequestContext.setEvent(null);
        }
    }

    public static Transformer getTestTransformer() throws Exception
    {
        Transformer t = new TestCompressionTransformer();
        t.initialise();
        return t;
    }

    public static MuleSession getTestSession(Component component)
    {
        return new DefaultMuleSession(component);
    }

    public static MuleSession getTestSession()
    {
        return getTestSession(null);
    }

    public static TestConnector getTestConnector(MuleContext context) throws Exception
    {
        TestConnector testConnector = new TestConnector();
        testConnector.setName("testConnector");
        testConnector.setMuleContext(context);
        context.applyLifecycle(testConnector);
        return testConnector;
    }

    public static Component getTestComponent(MuleContext context) throws Exception
    {
        return getTestComponent("appleService", Apple.class, context);
    }

    public static Component getTestComponent(String name, Class clazz, MuleContext context) throws Exception
    {
        return getTestComponent(name, clazz, null, context);
    }

    public static Component getTestComponent(String name, Class clazz, Map props, MuleContext context) throws Exception
    {
        return getTestComponent(name, clazz, props, context, true);        
    }

    public static Component getTestComponent(String name, Class clazz, Map props, MuleContext context, boolean initialize) throws Exception
    {
        SedaModel model = new SedaModel();
        model.setMuleContext(context);
        context.applyLifecycle(model);
        
        Component c = new SedaComponent();
        c.setName(name);
        ObjectFactory of = new SingletonObjectFactory(clazz, props);
        of.initialise();
        c.setServiceFactory(of);
        c.setModel(model);
        if (initialize)
        {
            context.getRegistry().registerComponent(c);
            //TODO Why is this necessary
            OutboundRouter router = new OutboundPassThroughRouter();
            c.getOutboundRouter().addRouter(router);
        }

        return c;
    }

    public static TestAgent getTestAgent() throws Exception
    {
        TestAgent t = new TestAgent();
        t.initialise();
        return t;
    }

    public static Mock getMockSession()
    {
        return new Mock(MuleSession.class, "umoSession");
    }

    public static Mock getMockMessageDispatcher()
    {
        return new Mock(MessageDispatcher.class, "umoMessageDispatcher");
    }

    public static Mock getMockMessageDispatcherFactory()
    {
        return new Mock(MessageDispatcherFactory.class, "umoMessageDispatcherFactory");
    }

    public static Mock getMockConnector()
    {
        return new Mock(Connector.class, "umoConnector");
    }

    public static Mock getMockEvent()
    {
        return new Mock(MuleEvent.class, "umoEvent");
    }

    public static Mock getMockMuleContext()
    {
        return new Mock(DefaultMuleContext.class, "muleMuleContext");
    }

    public static Mock getMockEndpoint()
    {
        return new Mock(Endpoint.class, "umoEndpoint");
    }

    public static Mock getMockEndpointURI()
    {
        return new Mock(EndpointURI.class, "umoEndpointUri");
    }

    public static Mock getMockTransaction()
    {
        return new Mock(Transaction.class, "umoTransaction");
    }

    public static Mock getMockTransactionFactory()
    {
        return new Mock(TransactionFactory.class, "umoTransactionFactory");
    }
}
