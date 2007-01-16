/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config.handlers;

import org.mule.extras.spring.config.parsers.ConfigurationDefinitionParser;
import org.mule.extras.spring.config.parsers.ConnectionStrategyDefinitionParser;
import org.mule.extras.spring.config.parsers.CustomElementDefinitionParser;
import org.mule.extras.spring.config.parsers.EndpointDefinitionParser;
import org.mule.extras.spring.config.parsers.EndpointRefDefinitionParser;
import org.mule.extras.spring.config.parsers.FilterDefinitionParser;
import org.mule.extras.spring.config.parsers.KnownTypeElementDefinitionParser;
import org.mule.extras.spring.config.parsers.ModelDefinitionParser;
import org.mule.extras.spring.config.parsers.PropertiesDefinitionParser;
import org.mule.extras.spring.config.parsers.RouterDefinitionParser;
import org.mule.extras.spring.config.parsers.ServiceDescriptorDefinitionParser;
import org.mule.extras.spring.config.parsers.ServiceOverridesDefinitionParser;
import org.mule.extras.spring.config.parsers.SimpleChildDefinitionParser;
import org.mule.extras.spring.config.parsers.SingleElementDefinitionParser;
import org.mule.extras.spring.config.parsers.SourceTypeDefinitionParser;
import org.mule.extras.spring.config.parsers.ThreadingProfileDefinitionParser;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.container.JndiContainerContext;
import org.mule.impl.container.PropertiesContainerContext;
import org.mule.impl.container.RmiContainerContext;
import org.mule.model.CallableEntryPointResolver;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.inbound.CorrelationAggregator;
import org.mule.routing.inbound.CorrelationEventResequencer;
import org.mule.routing.inbound.IdempotentReceiver;
import org.mule.routing.inbound.IdempotentSecureHashReceiver;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.routing.inbound.InboundPassThroughRouter;
import org.mule.routing.inbound.MessageChunkingAggregator;
import org.mule.routing.inbound.SelectiveConsumer;
import org.mule.routing.inbound.WireTap;
import org.mule.routing.outbound.ChainingRouter;
import org.mule.routing.outbound.EndpointSelector;
import org.mule.routing.outbound.ExceptionBasedRouter;
import org.mule.routing.outbound.FilteringListMessageSplitter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.routing.outbound.MessageChunkingRouter;
import org.mule.routing.outbound.MulticastingRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.routing.outbound.TemplateEndpointRouter;
import org.mule.routing.response.ResponseMessageRouter;
import org.mule.transformers.NoActionTransformer;
import org.mule.transformers.codec.Base64Decoder;
import org.mule.transformers.codec.Base64Encoder;
import org.mule.transformers.codec.UCDecoder;
import org.mule.transformers.codec.UCEncoder;
import org.mule.transformers.codec.UUDecoder;
import org.mule.transformers.codec.UUEncoder;
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

/**
 * todo document
 *
 */
public class MuleNamespaceHandler extends AbstractHierarchicalNamespaceHandler
{

    public void init()
    {
        //Common elements
        registerBeanDefinitionParser("configuration", new ConfigurationDefinitionParser());
        registerBeanDefinitionParser("default-threading-profile", new ThreadingProfileDefinitionParser());
        registerBeanDefinitionParser("default-dispatcher-connection-straqtegy", new ConnectionStrategyDefinitionParser());
        registerBeanDefinitionParser("default-receiver-connection-straqtegy", new ConnectionStrategyDefinitionParser());
        registerBeanDefinitionParser("properties", new PropertiesDefinitionParser());

        //registerBeanDefinitionParser("mule-configuration", new ManagementContextDefinitionParser());
        registerBeanDefinitionParser("threading-profile", new ThreadingProfileDefinitionParser());
        //TODO RM* remove confusion between exceptionListener and ExceptionStrategy
        registerBeanDefinitionParser("custom-exception-strategy", new SimpleChildDefinitionParser("exceptionListener", null));
        registerBeanDefinitionParser("default-component-exception-strategy", new SimpleChildDefinitionParser("exceptionListener", DefaultComponentExceptionStrategy.class));
        registerBeanDefinitionParser("default-connector-exception-strategy", new SimpleChildDefinitionParser("exceptionListener", DefaultExceptionStrategy.class));

        //Connector elements
        registerBeanDefinitionParser("dispatcher-threading-profile", new ThreadingProfileDefinitionParser());
        registerBeanDefinitionParser("receiver-threading-profile", new ThreadingProfileDefinitionParser());
        registerBeanDefinitionParser("dispatcher-connection-straqtegy", new ConnectionStrategyDefinitionParser());
        registerBeanDefinitionParser("receiver-connection-straqtegy", new ConnectionStrategyDefinitionParser());
        registerBeanDefinitionParser("service-overrides", new ServiceOverridesDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new CustomElementDefinitionParser());

        //Transformer elements
        registerBeanDefinitionParser("custom-transformer", new CustomElementDefinitionParser());
        registerBeanDefinitionParser("transformer-no-action", new SingleElementDefinitionParser(NoActionTransformer.class));
        registerBeanDefinitionParser("source-type", new SourceTypeDefinitionParser());

        registerBeanDefinitionParser("transformer-base64-encoder", new SingleElementDefinitionParser(Base64Encoder.class));
        registerBeanDefinitionParser("transformer-base64-decoder", new SingleElementDefinitionParser(Base64Decoder.class));
        registerBeanDefinitionParser("transformer-uc-encoder", new SingleElementDefinitionParser(UCEncoder.class));
        registerBeanDefinitionParser("transformer-uc-decoder", new SingleElementDefinitionParser(UCDecoder.class));
        registerBeanDefinitionParser("transformer-uu-encoder", new SingleElementDefinitionParser(UUEncoder.class));
        registerBeanDefinitionParser("transformer-uu-decoder", new SingleElementDefinitionParser(UUDecoder.class));

        registerBeanDefinitionParser("transformer-xml-entity-encoder", new SingleElementDefinitionParser(XmlEntityEncoder.class));
        registerBeanDefinitionParser("transformer-xml-entity-decoder", new SingleElementDefinitionParser(XmlEntityDecoder.class));
        registerBeanDefinitionParser("transformer-gzip-compress", new SingleElementDefinitionParser(GZipCompressTransformer.class));
        registerBeanDefinitionParser("transformer-gzip-uncompress", new SingleElementDefinitionParser(GZipUncompressTransformer.class));
        registerBeanDefinitionParser("transformer-encrypt", new SingleElementDefinitionParser(EncryptionTransformer.class));
        registerBeanDefinitionParser("transformer-decrypt", new SingleElementDefinitionParser(DecryptionTransformer.class));
        registerBeanDefinitionParser("transformer-byte-array-to-hex-string", new SingleElementDefinitionParser(ByteArrayToHexString.class));
        registerBeanDefinitionParser("transformer-hex-sting-to-byte-array", new SingleElementDefinitionParser(HexStringToByteArray.class));

        registerBeanDefinitionParser("transformer-byte-array-to-object", new SingleElementDefinitionParser(ByteArrayToObject.class));
        registerBeanDefinitionParser("transformer-object-to-byte-array", new SingleElementDefinitionParser(ObjectToByteArray.class));
        registerBeanDefinitionParser("transformer-byte-array-to-serializable", new SingleElementDefinitionParser(ByteArrayToSerializable.class));
        registerBeanDefinitionParser("transformer-serializable-to-byte-array", new SingleElementDefinitionParser(SerializableToByteArray.class));

        //Endpoint elements
        registerBeanDefinitionParser("endpoint", new EndpointDefinitionParser());
        registerBeanDefinitionParser("endpoint-ref", new EndpointRefDefinitionParser());

        //Container contexts
        registerBeanDefinitionParser("custom-container", new CustomElementDefinitionParser());
        registerBeanDefinitionParser("rmi-container", new KnownTypeElementDefinitionParser(RmiContainerContext.class));
        registerBeanDefinitionParser("jndi-container", new KnownTypeElementDefinitionParser(JndiContainerContext.class));
        registerBeanDefinitionParser("properties-container", new KnownTypeElementDefinitionParser(PropertiesContainerContext.class));

        //Model Elements
        registerBeanDefinitionParser("model-seda", new ModelDefinitionParser("seda"));
        registerBeanDefinitionParser("model-seda-optimised", new ModelDefinitionParser("seda-optimised"));
        registerBeanDefinitionParser("model-simple", new ModelDefinitionParser("simple"));
        registerBeanDefinitionParser("model-pipeline", new ModelDefinitionParser("pipeline"));
        registerBeanDefinitionParser("custom-model", new ModelDefinitionParser("custom"));

        registerBeanDefinitionParser("custom-lifecycle-adaptor", new SimpleChildDefinitionParser("lifecycleAdapater", null));
        registerBeanDefinitionParser("callable-entrypoint-resolver", new SimpleChildDefinitionParser("entryPointResolver", CallableEntryPointResolver.class));
        registerBeanDefinitionParser("custom-entrypoint-resolver", new SimpleChildDefinitionParser("entryPointResolver", null));
        //registerBeanDefinitionParser("method-entrypoint-resolver", new SimpleChildDefinitionParser("entrypointResolver", MethodEntryPointResolver.class));
        //registerBeanDefinitionParser("reflection-entrypoint-resolver", new SimpleChildDefinitionParser("entrypointResolver", MethodEntryPointResolver.class));
        //registerBeanDefinitionParser("non-void-entrypoint-resolver", new SimpleChildDefinitionParser("entrypointResolver", NonVoidEntryPointResolver.class));

        //Service Elements
        registerBeanDefinitionParser("service", new ServiceDescriptorDefinitionParser());
        registerBeanDefinitionParser("inbound-router", new SimpleChildDefinitionParser("inboundRouter", InboundMessageRouter.class));
        registerBeanDefinitionParser("outbound-router", new SimpleChildDefinitionParser("outboundRouter", OutboundMessageRouter.class));
        registerBeanDefinitionParser("response-router", new SimpleChildDefinitionParser("responseRouter", ResponseMessageRouter.class));

        //Inbound Routers
        //TODO RM*
        registerBeanDefinitionParser("pass-through-router", new RouterDefinitionParser("router", InboundPassThroughRouter.class));
        registerBeanDefinitionParser("idempotent-receiver-router", new RouterDefinitionParser("router", IdempotentReceiver.class));
        registerBeanDefinitionParser("idempotent-secure-hash-receiver-router", new RouterDefinitionParser("router", IdempotentSecureHashReceiver.class));
        registerBeanDefinitionParser("selective-consumer-router", new RouterDefinitionParser("router", SelectiveConsumer.class));
        registerBeanDefinitionParser("wire-tap-router", new RouterDefinitionParser("router", WireTap.class));
        registerBeanDefinitionParser("correlation-aggregator-router", new RouterDefinitionParser("router", CorrelationAggregator.class));
        registerBeanDefinitionParser("message-chunking-aggregator-router", new RouterDefinitionParser("router", MessageChunkingAggregator.class));
        registerBeanDefinitionParser("correlation-resequencer-router", new RouterDefinitionParser("router", CorrelationEventResequencer.class));
        registerBeanDefinitionParser("custom-router", new RouterDefinitionParser("router", null));


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

    }
}
