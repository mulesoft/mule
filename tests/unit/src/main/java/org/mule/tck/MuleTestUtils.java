/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.mockito.Mockito.spy;

import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.api.transport.MuleMessageFactory;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.endpoint.MuleEndpointURI;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestAgent;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import java.util.ArrayList;
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

    public static Injector spyInjector(MuleContext muleContext)
    {
        Injector spy = spy(muleContext.getInjector());
        ((DefaultMuleContext) muleContext).setInjector(spy);

        return spy;
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
        return getTestEvent(data, getTestFlow(context), MessageExchangePattern.REQUEST_RESPONSE, context);
    }

    public static MuleEvent getTestEvent(Object data, MessageExchangePattern mep, MuleContext context)
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


    public static MuleEvent getTestEvent(Object data,
                                         FlowConstruct flowConstruct,
                                         MessageExchangePattern mep,
                                         MuleContext context) throws Exception
    {
        final MuleSession session = getTestSession(flowConstruct, context);
        final DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(data, context), mep, flowConstruct, session);
        event.populateFieldsFromInboundEndpoint(getTestInboundEndpoint("test1", mep, context, null));
        return event;
    }


    /**
     * Supply endpoint but no service
     */
    public static MuleEvent getTestEvent(Object data, FlowConstruct flowConstruct, MuleContext context)
            throws Exception
    {
        final MuleSession session = getTestSession(flowConstruct, context);
        final DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(data, context), flowConstruct, session);
        event.populateFieldsFromInboundEndpoint(getTestInboundEndpoint("test1", MessageExchangePattern.REQUEST_RESPONSE, context, null));
        return event;
    }

    /**
     * Supply endpoint but no service
     */
    public static MuleEvent getTestEvent(Object data, InboundEndpoint endpoint, MuleContext context)
            throws Exception
    {
        return getTestEvent(data, getTestFlow(context), endpoint, context);
    }

    public static MuleEvent getTestEvent(Object data,
                                         FlowConstruct flowConstruct,
                                         InboundEndpoint endpoint,
                                         MuleContext context) throws Exception
    {
        final MuleSession session = getTestSession(flowConstruct, context);

        final MuleMessageFactory factory = endpoint.getConnector().createMuleMessageFactory();
        final MuleMessage message = factory.create(data, endpoint.getEncoding(), context);

        final DefaultMuleEvent event = new DefaultMuleEvent(message, flowConstruct, session);
        event.populateFieldsFromInboundEndpoint(endpoint);
        return event;
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

    public static Flow getTestFlow(MuleContext context) throws Exception
    {
        return getTestFlow(APPLE_FLOW, context);
    }

    public static Flow getTestFlow(String name, Class<?> clazz, MuleContext context) throws Exception
    {
        return getTestFlow(name, clazz, null, context);
    }

    @Deprecated
    public static Flow getTestFlow(String name, Class<?> clazz, Map props, MuleContext context)
            throws Exception
    {
        return getTestFlow(name, clazz, props, context, true);
    }

    public static Flow getTestFlow(String name, MuleContext context) throws Exception
    {
        return getTestFlow(name, context, true);
    }

    public static Flow getTestFlow(String name,
                                         Class<?> clazz,
                                         Map props,
                                         MuleContext context,
                                         boolean initialize) throws Exception
    {
        final SingletonObjectFactory of = new SingletonObjectFactory(clazz, props);
        of.initialise();
        final JavaComponent component = new DefaultJavaComponent(of);
        ((MuleContextAware) component).setMuleContext(context);

        return getTestFlow(name, component, initialize, context);
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

    public static Flow getTestFlow(String name, Object component,  boolean initialize, MuleContext context)
            throws Exception
    {
        final Flow flow = new Flow(name, context);
        flow.setMessageProcessors(new ArrayList<MessageProcessor>());
        if (component instanceof Component)
        {
            flow.getMessageProcessors().add((MessageProcessor) component);
        }
        else
        {
            flow.getMessageProcessors().add(new DefaultJavaComponent(new SingletonObjectFactory(component)));

        }
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

    /**
     * Returns a currently running {@link Thread} of the given {@code name}
     *
     * @param name the name of the {@link Thread} you're looking for
     * @return a {@link Thread} or {@code null} if none found
     */
    public static Thread getRunningThreadByName(String name)
    {
        for (Thread thread : Thread.getAllStackTraces().keySet())
        {
            if (thread.getName().equals(name))
            {
                return thread;
            }
        }

        return null;
    }
}
