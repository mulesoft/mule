/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.config.MuleProperties;
import org.mule.config.QueueProfile;
import org.mule.config.spring.parsers.ConfigurationDefinitionParser;
import org.mule.config.spring.parsers.ConnectionStrategyDefinitionParser;
import org.mule.config.spring.parsers.CustomElementDefinitionParser;
import org.mule.config.spring.parsers.EndpointDefinitionParser;
import org.mule.config.spring.parsers.EndpointRefDefinitionParser;
import org.mule.config.spring.parsers.FilterDefinitionParser;
import org.mule.config.spring.parsers.InheritedModelDefinitionParser;
import org.mule.config.spring.parsers.MapBeanDefinitionParser;
import org.mule.config.spring.parsers.PoolingProfileDefinitionParser;
import org.mule.config.spring.parsers.PropertiesBeanDefinitionParser;
import org.mule.config.spring.parsers.RouterDefinitionParser;
import org.mule.config.spring.parsers.ServiceDescriptorDefinitionParser;
import org.mule.config.spring.parsers.ServiceOverridesDefinitionParser;
import org.mule.config.spring.parsers.SimpleChildDefinitionParser;
import org.mule.config.spring.parsers.SingleElementDefinitionParser;
import org.mule.config.spring.parsers.ThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.TransactionConfigDefinitionParser;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.container.JndiContainerContext;
import org.mule.impl.container.PropertiesContainerContext;
import org.mule.impl.container.RmiContainerContext;
import org.mule.impl.model.direct.DirectModel;
import org.mule.impl.model.pipeline.PipelineModel;
import org.mule.impl.model.resolvers.CallableEntryPointResolver;
import org.mule.impl.model.seda.SedaModel;
import org.mule.impl.model.seda.optimised.OptimisedSedaModel;
import org.mule.impl.model.streaming.StreamingModel;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.inbound.CorrelationAggregator;
import org.mule.routing.inbound.CorrelationEventResequencer;
import org.mule.routing.inbound.IdempotentReceiver;
import org.mule.routing.inbound.IdempotentSecureHashReceiver;
import org.mule.routing.inbound.InboundPassThroughRouter;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.inbound.MessageChunkingAggregator;
import org.mule.routing.inbound.SelectiveConsumer;
import org.mule.routing.inbound.WireTap;
import org.mule.routing.nested.NestedRouter;
import org.mule.routing.nested.NestedRouterCollection;
import org.mule.routing.outbound.ChainingRouter;
import org.mule.routing.outbound.EndpointSelector;
import org.mule.routing.outbound.ExceptionBasedRouter;
import org.mule.routing.outbound.FilteringListMessageSplitter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.routing.outbound.MessageChunkingRouter;
import org.mule.routing.outbound.MulticastingRouter;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.routing.outbound.TemplateEndpointRouter;
import org.mule.routing.response.ResponseRouterCollection;
import org.mule.transaction.lookup.GenericTransactionManagerLookupFactory;
import org.mule.transaction.lookup.JBossTransactionManagerLookupFactory;
import org.mule.transaction.lookup.JRunTransactionManagerLookupFactory;
import org.mule.transaction.lookup.Resin3TransactionManagerLookupFactory;
import org.mule.transaction.lookup.WeblogicTransactionManagerLookupFactory;
import org.mule.transaction.lookup.WebsphereTransactionManagerLookupFactory;
import org.mule.transformers.NoActionTransformer;
import org.mule.transformers.codec.Base64Decoder;
import org.mule.transformers.codec.Base64Encoder;
import org.mule.transformers.codec.XmlEntityDecoder;
import org.mule.transformers.codec.XmlEntityEncoder;
import org.mule.transformers.compression.GZipCompressTransformer;
import org.mule.transformers.compression.GZipUncompressTransformer;
import org.mule.transformers.encryption.DecryptionTransformer;
import org.mule.transformers.encryption.EncryptionTransformer;
import org.mule.transformers.simple.ByteArrayToHexString;
import org.mule.transformers.simple.ByteArrayToObject;
import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.transformers.simple.HexStringToByteArray;
import org.mule.transformers.simple.ObjectToByteArray;
import org.mule.transformers.simple.SerializableToByteArray;

import java.util.HashMap;

/**
 * This is the core namespace handler for Mule and configures all Mule configuration elements under the
 * <code>http://www.mulesource.org/schema/mule/core/2.0</code> Namespace.
 */
public class MuleNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        //Common elements
        registerBeanDefinitionParser("configuration", new ConfigurationDefinitionParser());
        registerBeanDefinitionParser("environment-properties", new MapBeanDefinitionParser(HashMap.class, MuleProperties.OBJECT_MULE_APPLICATION_PROPERTIES));
        registerBeanDefinitionParser("default-threading-profile", new ThreadingProfileDefinitionParser());
        registerBeanDefinitionParser("default-dispatcher-connection-strategy", new ConnectionStrategyDefinitionParser());
        registerBeanDefinitionParser("default-receiver-connection-strategy", new ConnectionStrategyDefinitionParser());

        //registerBeanDefinitionParser("mule-configuration", new ManagementContextDefinitionParser());
        registerBeanDefinitionParser("threading-profile", new ThreadingProfileDefinitionParser());
        registerBeanDefinitionParser("custom-exception-strategy", new SimpleChildDefinitionParser("exceptionListener", null));
        registerBeanDefinitionParser("default-component-exception-strategy", new SimpleChildDefinitionParser("exceptionListener", DefaultComponentExceptionStrategy.class));
        registerBeanDefinitionParser("default-connector-exception-strategy", new SimpleChildDefinitionParser("exceptionListener", DefaultExceptionStrategy.class));
        registerBeanDefinitionParser("pooling-profile", new PoolingProfileDefinitionParser());
        registerBeanDefinitionParser("queue-profile", new SimpleChildDefinitionParser("queueProfile", QueueProfile.class));

        //Connector elements
        registerBeanDefinitionParser("dispatcher-threading-profile", new ThreadingProfileDefinitionParser());
        registerBeanDefinitionParser("receiver-threading-profile", new ThreadingProfileDefinitionParser());
        registerBeanDefinitionParser("dispatcher-connection-straqtegy", new ConnectionStrategyDefinitionParser());
        registerBeanDefinitionParser("receiver-connection-straqtegy", new ConnectionStrategyDefinitionParser());
        registerBeanDefinitionParser("service-overrides", new ServiceOverridesDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new CustomElementDefinitionParser(true));

        //Transformer elements
        registerBeanDefinitionParser("custom-transformer", new CustomElementDefinitionParser(false));
        registerBeanDefinitionParser("transformer-no-action", new SingleElementDefinitionParser(NoActionTransformer.class, false));

        registerBeanDefinitionParser("transformer-base64-encoder", new SingleElementDefinitionParser(Base64Encoder.class, false));
        registerBeanDefinitionParser("transformer-base64-decoder", new SingleElementDefinitionParser(Base64Decoder.class, false));

        registerBeanDefinitionParser("transformer-xml-entity-encoder", new SingleElementDefinitionParser(XmlEntityEncoder.class, false));
        registerBeanDefinitionParser("transformer-xml-entity-decoder", new SingleElementDefinitionParser(XmlEntityDecoder.class, false));
        registerBeanDefinitionParser("transformer-gzip-compress", new SingleElementDefinitionParser(GZipCompressTransformer.class, false));
        registerBeanDefinitionParser("transformer-gzip-uncompress", new SingleElementDefinitionParser(GZipUncompressTransformer.class, false));
        registerBeanDefinitionParser("transformer-encrypt", new SingleElementDefinitionParser(EncryptionTransformer.class, false));
        registerBeanDefinitionParser("transformer-decrypt", new SingleElementDefinitionParser(DecryptionTransformer.class, false));
        registerBeanDefinitionParser("transformer-byte-array-to-hex-string", new SingleElementDefinitionParser(ByteArrayToHexString.class, false));
        registerBeanDefinitionParser("transformer-hex-sting-to-byte-array", new SingleElementDefinitionParser(HexStringToByteArray.class, false));

        registerBeanDefinitionParser("transformer-byte-array-to-object", new SingleElementDefinitionParser(ByteArrayToObject.class, false));
        registerBeanDefinitionParser("transformer-object-to-byte-array", new SingleElementDefinitionParser(ObjectToByteArray.class, false));
        registerBeanDefinitionParser("transformer-byte-array-to-serializable", new SingleElementDefinitionParser(ByteArrayToSerializable.class, false));
        registerBeanDefinitionParser("transformer-serializable-to-byte-array", new SingleElementDefinitionParser(SerializableToByteArray.class, false));

        //Transaction Managers
        //TODO RM*: Better implementation of transaction manager properties
        registerBeanDefinitionParser("jndi-transaction-manager", new SingleElementDefinitionParser(GenericTransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("weblogic-transaction-manager", new SingleElementDefinitionParser(WeblogicTransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("jboss-transaction-manager", new SingleElementDefinitionParser(JBossTransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("jrun-transaction-manager", new SingleElementDefinitionParser(JRunTransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("resin3-transaction-manager", new SingleElementDefinitionParser(Resin3TransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("websphere-transaction-manager", new SingleElementDefinitionParser(WebsphereTransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("custom-transaction-manager-factory", new CustomElementDefinitionParser(true));

        //Endpoint elements
        registerBeanDefinitionParser("endpoint", new EndpointDefinitionParser());
        registerBeanDefinitionParser("endpoint-ref", new EndpointRefDefinitionParser());
        registerBeanDefinitionParser("transaction", new TransactionConfigDefinitionParser());

        //Container contexts
        registerBeanDefinitionParser("custom-container", new CustomElementDefinitionParser(true));
        registerBeanDefinitionParser("rmi-container", new SingleElementDefinitionParser(RmiContainerContext.class, true));
        registerBeanDefinitionParser("jndi-container", new SingleElementDefinitionParser(JndiContainerContext.class, true));
        registerBeanDefinitionParser("properties-container", new SingleElementDefinitionParser(PropertiesContainerContext.class, true));

        //Model Elements
        registerBeanDefinitionParser("model-seda", new SingleElementDefinitionParser(SedaModel.class, true));
        registerBeanDefinitionParser("model-inherited", new InheritedModelDefinitionParser());
        registerBeanDefinitionParser("model-seda-optimised", new SingleElementDefinitionParser(OptimisedSedaModel.class, true));
        registerBeanDefinitionParser("model-simple", new SingleElementDefinitionParser(DirectModel.class, true));
        registerBeanDefinitionParser("model-pipeline", new SingleElementDefinitionParser(PipelineModel.class, true));
        registerBeanDefinitionParser("model-streaming", new SingleElementDefinitionParser(StreamingModel.class, true));
        registerBeanDefinitionParser("model-custom", new CustomElementDefinitionParser(true));

        registerBeanDefinitionParser("component-lifecycle-adapter-factory", new SimpleChildDefinitionParser("lifecycleAdapterFactory", null));
        registerBeanDefinitionParser("callable-entrypoint-resolver", new SimpleChildDefinitionParser("entryPointResolver", CallableEntryPointResolver.class));
        registerBeanDefinitionParser("custom-entrypoint-resolver", new SimpleChildDefinitionParser("entryPointResolver", null));
        //registerBeanDefinitionParser("method-entrypoint-resolver", new SimpleChildDefinitionParser("entrypointResolver", MethodEntryPointResolver.class));
        //registerBeanDefinitionParser("reflection-entrypoint-resolver", new SimpleChildDefinitionParser("entrypointResolver", MethodEntryPointResolver.class));
        //registerBeanDefinitionParser("non-void-entrypoint-resolver", new SimpleChildDefinitionParser("entrypointResolver", NonVoidEntryPointResolver.class));

        //Service Elements
        registerBeanDefinitionParser("service", new ServiceDescriptorDefinitionParser());
        registerBeanDefinitionParser("inbound-router", new SimpleChildDefinitionParser("inboundRouter", InboundRouterCollection.class));
        registerBeanDefinitionParser("outbound-router", new SimpleChildDefinitionParser("outboundRouter", OutboundRouterCollection.class));
        registerBeanDefinitionParser("nested-router", new SimpleChildDefinitionParser("nestedRouter", NestedRouterCollection.class));
        registerBeanDefinitionParser("response-router", new SimpleChildDefinitionParser("responseRouter", ResponseRouterCollection.class));

        //Inbound Routers
        registerBeanDefinitionParser("pass-through-router", new RouterDefinitionParser("router", InboundPassThroughRouter.class));
        registerBeanDefinitionParser("idempotent-receiver-router", new RouterDefinitionParser("router", IdempotentReceiver.class));
        registerBeanDefinitionParser("idempotent-secure-hash-receiver-router", new RouterDefinitionParser("router", IdempotentSecureHashReceiver.class));
        registerBeanDefinitionParser("selective-consumer-router", new RouterDefinitionParser("router", SelectiveConsumer.class));
        registerBeanDefinitionParser("wire-tap-router", new RouterDefinitionParser("router", WireTap.class));
        registerBeanDefinitionParser("correlation-aggregator-router", new RouterDefinitionParser("router", CorrelationAggregator.class));
        registerBeanDefinitionParser("message-chunking-aggregator-router", new RouterDefinitionParser("router", MessageChunkingAggregator.class));
        registerBeanDefinitionParser("correlation-resequencer-router", new RouterDefinitionParser("router", CorrelationEventResequencer.class));
        registerBeanDefinitionParser("custom-router", new RouterDefinitionParser("router", null));

        //Nested binding
        registerBeanDefinitionParser("binding", new RouterDefinitionParser("router", NestedRouter.class));

        //Outbound Routers
        registerBeanDefinitionParser("pass-through-router", new RouterDefinitionParser("router", OutboundPassThroughRouter.class));
        registerBeanDefinitionParser("filtering-router", new RouterDefinitionParser("router", FilteringOutboundRouter.class));
        registerBeanDefinitionParser("chaining-router", new RouterDefinitionParser("router", ChainingRouter.class));
        registerBeanDefinitionParser("endpoint-selector-router", new RouterDefinitionParser("router", EndpointSelector.class));
        registerBeanDefinitionParser("exception-based-router", new RouterDefinitionParser("router", ExceptionBasedRouter.class));
        registerBeanDefinitionParser("list-message-splitter-router", new RouterDefinitionParser("router", FilteringListMessageSplitter.class));
        registerBeanDefinitionParser("message-chunking-router", new RouterDefinitionParser("router", MessageChunkingRouter.class));
        registerBeanDefinitionParser("multicasting-router", new RouterDefinitionParser("router", MulticastingRouter.class));
        registerBeanDefinitionParser("static-recipient-list-router", new RouterDefinitionParser("router", StaticRecipientList.class));
        registerBeanDefinitionParser("template-endpoint-router", new RouterDefinitionParser("router", TemplateEndpointRouter.class));
        registerBeanDefinitionParser("custom-router", new RouterDefinitionParser("router", null));

        //Catch all Strategies
        registerBeanDefinitionParser("forwarding-catch-all-strategy", new SimpleChildDefinitionParser("catchAllStrategy", ForwardingCatchAllStrategy.class));
        registerBeanDefinitionParser("custom-catch-all-strategy", new SimpleChildDefinitionParser("catchAllStrategy", null));

        //Common Filters
        registerBeanDefinitionParser("and-filter", new FilterDefinitionParser());
        registerBeanDefinitionParser("or-filter", new FilterDefinitionParser());
        registerBeanDefinitionParser("not-filter", new FilterDefinitionParser());
        registerBeanDefinitionParser("regex-filter", new FilterDefinitionParser());
        registerBeanDefinitionParser("exception-type-filter", new FilterDefinitionParser());
        registerBeanDefinitionParser("message-property-filter", new FilterDefinitionParser());
        registerBeanDefinitionParser("payload-type-filter", new FilterDefinitionParser());
        registerBeanDefinitionParser("wildcard-filter", new FilterDefinitionParser());
        registerBeanDefinitionParser("equals-filter", new FilterDefinitionParser());

        // Utils / Standard Types
        registerBeanDefinitionParser("properties", new PropertiesBeanDefinitionParser("properties"));
        registerBeanDefinitionParser("jndi-provider-properties", new PropertiesBeanDefinitionParser("jndiProviderProperties"));
    }
}
