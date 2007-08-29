/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.components.simple.BridgeComponent;
import org.mule.components.simple.EchoComponent;
import org.mule.components.simple.LogComponent;
import org.mule.components.simple.NullComponent;
import org.mule.config.MuleProperties;
import org.mule.config.QueueProfile;
import org.mule.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.config.spring.parsers.collection.OrphanMapDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.InheritDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.generic.NameTransferDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.ConfigurationDefinitionParser;
import org.mule.config.spring.parsers.specific.ConnectionStrategyDefinitionParser;
import org.mule.config.spring.parsers.specific.EndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.config.spring.parsers.specific.PoolingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceDescriptorDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceOverridesDefinitionParser;
import org.mule.config.spring.parsers.specific.SimpleComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.ThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionConfigDefinitionParser;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.container.JndiContainerContext;
import org.mule.impl.container.PropertiesContainerContext;
import org.mule.impl.container.RmiContainerContext;
import org.mule.impl.model.resolvers.CallableEntryPointResolver;
import org.mule.impl.model.seda.SedaModel;
import org.mule.impl.security.PasswordBasedEncryptionStrategy;
import org.mule.impl.security.SecretKeyEncryptionStrategy;
import org.mule.impl.security.filters.MuleEncryptionEndpointSecurityFilter;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.routing.CorrelationPropertiesExtractor;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.ExceptionTypeFilter;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.filters.logic.OrFilter;
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
import org.mule.transformers.simple.ByteArrayToString;
import org.mule.transformers.simple.HexStringToByteArray;
import org.mule.transformers.simple.ObjectToByteArray;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.transformers.simple.StringToByteArray;
import org.mule.util.properties.BeanPropertyExtractor;
import org.mule.util.properties.MapPropertyExtractor;
import org.mule.util.properties.MessagePropertyExtractor;
import org.mule.util.properties.PayloadPropertyExtractor;

import java.util.HashMap;

/**
 * This is the core namespace handler for Mule and configures all Mule configuration elements under the
 * <code>http://www.mulesource.org/schema/mule/core/2.0</code> Namespace.
 */
public class MuleNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        registerIgnoredElement("mule");
        registerIgnoredElement("other");
        registerIgnoredElement("description");

        //Common elements
        registerBeanDefinitionParser("configuration", new ConfigurationDefinitionParser());
        registerBeanDefinitionParser("environment-properties", new OrphanMapDefinitionParser(HashMap.class, MuleProperties.OBJECT_MULE_APPLICATION_PROPERTIES));
        registerBeanDefinitionParser("default-threading-profile", new ThreadingProfileDefinitionParser("defaultThreadingProfile"));
        registerBeanDefinitionParser("default-dispatcher-threading-profile", new ThreadingProfileDefinitionParser("defaultMessageDispatcherThreadingProfile"));
        registerBeanDefinitionParser("default-receiver-threading-profile", new ThreadingProfileDefinitionParser("defaultMessageReceiverThreadingProfile"));
        registerBeanDefinitionParser("default-dispatcher-connection-strategy", new ConnectionStrategyDefinitionParser());
        registerBeanDefinitionParser("default-receiver-connection-strategy", new ConnectionStrategyDefinitionParser());

        //registerBeanDefinitionParser("mule-configuration", new ManagementContextDefinitionParser());
        registerBeanDefinitionParser("threading-profile", new ThreadingProfileDefinitionParser("threadingProfile"));
        registerBeanDefinitionParser("custom-exception-strategy", new ChildDefinitionParser("exceptionListener", null));
        registerBeanDefinitionParser("default-component-exception-strategy", new ChildDefinitionParser("exceptionListener", DefaultComponentExceptionStrategy.class));
        registerBeanDefinitionParser("default-connector-exception-strategy", new ChildDefinitionParser("exceptionListener", DefaultExceptionStrategy.class));
        registerBeanDefinitionParser("pooling-profile", new PoolingProfileDefinitionParser());
        registerBeanDefinitionParser("queue-profile", new ChildDefinitionParser("queueProfile", QueueProfile.class));

        //Connector elements
        registerBeanDefinitionParser("dispatcher-threading-profile", new ThreadingProfileDefinitionParser("dispatcherThreadingProfile"));
        registerBeanDefinitionParser("receiver-threading-profile", new ThreadingProfileDefinitionParser("receiverThreadingProfile"));
        registerBeanDefinitionParser("dispatcher-connection-straqtegy", new ConnectionStrategyDefinitionParser("dispatcherConnectionStrategy"));
        registerBeanDefinitionParser("receiver-connection-straqtegy", new ConnectionStrategyDefinitionParser("receiverConnectionStrategy"));
        registerBeanDefinitionParser("service-overrides", new ServiceOverridesDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new MuleChildDefinitionParser(true));

        //Transformer elements
        registerBeanDefinitionParser("custom-transformer", new MuleChildDefinitionParser(false));
        registerBeanDefinitionParser("transformer-no-action", new MuleChildDefinitionParser(NoActionTransformer.class, false));

        registerBeanDefinitionParser("transformer-base64-encoder", new MuleChildDefinitionParser(Base64Encoder.class, false));
        registerBeanDefinitionParser("transformer-base64-decoder", new MuleChildDefinitionParser(Base64Decoder.class, false));

        registerBeanDefinitionParser("transformer-xml-entity-encoder", new MuleChildDefinitionParser(XmlEntityEncoder.class, false));
        registerBeanDefinitionParser("transformer-xml-entity-decoder", new MuleChildDefinitionParser(XmlEntityDecoder.class, false));
        registerBeanDefinitionParser("transformer-gzip-compress", new MuleChildDefinitionParser(GZipCompressTransformer.class, false));
        registerBeanDefinitionParser("transformer-gzip-uncompress", new MuleChildDefinitionParser(GZipUncompressTransformer.class, false));
        registerBeanDefinitionParser("transformer-encrypt", new MuleChildDefinitionParser(EncryptionTransformer.class, false));
        registerBeanDefinitionParser("transformer-decrypt", new MuleChildDefinitionParser(DecryptionTransformer.class, false));
        registerBeanDefinitionParser("transformer-byte-array-to-hex-string", new MuleChildDefinitionParser(ByteArrayToHexString.class, false));
        registerBeanDefinitionParser("transformer-hex-sting-to-byte-array", new MuleChildDefinitionParser(HexStringToByteArray.class, false));

        registerBeanDefinitionParser("transformer-byte-array-to-object", new MuleChildDefinitionParser(ByteArrayToObject.class, false));
        registerBeanDefinitionParser("transformer-object-to-byte-array", new MuleChildDefinitionParser(ObjectToByteArray.class, false));
        registerBeanDefinitionParser("transformer-byte-array-to-serializable", new MuleChildDefinitionParser(ByteArrayToSerializable.class, false));
        registerBeanDefinitionParser("transformer-serializable-to-byte-array", new MuleChildDefinitionParser(SerializableToByteArray.class, false));
        registerBeanDefinitionParser("transformer-byte-array-to-string", new MuleChildDefinitionParser(ByteArrayToString.class, false));
        registerBeanDefinitionParser("transformer-string-to-byte-array", new MuleChildDefinitionParser(StringToByteArray.class, false));

        //Transaction Managers
        //TODO RM*: Need to review these, since Spring have some facilities for configuring the transactionManager
        registerBeanDefinitionParser("custom-transaction-manager", new MuleChildDefinitionParser(true));
        registerBeanDefinitionParser("transaction-manager-jndi", new MuleChildDefinitionParser(GenericTransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("transaction-manager-weblogic", new MuleChildDefinitionParser(WeblogicTransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("transaction-manager-jboss", new MuleChildDefinitionParser(JBossTransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("transaction-manager-jrun", new MuleChildDefinitionParser(JRunTransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("transaction-manager-resin", new MuleChildDefinitionParser(Resin3TransactionManagerLookupFactory.class, true));
        registerBeanDefinitionParser("transaction-manager-websphere", new MuleChildDefinitionParser(WebsphereTransactionManagerLookupFactory.class, true));

        //Endpoint elements
        registerBeanDefinitionParser("endpoint", new EndpointDefinitionParser());
        registerBeanDefinitionParser("transaction", new TransactionConfigDefinitionParser());
        registerBeanDefinitionParser("transaction-factory", new ObjectFactoryDefinitionParser("factory"));

        //Container contexts
        registerBeanDefinitionParser("custom-container", new MuleChildDefinitionParser(true));
        registerBeanDefinitionParser("rmi-container", new MuleChildDefinitionParser(RmiContainerContext.class, true));
        registerBeanDefinitionParser("jndi-container", new MuleChildDefinitionParser(JndiContainerContext.class, true));
        registerBeanDefinitionParser("properties-container", new MuleChildDefinitionParser(PropertiesContainerContext.class, true));

        //Model Elements
        registerBeanDefinitionParser("model", new InheritDefinitionParser(new OrphanDefinitionParser(SedaModel.class, true), new NamedDefinitionParser()));
//        registerBeanDefinitionParser("model-seda-optimised", new OrphanDefinitionParser(OptimisedSedaModel.class, true));
//        registerBeanDefinitionParser("model-pipeline", new OrphanDefinitionParser(PipelineModel.class, true));

        registerBeanDefinitionParser("callable-entrypoint-resolver", new ChildDefinitionParser("entryPointResolver", CallableEntryPointResolver.class));
        registerBeanDefinitionParser("custom-entrypoint-resolver", new ChildDefinitionParser("entryPointResolver", null));
        //registerBeanDefinitionParser("method-entrypoint-resolver", new ChildDefinitionParser("entrypointResolver", MethodEntryPointResolver.class));
        //registerBeanDefinitionParser("reflection-entrypoint-resolver", new ChildDefinitionParser("entrypointResolver", MethodEntryPointResolver.class));
        //registerBeanDefinitionParser("non-void-entrypoint-resolver", new ChildDefinitionParser("entrypointResolver", NonVoidEntryPointResolver.class));

        //Service Elements
        registerBeanDefinitionParser("service", new ServiceDescriptorDefinitionParser());
        registerBeanDefinitionParser("component", new ComponentDefinitionParser("serviceFactory"));
        registerBeanDefinitionParser("bridge-component", new SimpleComponentDefinitionParser("serviceFactory", BridgeComponent.class));
        registerBeanDefinitionParser("log-component", new SimpleComponentDefinitionParser("serviceFactory", LogComponent.class));
        registerBeanDefinitionParser("echo-component", new SimpleComponentDefinitionParser("serviceFactory", EchoComponent.class));
        registerBeanDefinitionParser("null-component", new SimpleComponentDefinitionParser("serviceFactory", NullComponent.class));
        registerBeanDefinitionParser("inbound-router", new ChildDefinitionParser("inboundRouter", InboundRouterCollection.class));
        registerBeanDefinitionParser("outbound-router", new ChildDefinitionParser("outboundRouter", OutboundRouterCollection.class));
        registerBeanDefinitionParser("nested-router", new ChildDefinitionParser("nestedRouter", NestedRouterCollection.class));
        registerBeanDefinitionParser("response-router", new ChildDefinitionParser("responseRouter", ResponseRouterCollection.class));

        //Inbound Routers
        registerBeanDefinitionParser("inbound-pass-through-router", new RouterDefinitionParser("router", InboundPassThroughRouter.class));
        registerBeanDefinitionParser("idempotent-receiver-router", new RouterDefinitionParser("router", IdempotentReceiver.class));
        registerBeanDefinitionParser("idempotent-secure-hash-receiver-router", new RouterDefinitionParser("router", IdempotentSecureHashReceiver.class));
        registerBeanDefinitionParser("selective-consumer-router", new RouterDefinitionParser("router", SelectiveConsumer.class));
        registerBeanDefinitionParser("wire-tap-router", new RouterDefinitionParser("router", WireTap.class));
        registerBeanDefinitionParser("correlation-aggregator-router", new RouterDefinitionParser("router", CorrelationAggregator.class));
        registerBeanDefinitionParser("message-chunking-aggregator-router", new RouterDefinitionParser("router", MessageChunkingAggregator.class));
        registerBeanDefinitionParser("correlation-resequencer-router", new RouterDefinitionParser("router", CorrelationEventResequencer.class));
        registerBeanDefinitionParser("custom-inbound-router", new RouterDefinitionParser("router", null));

        //Nested binding
        registerBeanDefinitionParser("binding", new RouterDefinitionParser("router", NestedRouter.class));

        //Outbound Routers
        registerBeanDefinitionParser("outbound-pass-through-router", new RouterDefinitionParser("router", OutboundPassThroughRouter.class));
        registerBeanDefinitionParser("filtering-router", new RouterDefinitionParser("router", FilteringOutboundRouter.class));
        registerBeanDefinitionParser("chaining-router", new RouterDefinitionParser("router", ChainingRouter.class));
        registerBeanDefinitionParser("endpoint-selector-router", new RouterDefinitionParser("router", EndpointSelector.class));
        registerBeanDefinitionParser("exception-based-router", new RouterDefinitionParser("router", ExceptionBasedRouter.class));
        registerBeanDefinitionParser("list-message-splitter-router", new RouterDefinitionParser("router", FilteringListMessageSplitter.class));
        registerBeanDefinitionParser("message-chunking-router", new RouterDefinitionParser("router", MessageChunkingRouter.class));
        registerBeanDefinitionParser("multicasting-router", new RouterDefinitionParser("router", MulticastingRouter.class));
        registerBeanDefinitionParser("static-recipient-list-router", new RouterDefinitionParser("router", StaticRecipientList.class));
        registerBeanDefinitionParser("template-endpoint-router", new RouterDefinitionParser("router", TemplateEndpointRouter.class));
        registerBeanDefinitionParser("custom-outbound-router", new RouterDefinitionParser("router", null));
        registerBeanDefinitionParser("reply-to", new ParentDefinitionParser().addAlias("address", "replyTo"));

        //Response Routers
        registerBeanDefinitionParser("custom-response-router", new RouterDefinitionParser("router", null));

        //Property Extractors
        registerBeanDefinitionParser("bean-property-extractor", new ChildDefinitionParser("propertyExtractor", BeanPropertyExtractor.class));
        registerBeanDefinitionParser("correlation-property-extractor", new ChildDefinitionParser("propertyExtractor", CorrelationPropertiesExtractor.class));
        registerBeanDefinitionParser("custom-property-extractor", new ObjectFactoryDefinitionParser("propertyExtractor"));
        registerBeanDefinitionParser("map-property-extractor", new ChildDefinitionParser("propertyExtractor", MapPropertyExtractor.class));
        registerBeanDefinitionParser("message-property-extractor", new ChildDefinitionParser("propertyExtractor", MessagePropertyExtractor.class));
        registerBeanDefinitionParser("payload-property-extractor", new ChildDefinitionParser("propertyExtractor", PayloadPropertyExtractor.class));

        //Catch all Strategies
        registerBeanDefinitionParser("forwarding-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", ForwardingCatchAllStrategy.class));
        registerBeanDefinitionParser("custom-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", null));
        registerBeanDefinitionParser("logging-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", LoggingCatchAllStrategy.class));

        //Common Filters
        registerBeanDefinitionParser("and-filter", new FilterDefinitionParser(AndFilter.class));
        registerBeanDefinitionParser("or-filter", new FilterDefinitionParser(OrFilter.class));
        registerBeanDefinitionParser("not-filter", new FilterDefinitionParser(NotFilter.class));
        registerBeanDefinitionParser("regex-filter", new FilterDefinitionParser(RegExFilter.class));
        registerBeanDefinitionParser("exception-type-filter", new FilterDefinitionParser(ExceptionTypeFilter.class));
        registerBeanDefinitionParser("message-property-filter", new FilterDefinitionParser(MessagePropertyFilter.class));
        registerBeanDefinitionParser("payload-type-filter", new FilterDefinitionParser(PayloadTypeFilter.class));
        registerBeanDefinitionParser("wildcard-filter", new FilterDefinitionParser(WildcardFilter.class));
        registerBeanDefinitionParser("equals-filter", new FilterDefinitionParser(EqualsFilter.class));
        registerBeanDefinitionParser("custom-filter", new FilterDefinitionParser());

        //Retry strategies
        registerBeanDefinitionParser("retry-connection-strategy", new ChildDefinitionParser("connectionStrategy", SimpleRetryConnectionStrategy.class));

        //Utils / Standard Types
        registerBeanDefinitionParser("properties", new ChildMapDefinitionParser("properties"));
        registerBeanDefinitionParser("meta-info", new ChildMapDefinitionParser("properties"));
        registerBeanDefinitionParser("jndi-provider-properties", new ChildMapDefinitionParser("jndiProviderProperties"));
        registerBeanDefinitionParser("environment", new ChildMapDefinitionParser("environment"));

        //Security
        registerBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER).addIgnored("type"));
        registerBeanDefinitionParser("custom-security-provider", new NameTransferDefinitionParser("providers"));
        registerBeanDefinitionParser("custom-encryption-strategy", new NameTransferDefinitionParser("encryptionStrategies").addAlias("strategy", "encryptionStrategy"));
        registerBeanDefinitionParser("password-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", PasswordBasedEncryptionStrategy.class));
        registerBeanDefinitionParser("secret-key-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", SecretKeyEncryptionStrategy.class));
        registerBeanDefinitionParser("encryption-security-filter", new ChildDefinitionParser("securityFilter", MuleEncryptionEndpointSecurityFilter.class));
    }
}
