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

import org.mule.components.simple.EchoComponent;
import org.mule.components.simple.LogComponent;
import org.mule.components.simple.NoArgsCallWrapper;
import org.mule.components.simple.NullComponent;
import org.mule.components.simple.PassThroughComponent;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.QueueProfile;
import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.factories.ResponseEndpointFactoryBean;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.collection.AttributeMapDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildListDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.config.spring.parsers.delegate.InheritDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.NameTransferDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.ConfigurationDefinitionParser;
import org.mule.config.spring.parsers.specific.ConnectionStrategyDefinitionParser;
import org.mule.config.spring.parsers.specific.DefaultThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.EnvironmentPropertyDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.ForwardingRouterDefinitionParser;
import org.mule.config.spring.parsers.specific.MuleAdminAgentDefinitionParser;
import org.mule.config.spring.parsers.specific.PassThroughComponentAdapter;
import org.mule.config.spring.parsers.specific.PojoServiceDefinitionParser;
import org.mule.config.spring.parsers.specific.PoolingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceOverridesDefinitionParser;
import org.mule.config.spring.parsers.specific.SimplePojoServiceDefinitionParser;
import org.mule.config.spring.parsers.specific.ThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionConfigDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionFactoryDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionManagerDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.GenericEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.container.JndiContainerContext;
import org.mule.impl.container.PropertiesContainerContext;
import org.mule.impl.container.RmiContainerContext;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.impl.model.seda.SedaComponent;
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
import org.mule.routing.response.SingleResponseRouter;
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
import org.mule.transformers.simple.MessagePropertiesTransformer;
import org.mule.transformers.simple.ObjectToByteArray;
import org.mule.transformers.simple.ObjectToString;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.transformers.simple.StringAppendTransformer;
import org.mule.util.object.PooledObjectFactory;
import org.mule.util.object.PrototypeObjectFactory;
import org.mule.util.object.SingletonObjectFactory;
import org.mule.util.properties.FunctionPropertyExtractor;
import org.mule.util.properties.MapPayloadPropertyExtractor;
import org.mule.util.properties.MessageHeaderPropertyExtractor;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

/**
 * This is the core namespace handler for Mule and configures all Mule configuration elements under the
 * <code>http://www.mulesource.org/schema/mule/core/2.0</code> Namespace.
 */
public class MuleNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerIgnoredElement("mule");
        registerIgnoredElement("description");

        //Common elements
        registerBeanDefinitionParser("configuration", new ConfigurationDefinitionParser());
        registerBeanDefinitionParser("environment-property", new EnvironmentPropertyDefinitionParser());
        registerBeanDefinitionParser("admin-agent", new MuleAdminAgentDefinitionParser());
        registerBeanDefinitionParser("default-threading-profile", new DefaultThreadingProfileDefinitionParser(MuleConfiguration.DEFAULT_THREADING_PROFILE));
        registerBeanDefinitionParser("default-dispatcher-threading-profile", new DefaultThreadingProfileDefinitionParser(MuleConfiguration.DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE));
        registerBeanDefinitionParser("default-receiver-threading-profile", new DefaultThreadingProfileDefinitionParser(MuleConfiguration.DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE));
        registerBeanDefinitionParser("default-component-threading-profile", new DefaultThreadingProfileDefinitionParser(MuleConfiguration.DEFAULT_COMPONENT_THREADING_PROFILE));
        registerBeanDefinitionParser("default-dispatcher-connection-strategy", new ConnectionStrategyDefinitionParser());
        registerBeanDefinitionParser("default-receiver-connection-strategy", new ConnectionStrategyDefinitionParser());

        //registerBeanDefinitionParser("mule-configuration", new ManagementContextDefinitionParser());
        registerBeanDefinitionParser("component-threading-profile", new ThreadingProfileDefinitionParser("threadingProfile", MuleConfiguration.DEFAULT_COMPONENT_THREADING_PROFILE));
        registerBeanDefinitionParser("custom-exception-strategy", new ChildDefinitionParser("exceptionListener", null));
        registerBeanDefinitionParser("default-service-exception-strategy", new ChildDefinitionParser("exceptionListener", DefaultComponentExceptionStrategy.class));
        registerBeanDefinitionParser("default-connector-exception-strategy", new ChildDefinitionParser("exceptionListener", DefaultExceptionStrategy.class));
        registerBeanDefinitionParser("pooling-profile", new PoolingProfileDefinitionParser());
        registerBeanDefinitionParser("queue-profile", new ChildDefinitionParser("queueProfile", QueueProfile.class));

        //Connector elements
        registerBeanDefinitionParser("dispatcher-threading-profile", new ThreadingProfileDefinitionParser("dispatcherThreadingProfile", MuleConfiguration.DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE));
        registerBeanDefinitionParser("receiver-threading-profile", new ThreadingProfileDefinitionParser("receiverThreadingProfile", MuleConfiguration.DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE));
        registerBeanDefinitionParser("dispatcher-connection-strategy", new ConnectionStrategyDefinitionParser("dispatcherConnectionStrategy"));
        registerBeanDefinitionParser("receiver-connection-straqtegy", new ConnectionStrategyDefinitionParser("receiverConnectionStrategy"));
        registerBeanDefinitionParser("service-overrides", new ServiceOverridesDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new MuleOrphanDefinitionParser(true));

        //Transformer elements
        registerMuleDefinitionParser("transformer", new ParentDefinitionParser()).addAlias("ref", "transformer");

        registerBeanDefinitionParser("custom-transformer", new TransformerDefinitionParser());
        registerBeanDefinitionParser("transformer-no-action", new TransformerDefinitionParser(NoActionTransformer.class));
        registerBeanDefinitionParser("transformer-message-properties", new TransformerDefinitionParser(MessagePropertiesTransformer.class));

        registerBeanDefinitionParser("transformer-base64-encoder", new TransformerDefinitionParser(Base64Encoder.class));
        registerBeanDefinitionParser("transformer-base64-decoder", new TransformerDefinitionParser(Base64Decoder.class));

        registerBeanDefinitionParser("transformer-xml-entity-encoder", new TransformerDefinitionParser(XmlEntityEncoder.class));
        registerBeanDefinitionParser("transformer-xml-entity-decoder", new TransformerDefinitionParser(XmlEntityDecoder.class));
        registerBeanDefinitionParser("transformer-gzip-compress", new TransformerDefinitionParser(GZipCompressTransformer.class));
        registerBeanDefinitionParser("transformer-gzip-uncompress", new TransformerDefinitionParser(GZipUncompressTransformer.class));
        registerBeanDefinitionParser("transformer-encrypt", new TransformerDefinitionParser(EncryptionTransformer.class));
        registerBeanDefinitionParser("transformer-decrypt", new TransformerDefinitionParser(DecryptionTransformer.class));
        registerBeanDefinitionParser("transformer-byte-array-to-hex-string", new TransformerDefinitionParser(ByteArrayToHexString.class));
        registerBeanDefinitionParser("transformer-hex-sting-to-byte-array", new TransformerDefinitionParser(HexStringToByteArray.class));

        registerBeanDefinitionParser("transformer-byte-array-to-object", new TransformerDefinitionParser(ByteArrayToObject.class));
        registerBeanDefinitionParser("transformer-object-to-byte-array", new TransformerDefinitionParser(ObjectToByteArray.class));
        registerBeanDefinitionParser("transformer-object-to-string", new TransformerDefinitionParser(ObjectToString.class));
        registerBeanDefinitionParser("transformer-byte-array-to-serializable", new TransformerDefinitionParser(ByteArrayToSerializable.class));
        registerBeanDefinitionParser("transformer-serializable-to-byte-array", new TransformerDefinitionParser(SerializableToByteArray.class));
        registerBeanDefinitionParser("transformer-byte-array-to-string", new TransformerDefinitionParser(ObjectToString.class));
        registerBeanDefinitionParser("transformer-string-to-byte-array", new TransformerDefinitionParser(ObjectToByteArray.class));

        registerBeanDefinitionParser("transformer-append-string", new TransformerDefinitionParser(StringAppendTransformer.class));

        //Transaction Managers
        registerBeanDefinitionParser("custom-transaction-manager", new TransactionManagerDefinitionParser());
        registerBeanDefinitionParser("transaction-manager-jndi", new TransactionManagerDefinitionParser(GenericTransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("transaction-manager-weblogic", new TransactionManagerDefinitionParser(WeblogicTransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("transaction-manager-jboss", new TransactionManagerDefinitionParser(JBossTransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("transaction-manager-jrun", new TransactionManagerDefinitionParser(JRunTransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("transaction-manager-resin", new TransactionManagerDefinitionParser(Resin3TransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("transaction-manager-websphere", new TransactionManagerDefinitionParser(WebsphereTransactionManagerLookupFactory.class));

        //Endpoint elements
        registerBeanDefinitionParser("endpoint", new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
        registerBeanDefinitionParser("inbound-endpoint", new GenericEndpointDefinitionParser(InboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("outbound-endpoint", new GenericEndpointDefinitionParser(OutboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("response-endpoint", new GenericEndpointDefinitionParser(ResponseEndpointFactoryBean.class));
        registerBeanDefinitionParser("transaction", new TransactionConfigDefinitionParser());
        registerBeanDefinitionParser("custom-transaction-factory", new TransactionFactoryDefinitionParser());
        registerMuleDefinitionParser("transaction-factory", new ParentDefinitionParser()).addAlias("ref", "factory");

        //Container contexts
        registerBeanDefinitionParser("custom-container", new MuleOrphanDefinitionParser(true));
        registerBeanDefinitionParser("rmi-container", new MuleOrphanDefinitionParser(RmiContainerContext.class, true));
        registerBeanDefinitionParser("jndi-container", new MuleOrphanDefinitionParser(JndiContainerContext.class, true));
        registerBeanDefinitionParser("properties-container", new MuleOrphanDefinitionParser(PropertiesContainerContext.class, true));

        // Models
        registerBeanDefinitionParser("model", new InheritDefinitionParser(new OrphanDefinitionParser(SedaModel.class, true), new NamedDefinitionParser()));
        registerBeanDefinitionParser("seda-model", new InheritDefinitionParser(new OrphanDefinitionParser(SedaModel.class, true), new NamedDefinitionParser()));
//        registerBeanDefinitionParser("model-seda-optimised", new OrphanDefinitionParser(OptimisedSedaModel.class, true));
//        registerBeanDefinitionParser("model-pipeline", new OrphanDefinitionParser(PipelineModel.class, true));

        //TODO RM*
        registerBeanDefinitionParser("custom-entrypoint-resolver", new ChildDefinitionParser("entryPointResolverSet", null));
        //registerBeanDefinitionParser("callable-entrypoint-resolver", new ChildDefinitionParser("entryPointResolver", CallableEntryPointResolver.class));
        //registerBeanDefinitionParser("method-entrypoint-resolver", new ChildDefinitionParser("entrypointResolver", MethodEntryPointResolver.class));
        //registerBeanDefinitionParser("reflection-entrypoint-resolver", new ChildDefinitionParser("entrypointResolver", MethodEntryPointResolver.class));
        //registerBeanDefinitionParser("non-void-entrypoint-resolver", new ChildDefinitionParser("entrypointResolver", NonVoidEntryPointResolver.class));

        // Components
        registerBeanDefinitionParser("seda-component", new PassThroughComponentAdapter(new ComponentDefinitionParser(SedaComponent.class)));
        registerBeanDefinitionParser("service", new PassThroughComponentAdapter(new ComponentDefinitionParser(SedaComponent.class)));

        // Common POJO Services
        registerBeanDefinitionParser("bridge-component", new SimplePojoServiceDefinitionParser(PassThroughComponent.class));
        registerBeanDefinitionParser("pass-through-component", new SimplePojoServiceDefinitionParser(PassThroughComponent.class));
        registerBeanDefinitionParser("log-component", new SimplePojoServiceDefinitionParser(LogComponent.class));
        registerBeanDefinitionParser("echo-component", new SimplePojoServiceDefinitionParser(EchoComponent.class));
        registerBeanDefinitionParser("null-component", new SimplePojoServiceDefinitionParser(NullComponent.class));
        registerBeanDefinitionParser("no-args-call-component", new SimplePojoServiceDefinitionParser(NoArgsCallWrapper.class));

        //Object Factories
        registerBeanDefinitionParser("singleton-object", new PojoServiceDefinitionParser(SingletonObjectFactory.class));
        registerBeanDefinitionParser("prototype-object", new PojoServiceDefinitionParser(PrototypeObjectFactory.class));
        BeanDefinitionParser bpdPooledObject = new PojoServiceDefinitionParser(PooledObjectFactory.class);
        registerBeanDefinitionParser("pooled-object", bpdPooledObject);
        registerBeanDefinitionParser("component", bpdPooledObject);

        //Routers
        registerBeanDefinitionParser("inbound-router", new ChildDefinitionParser("inboundRouter", InboundRouterCollection.class));
        registerBeanDefinitionParser("outbound-router", new ChildDefinitionParser("outboundRouter", OutboundRouterCollection.class));
        registerBeanDefinitionParser("nested-router", new ChildDefinitionParser("nestedRouter", NestedRouterCollection.class));
        registerBeanDefinitionParser("response-router", new ChildDefinitionParser("responseRouter", ResponseRouterCollection.class));

        //NoArgsCallWrapper
        registerBeanDefinitionParser("delegateClass", new AttributeMapDefinitionParser("properties"));
        registerBeanDefinitionParser("delegateInstance", new AttributeMapDefinitionParser("properties"));

        //Inbound Routers
        registerBeanDefinitionParser("forwarding-router", new ForwardingRouterDefinitionParser());
        registerBeanDefinitionParser("inbound-pass-through-router", new RouterDefinitionParser("router", InboundPassThroughRouter.class));
        registerBeanDefinitionParser("idempotent-receiver-router", new RouterDefinitionParser("router", IdempotentReceiver.class));
        registerBeanDefinitionParser("idempotent-secure-hash-receiver-router", new RouterDefinitionParser("router", IdempotentSecureHashReceiver.class));
        registerBeanDefinitionParser("selective-consumer-router", new RouterDefinitionParser("router", SelectiveConsumer.class));
        registerBeanDefinitionParser("wire-tap-router", new RouterDefinitionParser("router", WireTap.class));
        registerBeanDefinitionParser("correlation-aggregator-router", new RouterDefinitionParser("router"));
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
        registerMuleDefinitionParser("reply-to", new ParentDefinitionParser()).addAlias("address", "replyTo");

        //Response Routers
        registerBeanDefinitionParser("custom-response-router", new RouterDefinitionParser("router", null));
        registerBeanDefinitionParser("single-response-router", new RouterDefinitionParser("router", SingleResponseRouter.class));

        //Property Extractors
        registerBeanDefinitionParser("function-property-extractor", new ChildDefinitionParser("propertyExtractor", FunctionPropertyExtractor.class));
        registerBeanDefinitionParser("correlation-property-extractor", new ChildDefinitionParser("propertyExtractor", CorrelationPropertiesExtractor.class));
        registerBeanDefinitionParser("custom-property-extractor", new ChildDefinitionParser("propertyExtractor"));
        registerBeanDefinitionParser("map-property-extractor", new ChildDefinitionParser("propertyExtractor", MapPayloadPropertyExtractor.class));
        registerBeanDefinitionParser("message-property-extractor", new ChildDefinitionParser("propertyExtractor", MessageHeaderPropertyExtractor.class));
        //registerBeanDefinitionParser("payload-property-extractor", new ChildDefinitionParser("propertyExtractor", PayloadPropertyExtractor.class));

        //Catch all Strategies
        registerBeanDefinitionParser("logging-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", LoggingCatchAllStrategy.class));
        registerBeanDefinitionParser("custom-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", null));
        registerBeanDefinitionParser("forwarding-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", ForwardingCatchAllStrategy.class));
        registerBeanDefinitionParser("custom-forwarding-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", null));

        //Common Filters
        registerMuleDefinitionParser("filter", new ParentDefinitionParser()).addAlias("ref", "filter");
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
        registerMuleDefinitionParser("add-properties", new ChildMapDefinitionParser("addProperties"));
        registerMuleDefinitionParser("delete-properties", new ChildListDefinitionParser("deleteProperties"));
        registerBeanDefinitionParser("jndi-provider-properties", new ChildMapDefinitionParser("jndiProviderProperties"));
        registerBeanDefinitionParser("environment", new ChildMapDefinitionParser("environment"));

        //Security
        registerMuleDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER)).addIgnored("type").addIgnored("name");
        registerBeanDefinitionParser("custom-security-provider", new NameTransferDefinitionParser("providers"));
        registerMuleDefinitionParser("custom-encryption-strategy", new NameTransferDefinitionParser("encryptionStrategies")).addAlias("strategy", "encryptionStrategy");
        registerBeanDefinitionParser("password-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", PasswordBasedEncryptionStrategy.class));
        registerBeanDefinitionParser("secret-key-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", SecretKeyEncryptionStrategy.class));
        registerBeanDefinitionParser("encryption-security-filter", new ChildDefinitionParser("securityFilter", MuleEncryptionEndpointSecurityFilter.class));
    }
}
