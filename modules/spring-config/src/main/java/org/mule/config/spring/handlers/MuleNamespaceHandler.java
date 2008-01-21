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
import org.mule.components.simple.NullComponent;
import org.mule.components.simple.PassThroughComponent;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.QueueProfile;
import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.parsers.collection.ChildListDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.delegate.InheritDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.NameTransferDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.BindingDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.ConfigurationDefinitionParser;
import org.mule.config.spring.parsers.specific.ConnectionStrategyDefinitionParser;
import org.mule.config.spring.parsers.specific.DefaultThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.EnvironmentPropertyDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.ForwardingRouterDefinitionParser;
import org.mule.config.spring.parsers.specific.MuleAdminAgentDefinitionParser;
import org.mule.config.spring.parsers.specific.NotificationDefinitionParser;
import org.mule.config.spring.parsers.specific.NotificationDisableDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.config.spring.parsers.specific.PoolingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceOverridesDefinitionParser;
import org.mule.config.spring.parsers.specific.SimplePojoServiceDefinitionParser;
import org.mule.config.spring.parsers.specific.SpringFactoryBeanDefinitionParser;
import org.mule.config.spring.parsers.specific.ThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionManagerDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerRefDefinitionParser;
import org.mule.config.spring.parsers.specific.IgnoreObjectMethodsDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.GenericEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.container.JndiContainerContext;
import org.mule.impl.container.PropertiesContainerContext;
import org.mule.impl.container.RmiContainerContext;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.impl.internal.notifications.manager.ListenerSubscriptionPair;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.impl.model.seda.SedaModel;
import org.mule.impl.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.impl.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.impl.model.resolvers.CallableEntryPointResolver;
import org.mule.impl.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.impl.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.impl.model.resolvers.ReflectionEntryPointResolver;
import org.mule.impl.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.impl.model.resolvers.ArrayEntryPointResolver;
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
        registerMuleBeanDefinitionParser("notifications", new NamedDefinitionParser(MuleProperties.OBJECT_NOTIFICATION_MANAGER)).addAlias("dynamic", "notificationDynamic");
        registerBeanDefinitionParser("notification", new NotificationDefinitionParser());
        registerBeanDefinitionParser("disable-notification", new NotificationDisableDefinitionParser());
        registerMuleBeanDefinitionParser("notification-listener", new ChildDefinitionParser("allListenerSubscriptionPair", ListenerSubscriptionPair.class)).addAlias("ref", "listener").addReference("listener");

        //Connector elements
        registerBeanDefinitionParser("dispatcher-threading-profile", new ThreadingProfileDefinitionParser("dispatcherThreadingProfile", MuleConfiguration.DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE));
        registerBeanDefinitionParser("receiver-threading-profile", new ThreadingProfileDefinitionParser("receiverThreadingProfile", MuleConfiguration.DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE));
        registerBeanDefinitionParser("dispatcher-connection-strategy", new ConnectionStrategyDefinitionParser("dispatcherConnectionStrategy"));
        registerBeanDefinitionParser("receiver-connection-straqtegy", new ConnectionStrategyDefinitionParser("receiverConnectionStrategy"));
        registerBeanDefinitionParser("service-overrides", new ServiceOverridesDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new MuleOrphanDefinitionParser(true));

        //Transformer elements
        registerBeanDefinitionParser("transformers", new ParentDefinitionParser());
        registerMuleBeanDefinitionParser("responseTransformers", new ParentDefinitionParser()).addAlias("transformer", "responseTransformer");

        registerBeanDefinitionParser("transformer", new TransformerRefDefinitionParser());

        registerBeanDefinitionParser("custom-transformer", new TransformerDefinitionParser());
        registerBeanDefinitionParser("no-action-transformer", new TransformerDefinitionParser(NoActionTransformer.class));
        registerBeanDefinitionParser("message-properties-transformer", new TransformerDefinitionParser(MessagePropertiesTransformer.class));

        registerBeanDefinitionParser("base64-encoder-transformer", new TransformerDefinitionParser(Base64Encoder.class));
        registerBeanDefinitionParser("base64-decoder-transformer", new TransformerDefinitionParser(Base64Decoder.class));

        registerBeanDefinitionParser("xml-entity-encoder-transformer", new TransformerDefinitionParser(XmlEntityEncoder.class));
        registerBeanDefinitionParser("xml-entity-decoder-transformer", new TransformerDefinitionParser(XmlEntityDecoder.class));
        registerBeanDefinitionParser("gzip-compress-transformer", new TransformerDefinitionParser(GZipCompressTransformer.class));
        registerBeanDefinitionParser("gzip-uncompress-transformer", new TransformerDefinitionParser(GZipUncompressTransformer.class));
        registerBeanDefinitionParser("encrypt-transformer", new TransformerDefinitionParser(EncryptionTransformer.class));
        registerBeanDefinitionParser("decrypt-transformer", new TransformerDefinitionParser(DecryptionTransformer.class));
        registerBeanDefinitionParser("byte-array-to-hex-string-transformer", new TransformerDefinitionParser(ByteArrayToHexString.class));
        registerBeanDefinitionParser("hex-string-to-byte-array-transformer", new TransformerDefinitionParser(HexStringToByteArray.class));

        registerBeanDefinitionParser("byte-array-to-object-transformer", new TransformerDefinitionParser(ByteArrayToObject.class));
        registerBeanDefinitionParser("object-to-byte-array-transformer", new TransformerDefinitionParser(ObjectToByteArray.class));
        registerBeanDefinitionParser("object-to-string-transformer", new TransformerDefinitionParser(ObjectToString.class));
        registerBeanDefinitionParser("byte-array-to-serializable-transformer", new TransformerDefinitionParser(ByteArrayToSerializable.class));
        registerBeanDefinitionParser("serializable-to-byte-array-transformer", new TransformerDefinitionParser(SerializableToByteArray.class));
        registerBeanDefinitionParser("byte-array-to-string-transformer", new TransformerDefinitionParser(ObjectToString.class));
        registerBeanDefinitionParser("string-to-byte-array-transformer", new TransformerDefinitionParser(ObjectToByteArray.class));

        registerBeanDefinitionParser("append-string-transformer", new TransformerDefinitionParser(StringAppendTransformer.class));

        //Transaction Managers
        registerBeanDefinitionParser("custom-transaction-manager", new TransactionManagerDefinitionParser());
        registerBeanDefinitionParser("jndi-transaction-manager", new TransactionManagerDefinitionParser(GenericTransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("weblogic-transaction-manager", new TransactionManagerDefinitionParser(WeblogicTransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("jboss-transaction-manager", new TransactionManagerDefinitionParser(JBossTransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("jrun-transaction-manager", new TransactionManagerDefinitionParser(JRunTransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("resin-transaction-manager", new TransactionManagerDefinitionParser(Resin3TransactionManagerLookupFactory.class));
        registerBeanDefinitionParser("websphere-transaction-manager", new TransactionManagerDefinitionParser(WebsphereTransactionManagerLookupFactory.class));

        //Endpoint elements
        registerBeanDefinitionParser("endpoint", new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
        registerBeanDefinitionParser("inbound-endpoint", new GenericEndpointDefinitionParser(InboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("outbound-endpoint", new GenericEndpointDefinitionParser(OutboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("custom-transaction", new TransactionDefinitionParser());

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

        registerBeanDefinitionParser("entry-point-resolver-set", new ChildDefinitionParser("entryPointResolverSet", DefaultEntryPointResolverSet.class));
        registerBeanDefinitionParser("legacy-entry-point-resolver-set", new ChildDefinitionParser("entryPointResolverSet", LegacyEntryPointResolverSet.class));
        registerBeanDefinitionParser("custom-entry-point-resolver-set", new ChildDefinitionParser("entryPointResolverSet"));

        registerBeanDefinitionParser("custom-entry-point-resolver", new ChildDefinitionParser("entryPointResolver"));
        registerBeanDefinitionParser("callable-entry-point-resolver", new ChildDefinitionParser("entryPointResolver", CallableEntryPointResolver.class));
        registerMuleBeanDefinitionParser("property-entry-point-resolver", new ChildDefinitionParser("entryPointResolver", MethodHeaderPropertyEntryPointResolver.class)).addAlias("property", "methodProperty");
        registerBeanDefinitionParser("method-entry-point-resolver", new ChildDefinitionParser("entryPointResolver", ExplicitMethodEntryPointResolver.class));
        registerBeanDefinitionParser("reflection-entry-point-resolver", new ChildDefinitionParser("entryPointResolver", ReflectionEntryPointResolver.class));
        registerBeanDefinitionParser("no-arguments-entry-point-resolver", new ChildDefinitionParser("entryPointResolver", NoArgumentsEntryPointResolver.class));
        registerBeanDefinitionParser("array-entry-point-resolver", new ChildDefinitionParser("entryPointResolver", ArrayEntryPointResolver.class));
        registerMuleBeanDefinitionParser("include-entry-point", new ParentDefinitionParser());
        registerMuleBeanDefinitionParser("exclude-entry-point", new ParentDefinitionParser()).addAlias("method", "ignoredMethod");
        registerMuleBeanDefinitionParser("exclude-object-methods", new IgnoreObjectMethodsDefinitionParser());

        // Services
        registerBeanDefinitionParser("seda-component", new ServiceDefinitionParser(SedaComponent.class));
        registerBeanDefinitionParser("service", new ServiceDefinitionParser(SedaComponent.class));

        // Pojo Components
        registerBeanDefinitionParser("component", new ComponentDefinitionParser());
        registerMuleBeanDefinitionParser("binding", new BindingDefinitionParser("nestedRouter.routers", NestedRouter.class)).addCollection("nestedRouter.routers");

        // Other Components
        registerBeanDefinitionParser("bridge-component", new SimplePojoServiceDefinitionParser(PassThroughComponent.class));
        registerBeanDefinitionParser("pass-through-component", new SimplePojoServiceDefinitionParser(PassThroughComponent.class));
        registerBeanDefinitionParser("log-component", new SimplePojoServiceDefinitionParser(LogComponent.class));
        registerBeanDefinitionParser("echo-component", new SimplePojoServiceDefinitionParser(EchoComponent.class));
        registerBeanDefinitionParser("null-component", new SimplePojoServiceDefinitionParser(NullComponent.class));

        // Object Factories
        registerBeanDefinitionParser("singleton-object", new ObjectFactoryDefinitionParser(SingletonObjectFactory.class));
        registerBeanDefinitionParser("prototype-object", new ObjectFactoryDefinitionParser(PrototypeObjectFactory.class));
        registerBeanDefinitionParser("pooled-object", new ObjectFactoryDefinitionParser(PooledObjectFactory.class));
        registerBeanDefinitionParser("spring-factory-bean", new SpringFactoryBeanDefinitionParser());

        //Routers
        registerBeanDefinitionParser("inbound", new ChildDefinitionParser("inboundRouter", InboundRouterCollection.class));
        registerBeanDefinitionParser("outbound", new ChildDefinitionParser("outboundRouter", OutboundRouterCollection.class));
        registerBeanDefinitionParser("async-reply", new ChildDefinitionParser("responseRouter", ResponseRouterCollection.class));

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
        registerBeanDefinitionParser("recipients", new ChildListDefinitionParser("recipients"));
        registerBeanDefinitionParser("template-endpoint-router", new RouterDefinitionParser("router", TemplateEndpointRouter.class));
        registerBeanDefinitionParser("custom-outbound-router", new RouterDefinitionParser("router", null));
        registerMuleBeanDefinitionParser("reply-to", new ParentDefinitionParser()).addAlias("address", "replyTo");

        //Response Routers
        registerBeanDefinitionParser("custom-async-reply-router", new RouterDefinitionParser("router", null));
        registerBeanDefinitionParser("single-async-reply-router", new RouterDefinitionParser("router", SingleResponseRouter.class));

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
        registerMuleBeanDefinitionParser("filter", new ParentDefinitionParser()).addAlias("ref", "filter");
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
        registerMuleBeanDefinitionParser("properties", new ChildMapDefinitionParser("properties")).addCollection("properties");
        registerMuleBeanDefinitionParser("property", new ChildMapEntryDefinitionParser("properties")).addCollection("properties");
        registerMuleBeanDefinitionParser("add-message-properties", new ChildMapDefinitionParser("addProperties")).addCollection("addProperties");
        registerMuleBeanDefinitionParser("add-message-property", new ChildMapEntryDefinitionParser("addProperties")).addCollection("addProperties");
        registerBeanDefinitionParser("delete-message-property", new ChildListEntryDefinitionParser("deleteProperties", ChildMapEntryDefinitionParser.KEY));
        registerMuleBeanDefinitionParser("jndi-provider-properties", new ChildMapDefinitionParser("jndiProviderProperties")).addCollection("jndiProviderProperties");
        registerMuleBeanDefinitionParser("jndi-provider-property", new ChildMapEntryDefinitionParser("jndiProviderProperties")).addCollection("jndiProviderProperties");
        registerBeanDefinitionParser("environment", new ChildMapDefinitionParser("environment"));

        //Security
        registerMuleBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER)).addIgnored("type").addIgnored("name");
        registerBeanDefinitionParser("custom-security-provider", new NameTransferDefinitionParser("providers"));
        registerMuleBeanDefinitionParser("custom-encryption-strategy", new NameTransferDefinitionParser("encryptionStrategies")).addAlias("strategy", "encryptionStrategy");
        registerBeanDefinitionParser("password-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", PasswordBasedEncryptionStrategy.class));
        registerBeanDefinitionParser("secret-key-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", SecretKeyEncryptionStrategy.class));
        registerBeanDefinitionParser("encryption-security-filter", new ChildDefinitionParser("securityFilter", MuleEncryptionEndpointSecurityFilter.class));
    }
    
}
