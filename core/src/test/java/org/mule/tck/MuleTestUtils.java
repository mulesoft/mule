/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.component.JavaComponent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.filter.Filter;
import org.mule.api.service.Service;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.component.DefaultJavaComponent;
import org.mule.construct.Flow;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.object.SingletonObjectFactory;
import org.mule.routing.MessageFilter;
import org.mule.session.DefaultMuleSession;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.mule.TestAgent;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transport.AbstractConnector;
import org.mule.util.ClassUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for creating test and Mock Mule objects
 */
public final class MuleTestUtils
{

    public static final String APPLE_SERVICE = "appleService";
    public static final String APPLE_FLOW = "appleFlow";

    // public static Endpoint getTestEndpoint(String name, String type, MuleContext
    // context) throws Exception
    // {
    // Map props = new HashMap();
    // props.put("name", name);
    // props.put("type", type);
    // props.put("endpointURI", new MuleEndpointURI("test://test"));
    // props.put("connector", "testConnector");
    // // need to build endpoint this way to avoid depenency to any endpoint jars
    // AbstractConnector connector = null;
    // connector =
    // (AbstractConnector)ClassUtils.loadClass("org.mule.tck.testmodels.mule.TestConnector",
    // AbstractMuleTestCase.class).newInstance();
    //
    // connector.setName("testConnector");
    // connector.setMuleContext(context);
    // context.applyLifecycle(connector);
    //
    // EndpointBuilder endpointBuilder = new
    // EndpointURIEndpointBuilder("test://test", context);
    // endpointBuilder.setConnector(connector);
    // endpointBuilder.setName(name);
    // if (ImmutableEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type))
    // {
    // return (Endpoint)
    // context.getEndpointFactory().getInboundEndpoint(endpointBuilder);
    // }
    // else if (ImmutableEndpoint.ENDPOINT_TYPE_SENDER.equals(type))
    // {
    // return (Endpoint)
    // context.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
    // }
    // else
    // {
    // throw new IllegalArgumentException("The endpoint type: " + type +
    // "is not recognized.");
    //
    // }
    // }

    public static InboundEndpoint getTestInboundEndpoint(String name, final MuleContext context)
            throws Exception
    {
        return (InboundEndpoint) getTestEndpoint(name, null, null, null, null, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                return context.getEndpointFactory().getInboundEndpoint(builder);
            }
        }, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, final MuleContext context)
            throws Exception
    {
        return (OutboundEndpoint) getTestEndpoint(name, null, null, null, null, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                return context.getEndpointFactory().getOutboundEndpoint(builder);
            }
        }, null);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name,
                                                         final MuleContext context,
                                                         String uri,
                                                         List<Transformer> transformers,
                                                         Filter filter,
                                                         Map<Object, Object> properties,
                                                         Connector connector) throws Exception
    {
        return (InboundEndpoint) getTestEndpoint(name, uri, transformers, filter, properties, context,
                                                 new EndpointSource()
                                                 {
                                                     @Override
                                                     public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
                                                     {
                                                         return context.getEndpointFactory().getInboundEndpoint(builder);
                                                     }
                                                 }, connector);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name,
                                                           final MuleContext context,
                                                           String uri,
                                                           List<Transformer> transformers,
                                                           Filter filter,
                                                           Map<Object, Object> properties) throws Exception
    {
        return (OutboundEndpoint) getTestEndpoint(name, uri, transformers, filter, properties, context,
                                                  new EndpointSource()
                                                  {
                                                      @Override
                                                      public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
                                                      {
                                                          return context.getEndpointFactory().getOutboundEndpoint(builder);
                                                      }
                                                  }, null);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name,
                                                         final MuleContext context,
                                                         String uri,
                                                         List<Transformer> transformers,
                                                         Filter filter,
                                                         Map<Object, Object> properties) throws Exception
    {
        return (InboundEndpoint) getTestEndpoint(name, uri, transformers, filter, properties, context,
                                                 new EndpointSource()
                                                 {
                                                     @Override
                                                     public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
                                                     {
                                                         return context.getEndpointFactory().getInboundEndpoint(builder);
                                                     }
                                                 }, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name,
                                                           final MuleContext context,
                                                           String uri,
                                                           List<Transformer> transformers,
                                                           Filter filter,
                                                           Map<Object, Object> properties,
                                                           final Connector connector) throws Exception
    {
        return (OutboundEndpoint) getTestEndpoint(name, uri, transformers, filter, properties, context,
                                                  new EndpointSource()
                                                  {
                                                      @Override
                                                      public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
                                                      {
                                                          builder.setConnector(connector);
                                                          return context.getEndpointFactory().getOutboundEndpoint(builder);
                                                      }
                                                  }, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(final MessageExchangePattern mep,
                                                           final MuleContext context,
                                                           String uri,
                                                           final Connector connector) throws Exception
    {
        return (OutboundEndpoint) getTestEndpoint(null, uri, null, null, null, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                builder.setConnector(connector);
                builder.setExchangePattern(mep);
                return context.getEndpointFactory().getOutboundEndpoint(builder);
            }
        }, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name,
                                                           final MessageExchangePattern mep,
                                                           final MuleContext context) throws Exception
    {
        return (OutboundEndpoint) getTestEndpoint(name, null, null, null, null, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                builder.setExchangePattern(mep);
                return context.getEndpointFactory().getOutboundEndpoint(builder);
            }
        }, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(final MessageExchangePattern mep,
                                                           final MuleContext context) throws Exception
    {
        return (OutboundEndpoint) getTestEndpoint(null, null, null, null, null, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                builder.setExchangePattern(mep);
                return context.getEndpointFactory().getOutboundEndpoint(builder);
            }
        }, null);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name,
                                                         final MessageExchangePattern mep,
                                                         final MuleContext context,
                                                         final Connector connector) throws Exception
    {
        return (InboundEndpoint) getTestEndpoint(name, null, null, null, null, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                builder.setExchangePattern(mep);
                return context.getEndpointFactory().getInboundEndpoint(builder);
            }
        }, connector);
    }

    public static InboundEndpoint getTestInboundEndpoint(final MessageExchangePattern mep,
                                                         final MuleContext context) throws Exception
    {
        return (InboundEndpoint) getTestEndpoint(null, null, null, null, null, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                builder.setExchangePattern(mep);
                return context.getEndpointFactory().getInboundEndpoint(builder);
            }
        }, null);
    }

    public static InboundEndpoint getTestTransactedInboundEndpoint(final MessageExchangePattern mep,
                                                                   final MuleContext context) throws Exception
    {
        return (InboundEndpoint) getTestEndpoint(null, null, null, null, null, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                builder.setExchangePattern(mep);
                TransactionConfig txConfig = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
                txConfig.setFactory(new TestTransactionFactory());
                builder.setTransactionConfig(txConfig);
                return context.getEndpointFactory().getInboundEndpoint(builder);
            }
        }, null);
    }

    private static ImmutableEndpoint getTestEndpoint(String name,
                                                     String uri,
                                                     List<Transformer> transformers,
                                                     Filter filter,
                                                     Map<Object, Object> properties,
                                                     MuleContext context,
                                                     EndpointSource source,
                                                     Connector connector) throws Exception
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("name", name);
        props.put("endpointURI", new MuleEndpointURI("test://test", context));
        props.put("connector", "testConnector");
        if (connector == null)
        {
            // need to build endpoint this way to avoid depenency to any endpoint
            // jars
            connector = (Connector) ClassUtils.loadClass("org.mule.tck.testmodels.mule.TestConnector",
                                                         AbstractMuleTestCase.class).getConstructor(MuleContext.class).newInstance(context);
        }

        connector.setName("testConnector");
        context.getRegistry().applyLifecycle(connector);

        final String endpoingUri = uri == null ? "test://test" : uri;
        final EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(endpoingUri, context);
        endpointBuilder.setConnector(connector);
        endpointBuilder.setName(name);
        if (transformers != null)
        {
            endpointBuilder.setTransformers(transformers);
        }

        if (properties != null)
        {
            endpointBuilder.setProperties(properties);
        }
        endpointBuilder.addMessageProcessor(new MessageFilter(filter));
        return source.getEndpoint(endpointBuilder);
    }

    private interface EndpointSource
    {

        ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException;
    }

    // public static Endpoint getTestSchemeMetaInfoEndpoint(String name, String type,
    // String protocol, MuleContext context)
    // throws Exception
    // {
    // // need to build endpoint this way to avoid depenency to any endpoint jars
    // AbstractConnector connector = null;
    // connector = (AbstractConnector)
    // ClassUtils.loadClass("org.mule.tck.testmodels.mule.TestConnector",
    // AbstractMuleTestCase.class).newInstance();
    //
    // connector.setName("testConnector");
    // connector.setMuleContext(context);
    // context.applyLifecycle(connector);
    // connector.registerSupportedProtocol(protocol);
    //
    // EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test:" +
    // protocol + "://test", context);
    // endpointBuilder.setConnector(connector);
    // endpointBuilder.setName(name);
    // if (ImmutableEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type))
    // {
    // return (Endpoint)
    // context.getEndpointFactory().getInboundEndpoint(endpointBuilder);
    // }
    // else if (ImmutableEndpoint.ENDPOINT_TYPE_SENDER.equals(type))
    // {
    // return (Endpoint)
    // context.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
    // }
    // else
    // {
    // throw new IllegalArgumentException("The endpoint type: " + type +
    // "is not recognized.");
    //
    // }
    // }

    public static ImmutableEndpoint getTestSchemeMetaInfoInboundEndpoint(String name,
                                                                         String protocol,
                                                                         final MuleContext context)
            throws Exception
    {
        return getTestSchemeMetaInfoEndpoint(name, protocol, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                return context.getEndpointFactory().getInboundEndpoint(builder);
            }
        });
    }

    public static ImmutableEndpoint getTestSchemeMetaInfoOutboundEndpoint(String name,
                                                                          String protocol,
                                                                          final MuleContext context)
            throws Exception
    {
        return getTestSchemeMetaInfoEndpoint(name, protocol, context, new EndpointSource()
        {
            @Override
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                return context.getEndpointFactory().getOutboundEndpoint(builder);
            }
        });
    }

    private static ImmutableEndpoint getTestSchemeMetaInfoEndpoint(String name,
                                                                   String protocol,
                                                                   MuleContext context,
                                                                   EndpointSource source) throws Exception
    {
        // need to build endpoint this way to avoid depenency to any endpoint jars
        final AbstractConnector connector = (AbstractConnector) ClassUtils.loadClass(
                "org.mule.tck.testmodels.mule.TestConnector", AbstractMuleTestCase.class).newInstance();

        connector.setName("testConnector");
        context.getRegistry().applyLifecycle(connector);
        connector.registerSupportedProtocol(protocol);

        final EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(
                "test:" + protocol + "://test", context);
        endpointBuilder.setConnector(connector);
        endpointBuilder.setName(name);
        return source.getEndpoint(endpointBuilder);
    }

    /**
     * Supply no service, no endpoint
     */
    public static MuleEvent getTestEvent(Object data, MuleContext context) throws Exception
    {
        return getTestEvent(data, getTestService(context), MessageExchangePattern.REQUEST_RESPONSE, context);
    }

    public static MuleEvent getTestEvent(Object data, MessageExchangePattern mep, MuleContext context)
            throws Exception
    {
        return getTestEvent(data, getTestService(context), mep, context);
    }

    public static MuleEvent getTestEventUsingFlow(Object data, MessageExchangePattern mep, MuleContext context)
            throws Exception
    {
        return getTestEvent(data, getTestFlow(context), mep, context);
    }

    //    public static MuleEvent getTestInboundEvent(Object data, MuleContext context) throws Exception
    //    {
    //        return getTestInboundEvent(data, getTestService(context), MessageExchangePattern.REQUEST_RESPONSE,
    //            context);
    //    }
    //
    //    public static MuleEvent getTestInboundEvent(Object data, MessageExchangePattern mep, MuleContext context)
    //        throws Exception
    //    {
    //        return getTestInboundEvent(data, getTestService(context), mep, context);
    //    }

    /**
     * Supply service but no endpoint
     */
    @Deprecated
    public static MuleEvent getTestEvent(Object data, Service service, MuleContext context) throws Exception
    {
        return getTestEvent(data, service, getTestInboundEndpoint("test1",
                                                                  MessageExchangePattern.REQUEST_RESPONSE, context, null), context);
    }

    public static MuleEvent getTestEvent(Object data,
                                         FlowConstruct flowConstruct,
                                         MessageExchangePattern mep,
                                         MuleContext context) throws Exception
    {
        return getTestEvent(data, flowConstruct, getTestInboundEndpoint("test1", mep, context, null), context);
    }

    //    public static MuleEvent getTestInboundEvent(Object data, Service service, MuleContext context)
    //        throws Exception
    //    {
    //        return getTestEvent(data, service, getTestInboundEndpoint("test1",
    //            MessageExchangePattern.REQUEST_RESPONSE, context, null), context);
    //    }
    //
    //    public static MuleEvent getTestInboundEvent(Object data,
    //                                                Service service,
    //                                                MessageExchangePattern mep,
    //                                                MuleContext context) throws Exception
    //    {
    //        return getTestEvent(data, service, getTestInboundEndpoint("test1", mep, context, null), context);
    //    }

    /**
     * Supply endpoint but no service
     */
    public static MuleEvent getTestEvent(Object data, InboundEndpoint endpoint, MuleContext context)
            throws Exception
    {
        return getTestEvent(data, getTestService(context), endpoint, context);
    }

    public static MuleEvent getTestEvent(Object data,
                                         FlowConstruct flowConstruct,
                                         InboundEndpoint endpoint,
                                         MuleContext context) throws Exception
    {
        final MuleSession session = getTestSession(flowConstruct, context);

        final MuleMessageFactory factory = endpoint.getConnector().createMuleMessageFactory();
        final MuleMessage message = factory.create(data, endpoint.getEncoding(), context);

        return new DefaultMuleEvent(message, endpoint, flowConstruct, session);
    }

    public static MuleEvent getTestEvent(Object data, MuleSession session, MuleContext context)
            throws Exception
    {
        final InboundEndpoint endpoint = getTestInboundEndpoint("test1",
                                                                MessageExchangePattern.REQUEST_RESPONSE, context, null);
        final MuleMessageFactory factory = endpoint.getConnector().createMuleMessageFactory();
        final MuleMessage message = factory.create(data, endpoint.getEncoding(), context);

        return new DefaultMuleEvent(message, endpoint, getTestService(context), session);
    }

    public static MuleEventContext getTestEventContext(Object data,
                                                       MessageExchangePattern mep,
                                                       MuleContext context) throws Exception
    {
        try
        {
            final MuleEvent event = getTestEvent(data, mep, context);
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
        final Transformer t = new TestCompressionTransformer();
        t.initialise();
        return t;
    }

    public static MuleSession getTestSession(FlowConstruct flowConstruct, MuleContext context)
    {
        return new DefaultMuleSession();
    }

    public static MuleSession getTestSession(MuleContext context)
    {
        return getTestSession(null, context);
    }

    public static TestConnector getTestConnector(MuleContext context) throws Exception
    {
        final TestConnector testConnector = new TestConnector(context);
        testConnector.setName("testConnector");
        context.getRegistry().applyLifecycle(testConnector);
        return testConnector;
    }

    @Deprecated
    public static Service getTestService(MuleContext context) throws Exception
    {
        return getTestService(APPLE_SERVICE, Apple.class, context);
    }

    public static Flow getTestFlow(MuleContext context) throws Exception
    {
        return getTestFlow(APPLE_FLOW, context);
    }

    @Deprecated
    public static Service getTestService(String name, Class<?> clazz, MuleContext context) throws Exception
    {
        return getTestService(name, clazz, null, context);
    }

    @Deprecated
    public static Service getTestService(String name, Class<?> clazz, Map props, MuleContext context)
            throws Exception
    {
        return getTestService(name, clazz, props, context, true);
    }

    public static Flow getTestFlow(String name, MuleContext context) throws Exception
    {
        return getTestFlow(name, context, true);
    }

    @Deprecated
    public static Service getTestService(String name,
                                         Class<?> clazz,
                                         Map props,
                                         MuleContext context,
                                         boolean initialize) throws Exception
    {
        final SedaModel model = new SedaModel();
        model.setMuleContext(context);
        context.getRegistry().applyLifecycle(model);

        final Service service = new SedaService(context);
        service.setName(name);
        final SingletonObjectFactory of = new SingletonObjectFactory(clazz, props);
        of.initialise();
        final JavaComponent component = new DefaultJavaComponent(of);
        ((MuleContextAware) component).setMuleContext(context);
        service.setComponent(component);
        service.setModel(model);
        if (initialize)
        {
            context.getRegistry().registerService(service);
        }

        return service;
    }

    public static Flow getTestFlow(String name, MuleContext context, boolean initialize)
            throws Exception
    {
        final Flow flow = new Flow(name, context);
        if (initialize)
        {
            context.getRegistry().registerFlowConstruct(flow);
        }

        return flow;
    }

    public static TestAgent getTestAgent() throws Exception
    {
        final TestAgent t = new TestAgent();
        t.initialise();
        return t;
    }
    
    /**
     * Execute callback with a given system property set and replaces the system property with it's original
     * value once done. Useful for asserting behaviour that is dependent on the presence of a system property.
     * 
     * @param propertyName Name of system property to set
     * @param propertyValue Value of system property
     * @param callback Callback implementing the the test code and assertions to be run with system property
     *            set.
     * @throws Exception any exception thrown by the execution of callback
     */
    public static void testWithSystemProperty(String propertyName, String propertyValue, TestCallback callback)
        throws Exception
    {
        assert propertyName != null && callback != null;
        String originalPropertyValue = null;
        try
        {
            if (propertyValue == null)
            {
                originalPropertyValue = System.clearProperty(propertyName);
            }
            else
            {
                originalPropertyValue = System.setProperty(propertyName, propertyValue);
            }
            callback.run();
        }
        finally
        {
            if (originalPropertyValue == null)
            {
                System.clearProperty(propertyName);
            }
            else
            {
                System.setProperty(propertyName, originalPropertyValue);
            }
        }
    }

    public static interface TestCallback
    {
        void run() throws Exception;
    }

}
