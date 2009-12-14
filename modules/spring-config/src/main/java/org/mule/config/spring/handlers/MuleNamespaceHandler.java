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

import org.mule.DefaultExceptionStrategy;
import org.mule.api.config.MuleProperties;
import org.mule.component.DefaultJavaComponent;
import org.mule.component.PooledJavaComponent;
import org.mule.component.SimpleCallableJavaComponent;
import org.mule.component.simple.EchoComponent;
import org.mule.component.simple.LogComponent;
import org.mule.component.simple.NullComponent;
import org.mule.component.simple.PassThroughComponent;
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
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.specific.AgentDefinitionParser;
import org.mule.config.spring.parsers.specific.BindingDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDelegatingDefinitionParser;
import org.mule.config.spring.parsers.specific.ConfigurationDefinitionParser;
import org.mule.config.spring.parsers.specific.DefaultThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.ExceptionTXFilterDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.GlobalPropertyDefinitionParser;
import org.mule.config.spring.parsers.specific.IgnoreObjectMethodsDefinitionParser;
import org.mule.config.spring.parsers.specific.InterceptorDefinitionParser;
import org.mule.config.spring.parsers.specific.InterceptorStackDefinitionParser;
import org.mule.config.spring.parsers.specific.MessagePropertiesTransformerDefinitionParser;
import org.mule.config.spring.parsers.specific.ModelDefinitionParser;
import org.mule.config.spring.parsers.specific.NotificationDefinitionParser;
import org.mule.config.spring.parsers.specific.NotificationDisableDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.config.spring.parsers.specific.PoolingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceOverridesDefinitionParser;
import org.mule.config.spring.parsers.specific.SimpleComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.StaticComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.ThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionManagerDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerRefDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.EndpointRefParser;
import org.mule.config.spring.parsers.specific.endpoint.GenericEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.config.spring.util.SpringBeanLookup;
import org.mule.context.notification.ListenerSubscriptionPair;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.BeanBuilderTransformer;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.expression.transformers.ExpressionTransformer;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.interceptor.TimerInterceptor;
import org.mule.model.resolvers.ArrayEntryPointResolver;
import org.mule.model.resolvers.CallableEntryPointResolver;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.model.resolvers.ReflectionEntryPointResolver;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.object.PrototypeObjectFactory;
import org.mule.object.SingletonObjectFactory;
import org.mule.routing.ExpressionMessageInfoMapping;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.binding.DefaultInterfaceBinding;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.ExceptionTypeFilter;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.filters.logic.OrFilter;
import org.mule.routing.inbound.CorrelationEventResequencer;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.routing.inbound.ForwardingConsumer;
import org.mule.routing.inbound.IdempotentReceiver;
import org.mule.routing.inbound.IdempotentSecureHashReceiver;
import org.mule.routing.inbound.MessageChunkingAggregator;
import org.mule.routing.inbound.SelectiveConsumer;
import org.mule.routing.inbound.SimpleCollectionAggregator;
import org.mule.routing.inbound.WireTap;
import org.mule.routing.outbound.ChainingRouter;
import org.mule.routing.outbound.DefaultOutboundRouterCollection;
import org.mule.routing.outbound.EndpointSelector;
import org.mule.routing.outbound.ExceptionBasedRouter;
import org.mule.routing.outbound.ExpressionMessageSplitter;
import org.mule.routing.outbound.ExpressionRecipientList;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.routing.outbound.ListMessageSplitter;
import org.mule.routing.outbound.MessageChunkingRouter;
import org.mule.routing.outbound.MulticastingRouter;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.routing.outbound.TemplateEndpointRouter;
import org.mule.routing.response.DefaultResponseRouterCollection;
import org.mule.routing.response.SimpleCollectionResponseAggregator;
import org.mule.routing.response.SingleResponseRouter;
import org.mule.security.PasswordBasedEncryptionStrategy;
import org.mule.security.SecretKeyEncryptionStrategy;
import org.mule.security.filters.MuleEncryptionEndpointSecurityFilter;
import org.mule.service.DefaultServiceExceptionStrategy;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transaction.lookup.GenericTransactionManagerLookupFactory;
import org.mule.transaction.lookup.JBossTransactionManagerLookupFactory;
import org.mule.transaction.lookup.JRunTransactionManagerLookupFactory;
import org.mule.transaction.lookup.Resin3TransactionManagerLookupFactory;
import org.mule.transaction.lookup.WeblogicTransactionManagerLookupFactory;
import org.mule.transaction.lookup.WebsphereTransactionManagerLookupFactory;
import org.mule.transformer.NoActionTransformer;
import org.mule.transformer.codec.Base64Decoder;
import org.mule.transformer.codec.Base64Encoder;
import org.mule.transformer.codec.XmlEntityDecoder;
import org.mule.transformer.codec.XmlEntityEncoder;
import org.mule.transformer.compression.GZipCompressTransformer;
import org.mule.transformer.compression.GZipUncompressTransformer;
import org.mule.transformer.encryption.DecryptionTransformer;
import org.mule.transformer.encryption.EncryptionTransformer;
import org.mule.transformer.simple.AutoTransformer;
import org.mule.transformer.simple.BeanToMap;
import org.mule.transformer.simple.ByteArrayToHexString;
import org.mule.transformer.simple.ByteArrayToObject;
import org.mule.transformer.simple.ByteArrayToSerializable;
import org.mule.transformer.simple.HexStringToByteArray;
import org.mule.transformer.simple.MapToBean;
import org.mule.transformer.simple.MessagePropertiesTransformer;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.ObjectToString;
import org.mule.transformer.simple.SerializableToByteArray;
import org.mule.transformer.simple.StringAppendTransformer;
import org.mule.util.store.InMemoryObjectStore;
import org.mule.util.store.TextFileObjectStore;

/**
 * This is the core namespace handler for Mule and configures all Mule configuration elements under the
 * <code>http://www.mulesoft.org/schema/mule/core/${version}</code> Namespace.
 */
public class MuleNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerIgnoredElement("mule");
        registerIgnoredElement("description");

        //Common elements
        registerBeanDefinitionParser("configuration", new ConfigurationDefinitionParser());
        registerBeanDefinitionParser("global-property", new GlobalPropertyDefinitionParser());
        registerBeanDefinitionParser("default-threading-profile", new DefaultThreadingProfileDefinitionParser(MuleProperties.OBJECT_DEFAULT_THREADING_PROFILE));
        registerBeanDefinitionParser("default-dispatcher-threading-profile", new DefaultThreadingProfileDefinitionParser(MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE));
        registerBeanDefinitionParser("default-receiver-threading-profile", new DefaultThreadingProfileDefinitionParser(MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE));
        registerBeanDefinitionParser("default-service-threading-profile", new DefaultThreadingProfileDefinitionParser(MuleProperties.OBJECT_DEFAULT_SERVICE_THREADING_PROFILE));
        registerBeanDefinitionParser("threading-profile", new ThreadingProfileDefinitionParser("threadingProfile", MuleProperties.OBJECT_DEFAULT_SERVICE_THREADING_PROFILE));
        registerBeanDefinitionParser("custom-exception-strategy", new ChildDefinitionParser("exceptionListener", null));
        registerBeanDefinitionParser("default-service-exception-strategy", new ChildDefinitionParser("exceptionListener", DefaultServiceExceptionStrategy.class));
        registerBeanDefinitionParser("commit-transaction", new ExceptionTXFilterDefinitionParser("commitTxFilter"));
        registerBeanDefinitionParser("rollback-transaction", new ExceptionTXFilterDefinitionParser("rollbackTxFilter"));
        registerBeanDefinitionParser("custom-agent", new AgentDefinitionParser());        

        registerBeanDefinitionParser("default-connector-exception-strategy", new ChildDefinitionParser("exceptionListener", DefaultExceptionStrategy.class));
        registerBeanDefinitionParser("pooling-profile", new PoolingProfileDefinitionParser());
        registerBeanDefinitionParser("queue-profile", new ChildDefinitionParser("queueProfile", QueueProfile.class));
        registerMuleBeanDefinitionParser("notifications", new NamedDefinitionParser(MuleProperties.OBJECT_NOTIFICATION_MANAGER)).addAlias("dynamic", "notificationDynamic");
        registerBeanDefinitionParser("notification", new NotificationDefinitionParser());
        registerBeanDefinitionParser("disable-notification", new NotificationDisableDefinitionParser());
        registerMuleBeanDefinitionParser("notification-listener", new ChildDefinitionParser("allListenerSubscriptionPair", ListenerSubscriptionPair.class)).addAlias("ref", "listener").addReference("listener");

        //Connector elements
        registerBeanDefinitionParser("dispatcher-threading-profile", new ThreadingProfileDefinitionParser("dispatcherThreadingProfile", MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE));
        registerBeanDefinitionParser("receiver-threading-profile", new ThreadingProfileDefinitionParser("receiverThreadingProfile", MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE));
        registerBeanDefinitionParser("service-overrides", new ServiceOverridesDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new MuleOrphanDefinitionParser(true));

        //Transformer elements
        registerBeanDefinitionParser("transformers", new ParentDefinitionParser());

        registerMuleBeanDefinitionParser("response-transformers", new ParentDefinitionParser());
        registerBeanDefinitionParser("transformer", new TransformerRefDefinitionParser());

        registerBeanDefinitionParser("custom-transformer", new TransformerDefinitionParser());
        registerBeanDefinitionParser("auto-transformer", new TransformerDefinitionParser(AutoTransformer.class));
        registerBeanDefinitionParser("no-action-transformer", new TransformerDefinitionParser(NoActionTransformer.class));
        registerBeanDefinitionParser("message-properties-transformer", new MessagePropertiesTransformerDefinitionParser());

        registerBeanDefinitionParser("expression-transformer", new TransformerDefinitionParser(ExpressionTransformer.class));
        registerBeanDefinitionParser("return-argument", new ChildDefinitionParser("argument", ExpressionArgument.class));

        registerBeanDefinitionParser("bean-builder-transformer", new TransformerDefinitionParser(BeanBuilderTransformer.class));
        registerBeanDefinitionParser("bean-property", new ChildDefinitionParser("argument", ExpressionArgument.class));

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

        registerBeanDefinitionParser("map-to-bean-transformer", new TransformerDefinitionParser(MapToBean.class));
        registerBeanDefinitionParser("bean-to-map-transformer", new TransformerDefinitionParser(BeanToMap.class));

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
        registerBeanDefinitionParser("xa-transaction", new TransactionDefinitionParser(XaTransactionFactory.class));

        // Models
        registerBeanDefinitionParser("model", new ModelDefinitionParser());
        registerBeanDefinitionParser("seda-model", new InheritDefinitionParser(new OrphanDefinitionParser(SedaModel.class, true), new NamedDefinitionParser()));

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
        registerBeanDefinitionParser("seda-service", new ServiceDefinitionParser(SedaService.class));
        registerBeanDefinitionParser("service", new ServiceDefinitionParser(SedaService.class));
        registerBeanDefinitionParser("custom-service", new ServiceDefinitionParser());

        // Components
        registerBeanDefinitionParser("component", new ComponentDelegatingDefinitionParser(DefaultJavaComponent.class));
        registerBeanDefinitionParser("pooled-component", new ComponentDelegatingDefinitionParser(PooledJavaComponent.class));

        registerMuleBeanDefinitionParser("binding", new BindingDefinitionParser("bindingCollection.routers", DefaultInterfaceBinding.class)).addCollection("bindingCollection.routers");

        // Simple Components
        registerBeanDefinitionParser("bridge-component", new ComponentDefinitionParser(PassThroughComponent.class));
        registerBeanDefinitionParser("pass-through-component", new ComponentDefinitionParser(PassThroughComponent.class));
        registerBeanDefinitionParser("log-component", new SimpleComponentDefinitionParser(SimpleCallableJavaComponent.class, LogComponent.class));
        registerBeanDefinitionParser("null-component", new SimpleComponentDefinitionParser(SimpleCallableJavaComponent.class, NullComponent.class));
        registerBeanDefinitionParser("static-component", new StaticComponentDefinitionParser());        
        registerIgnoredElement("return-data"); // Handled by StaticComponentDefinitionParser

        // We need to use DefaultJavaComponent for the echo component because some tests invoke EchoComponent with method name and therefore we need an entry point resolver
        registerBeanDefinitionParser("echo-component", new SimpleComponentDefinitionParser(DefaultJavaComponent.class, EchoComponent.class));

        // Object Factories
        registerBeanDefinitionParser("singleton-object", new ObjectFactoryDefinitionParser(SingletonObjectFactory.class, "objectFactory"));
        registerBeanDefinitionParser("prototype-object", new ObjectFactoryDefinitionParser(PrototypeObjectFactory.class, "objectFactory"));
        registerBeanDefinitionParser("spring-object", new ObjectFactoryDefinitionParser(SpringBeanLookup.class, "objectFactory"));

        // Life-cycle Adapters Factories
        registerBeanDefinitionParser("custom-lifecycle-adapter-factory", new ChildDefinitionParser("lifecycleAdapterFactory"));

        //Stores
        registerBeanDefinitionParser("in-memory-store", new ChildDefinitionParser("store", InMemoryObjectStore.class));
        registerBeanDefinitionParser("simple-text-file-store", new ChildDefinitionParser("store", TextFileObjectStore.class));

        //Routers
        registerBeanDefinitionParser("inbound", new ChildDefinitionParser("inboundRouter", DefaultInboundRouterCollection.class));
        registerBeanDefinitionParser("outbound", new ChildDefinitionParser("outboundRouter", DefaultOutboundRouterCollection.class));
        registerBeanDefinitionParser("async-reply", new ChildDefinitionParser("responseRouter", DefaultResponseRouterCollection.class));

        //Inbound Routers
        registerBeanDefinitionParser("forwarding-router", new RouterDefinitionParser(ForwardingConsumer.class));
        registerBeanDefinitionParser("idempotent-receiver-router", new RouterDefinitionParser(IdempotentReceiver.class));
        registerBeanDefinitionParser("idempotent-secure-hash-receiver-router", new RouterDefinitionParser(IdempotentSecureHashReceiver.class));
        registerBeanDefinitionParser("selective-consumer-router", new RouterDefinitionParser(SelectiveConsumer.class));
        registerBeanDefinitionParser("wire-tap-router", new RouterDefinitionParser(WireTap.class));
        registerBeanDefinitionParser("custom-correlation-aggregator-router", new RouterDefinitionParser());
        registerBeanDefinitionParser("collection-aggregator-router", new RouterDefinitionParser(SimpleCollectionAggregator.class));
        registerBeanDefinitionParser("message-chunking-aggregator-router", new RouterDefinitionParser(MessageChunkingAggregator.class));
        registerBeanDefinitionParser("correlation-resequencer-router", new RouterDefinitionParser(CorrelationEventResequencer.class));
        registerBeanDefinitionParser("custom-inbound-router", new RouterDefinitionParser(null));

        //Outbound Routers
        registerBeanDefinitionParser("pass-through-router", new RouterDefinitionParser(OutboundPassThroughRouter.class));
        registerBeanDefinitionParser("filtering-router", new RouterDefinitionParser(FilteringOutboundRouter.class));        registerBeanDefinitionParser("chaining-router", new RouterDefinitionParser(ChainingRouter.class));
        registerBeanDefinitionParser("endpoint-selector-router", new RouterDefinitionParser(EndpointSelector.class));
        registerBeanDefinitionParser("exception-based-router", new RouterDefinitionParser(ExceptionBasedRouter.class));
        registerBeanDefinitionParser("recipient-list-exception-based-router", new RouterDefinitionParser(ExceptionBasedRouter.class));
        registerBeanDefinitionParser("list-message-splitter-router", new RouterDefinitionParser(ListMessageSplitter.class));
        registerBeanDefinitionParser("expression-splitter-router", new RouterDefinitionParser(ExpressionMessageSplitter.class));
        registerBeanDefinitionParser("message-chunking-router", new RouterDefinitionParser(MessageChunkingRouter.class));
        registerBeanDefinitionParser("multicasting-router", new RouterDefinitionParser(MulticastingRouter.class));
        registerBeanDefinitionParser("static-recipient-list-router", new RouterDefinitionParser(StaticRecipientList.class));
        registerBeanDefinitionParser("expression-recipient-list-router", new RouterDefinitionParser(ExpressionRecipientList.class));
        registerBeanDefinitionParser("recipients", new ChildListDefinitionParser("recipients"));
        registerBeanDefinitionParser("template-endpoint-router", new RouterDefinitionParser(TemplateEndpointRouter.class));
        registerBeanDefinitionParser("custom-outbound-router", new RouterDefinitionParser(null));
        registerBeanDefinitionParser("reply-to", new EndpointRefParser("replyTo"));

        //Response Routers
        registerBeanDefinitionParser("custom-async-reply-router", new RouterDefinitionParser(null));
        registerBeanDefinitionParser("single-async-reply-router", new RouterDefinitionParser(SingleResponseRouter.class));
        registerBeanDefinitionParser("collection-async-reply-router", new RouterDefinitionParser(SimpleCollectionResponseAggregator.class));

        //Message Info Mappings
        registerBeanDefinitionParser("expression-message-info-mapping", new ChildDefinitionParser("messageInfoMapping", ExpressionMessageInfoMapping.class));
        registerBeanDefinitionParser("custom-message-info-mapping", new ChildDefinitionParser("messageInfoMapping"));

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
        registerBeanDefinitionParser("expression-filter", new FilterDefinitionParser(ExpressionFilter.class));
        registerBeanDefinitionParser("custom-filter", new FilterDefinitionParser());

        //Utils / Standard Types
        registerMuleBeanDefinitionParser("properties", new ChildMapDefinitionParser("properties")).addCollection("properties");
        registerMuleBeanDefinitionParser("property", new ChildMapEntryDefinitionParser("properties")).addCollection("properties");
        registerMuleBeanDefinitionParser("add-message-properties", new ChildMapDefinitionParser("addProperties")).addCollection("addProperties");
        registerMuleBeanDefinitionParser("add-message-property", new ChildMapEntryDefinitionParser("addProperties")).addCollection("addProperties");
        registerMuleBeanDefinitionParser("rename-message-property", new ChildMapEntryDefinitionParser("renameProperties")).addCollection("renameProperties");
        registerBeanDefinitionParser("delete-message-property", new ChildListEntryDefinitionParser("deleteProperties", ChildMapEntryDefinitionParser.KEY));
        registerMuleBeanDefinitionParser("jndi-provider-properties", new ChildMapDefinitionParser("jndiProviderProperties")).addCollection("jndiProviderProperties");
        registerMuleBeanDefinitionParser("jndi-provider-property", new ChildMapEntryDefinitionParser("jndiProviderProperties")).addCollection("jndiProviderProperties");
        registerBeanDefinitionParser("environment", new ChildMapDefinitionParser("environment"));
        registerBeanDefinitionParser("expression", new ChildDefinitionParser("expression", ExpressionConfig.class));

        //Security
        registerMuleBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER)).addIgnored("type").addIgnored("name");
        registerBeanDefinitionParser("custom-security-provider", new NameTransferDefinitionParser("providers"));
        registerMuleBeanDefinitionParser("custom-encryption-strategy", new NameTransferDefinitionParser("encryptionStrategies")).addAlias("strategy", "encryptionStrategy");
        registerBeanDefinitionParser("password-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", PasswordBasedEncryptionStrategy.class));
        registerMuleBeanDefinitionParser("secret-key-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", SecretKeyEncryptionStrategy.class)).registerPreProcessor(new CheckExclusiveAttributes(new String[][]{new String[]{"key"}, new String[]{"keyFactory-ref"}}));
        registerBeanDefinitionParser("encryption-security-filter", new ChildDefinitionParser("securityFilter", MuleEncryptionEndpointSecurityFilter.class));
        registerBeanDefinitionParser("custom-security-filter", new ChildDefinitionParser("securityFilter"));
        //Interceptors
        registerBeanDefinitionParser("interceptor-stack", new InterceptorStackDefinitionParser());
        registerBeanDefinitionParser("custom-interceptor", new InterceptorDefinitionParser());
        registerBeanDefinitionParser("timer-interceptor", new InterceptorDefinitionParser(TimerInterceptor.class));
        registerBeanDefinitionParser("logging-interceptor", new InterceptorDefinitionParser(LoggingInterceptor.class));
    }

}
