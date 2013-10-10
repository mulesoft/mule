/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.RouteableExceptionStrategy;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.LoggerMessageProcessor;
import org.mule.cache.CachingMessageProcessor;
import org.mule.cache.ObjectStoreCachingStrategy;
import org.mule.component.DefaultInterfaceBinding;
import org.mule.component.DefaultJavaComponent;
import org.mule.component.PooledJavaComponent;
import org.mule.component.SimpleCallableJavaComponent;
import org.mule.component.simple.EchoComponent;
import org.mule.component.simple.LogComponent;
import org.mule.component.simple.NullComponent;
import org.mule.component.simple.PassThroughComponent;
import org.mule.config.QueueProfile;
import org.mule.config.spring.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.config.spring.factories.ChoiceRouterFactoryBean;
import org.mule.config.spring.factories.CompositeMessageSourceFactoryBean;
import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.MessageProcessorFilterPairFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.factories.PollingMessageSourceFactoryBean;
import org.mule.config.spring.factories.TransactionalMessageProcessorsFactoryBean;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.cache.CacheDefinitionParser;
import org.mule.config.spring.parsers.cache.CachingStrategyDefinitionParser;
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
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributesWhenNoChildren;
import org.mule.config.spring.parsers.specific.AggregatorDefinitionParser;
import org.mule.config.spring.parsers.specific.BindingDefinitionParser;
import org.mule.config.spring.parsers.specific.BridgeDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDelegatingDefinitionParser;
import org.mule.config.spring.parsers.specific.ConfigurationDefinitionParser;
import org.mule.config.spring.parsers.specific.DefaultNameMuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.DefaultThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.ExceptionTXFilterDefinitionParser;
import org.mule.config.spring.parsers.specific.ExpressionTransformerDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterRefDefinitionParser;
import org.mule.config.spring.parsers.specific.FlowDefinitionParser;
import org.mule.config.spring.parsers.specific.FlowRefDefinitionParser;
import org.mule.config.spring.parsers.specific.GlobalPropertyDefinitionParser;
import org.mule.config.spring.parsers.specific.IgnoreObjectMethodsDefinitionParser;
import org.mule.config.spring.parsers.specific.InboundRouterDefinitionParser;
import org.mule.config.spring.parsers.specific.InterceptorDefinitionParser;
import org.mule.config.spring.parsers.specific.InterceptorStackDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageEnricherDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageFilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorChainDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.MessagePropertiesTransformerDefinitionParser;
import org.mule.config.spring.parsers.specific.ModelDefinitionParser;
import org.mule.config.spring.parsers.specific.NotificationDefinitionParser;
import org.mule.config.spring.parsers.specific.NotificationDisableDefinitionParser;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.config.spring.parsers.specific.PoolingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.RegExFilterDefinitionParser;
import org.mule.config.spring.parsers.specific.ResponseDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceOverridesDefinitionParser;
import org.mule.config.spring.parsers.specific.SimpleComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.SimpleServiceDefinitionParser;
import org.mule.config.spring.parsers.specific.SplitterDefinitionParser;
import org.mule.config.spring.parsers.specific.StaticComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.ThreadingProfileDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionManagerDefinitionParser;
import org.mule.config.spring.parsers.specific.ValidatorDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.EndpointRefParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.config.spring.util.SpringBeanLookup;
import org.mule.context.notification.ListenerSubscriptionPair;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.enricher.MessageEnricher;
import org.mule.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.exception.DefaultServiceExceptionStrategy;
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
import org.mule.processor.InvokerMessageProcessor;
import org.mule.processor.NullMessageProcessor;
import org.mule.routing.CollectionSplitter;
import org.mule.routing.ExpressionMessageInfoMapping;
import org.mule.routing.ExpressionSplitter;
import org.mule.routing.FirstSuccessful;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.IdempotentMessageFilter;
import org.mule.routing.IdempotentSecureHashMessageFilter;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.MapSplitter;
import org.mule.routing.MessageChunkAggregator;
import org.mule.routing.MessageChunkSplitter;
import org.mule.routing.MessageFilter;
import org.mule.routing.Resequencer;
import org.mule.routing.RoundRobin;
import org.mule.routing.SimpleCollectionAggregator;
import org.mule.routing.WireTap;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.filters.ExceptionTypeFilter;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.filters.logic.OrFilter;
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
import org.mule.routing.outbound.SequenceRouter;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.routing.requestreply.SimpleAsyncRequestReplyRequester;
import org.mule.security.PasswordBasedEncryptionStrategy;
import org.mule.security.SecretKeyEncryptionStrategy;
import org.mule.security.UsernamePasswordAuthenticationFilter;
import org.mule.security.filters.MuleEncryptionEndpointSecurityFilter;
import org.mule.service.ForwardingConsumer;
import org.mule.service.ServiceAsyncReplyCompositeMessageSource;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transaction.lookup.GenericTransactionManagerLookupFactory;
import org.mule.transaction.lookup.JBossTransactionManagerLookupFactory;
import org.mule.transaction.lookup.JRunTransactionManagerLookupFactory;
import org.mule.transaction.lookup.Resin3TransactionManagerLookupFactory;
import org.mule.transaction.lookup.WeblogicTransactionManagerLookupFactory;
import org.mule.transaction.lookup.WebsphereTransactionManagerLookupFactory;
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
import org.mule.transformer.simple.CombineCollectionsTransformer;
import org.mule.transformer.simple.HexStringToByteArray;
import org.mule.transformer.simple.MapToBean;
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
        registerBeanDefinitionParser("default-exception-strategy", new ChildDefinitionParser("exceptionListener", DefaultServiceExceptionStrategy.class));
        registerDeprecatedBeanDefinitionParser("default-service-exception-strategy", new ChildDefinitionParser("exceptionListener", DefaultServiceExceptionStrategy.class), "Use default-exception-strategy instead.");
        registerBeanDefinitionParser("commit-transaction", new ExceptionTXFilterDefinitionParser("commitTxFilter"));
        registerBeanDefinitionParser("rollback-transaction", new ExceptionTXFilterDefinitionParser("rollbackTxFilter"));
        registerBeanDefinitionParser("custom-agent", new DefaultNameMuleOrphanDefinitionParser());

        registerBeanDefinitionParser("routeable-exception-strategy", new ChildDefinitionParser("exceptionListener", RouteableExceptionStrategy.class));
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

        registerMuleBeanDefinitionParser("transformer", new ParentDefinitionParser()).addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "messageProcessor");

        registerBeanDefinitionParser("custom-transformer", new MessageProcessorDefinitionParser());
        registerBeanDefinitionParser("auto-transformer", new MessageProcessorDefinitionParser(AutoTransformer.class));
        registerBeanDefinitionParser("message-properties-transformer", new MessagePropertiesTransformerDefinitionParser());

        registerMuleBeanDefinitionParser("expression-transformer", new ExpressionTransformerDefinitionParser(
            ExpressionTransformer.class));

        registerBeanDefinitionParser("return-argument", new ChildDefinitionParser("argument", ExpressionArgument.class));

        registerBeanDefinitionParser("bean-builder-transformer", new MessageProcessorDefinitionParser(BeanBuilderTransformer.class));

        final ChildDefinitionParser beanPropertyParser = new ChildDefinitionParser("argument", ExpressionArgument.class);
        beanPropertyParser.addAlias("property-name", "name");
        registerBeanDefinitionParser("bean-property", beanPropertyParser);

        registerBeanDefinitionParser("base64-encoder-transformer", new MessageProcessorDefinitionParser(Base64Encoder.class));
        registerBeanDefinitionParser("base64-decoder-transformer", new MessageProcessorDefinitionParser(Base64Decoder.class));

        registerBeanDefinitionParser("xml-entity-encoder-transformer", new MessageProcessorDefinitionParser(XmlEntityEncoder.class));
        registerBeanDefinitionParser("xml-entity-decoder-transformer", new MessageProcessorDefinitionParser(XmlEntityDecoder.class));
        registerBeanDefinitionParser("gzip-compress-transformer", new MessageProcessorDefinitionParser(GZipCompressTransformer.class));
        registerBeanDefinitionParser("gzip-uncompress-transformer", new MessageProcessorDefinitionParser(GZipUncompressTransformer.class));
        registerBeanDefinitionParser("encrypt-transformer", new MessageProcessorDefinitionParser(EncryptionTransformer.class));
        registerBeanDefinitionParser("decrypt-transformer", new MessageProcessorDefinitionParser(DecryptionTransformer.class));
        registerBeanDefinitionParser("byte-array-to-hex-string-transformer", new MessageProcessorDefinitionParser(ByteArrayToHexString.class));
        registerBeanDefinitionParser("hex-string-to-byte-array-transformer", new MessageProcessorDefinitionParser(HexStringToByteArray.class));

        registerBeanDefinitionParser("byte-array-to-object-transformer", new MessageProcessorDefinitionParser(ByteArrayToObject.class));
        registerBeanDefinitionParser("object-to-byte-array-transformer", new MessageProcessorDefinitionParser(ObjectToByteArray.class));
        registerBeanDefinitionParser("object-to-string-transformer", new MessageProcessorDefinitionParser(ObjectToString.class));
        registerBeanDefinitionParser("byte-array-to-serializable-transformer", new MessageProcessorDefinitionParser(ByteArrayToSerializable.class));
        registerBeanDefinitionParser("serializable-to-byte-array-transformer", new MessageProcessorDefinitionParser(SerializableToByteArray.class));
        registerBeanDefinitionParser("byte-array-to-string-transformer", new MessageProcessorDefinitionParser(ObjectToString.class));
        registerBeanDefinitionParser("string-to-byte-array-transformer", new MessageProcessorDefinitionParser(ObjectToByteArray.class));

        registerBeanDefinitionParser("append-string-transformer", new MessageProcessorDefinitionParser(StringAppendTransformer.class));

        registerBeanDefinitionParser("map-to-bean-transformer", new MessageProcessorDefinitionParser(MapToBean.class));
        registerBeanDefinitionParser("bean-to-map-transformer", new MessageProcessorDefinitionParser(BeanToMap.class));

        registerBeanDefinitionParser("combine-collections-transformer", new MessageProcessorDefinitionParser(CombineCollectionsTransformer.class));
        
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
        registerBeanDefinitionParser("inbound-endpoint", new ChildEndpointDefinitionParser(InboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("poll", new ChildEndpointDefinitionParser(PollingMessageSourceFactoryBean.class));
        registerBeanDefinitionParser("outbound-endpoint", new ChildEndpointDefinitionParser(OutboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("custom-transaction", new TransactionDefinitionParser());
        registerBeanDefinitionParser("xa-transaction", new TransactionDefinitionParser(XaTransactionFactory.class));

        // Message Processors
        registerMuleBeanDefinitionParser("processor", new ParentDefinitionParser()).addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "messageProcessor");
        registerMuleBeanDefinitionParser("custom-processor", new MessageProcessorDefinitionParser()).addIgnored("name");
        registerBeanDefinitionParser("processor-chain", new MessageProcessorChainDefinitionParser());
        registerBeanDefinitionParser("response", new ResponseDefinitionParser());
        registerMuleBeanDefinitionParser("message-filter", new MessageFilterDefinitionParser());
        registerMuleBeanDefinitionParser("invoke",
            new MessageProcessorDefinitionParser(InvokerMessageProcessor.class)).addAlias("method",
            "methodName").addAlias("methodArguments", "argumentExpressionsString").addAlias(
            "methodArgumentTypes", "ArgumentTypes");
        registerMuleBeanDefinitionParser("enricher",
            new MessageEnricherDefinitionParser("messageProcessor", MessageEnricher.class)).addIgnored(
            "source")
            .addIgnored("target")
            .registerPreProcessor(
                new CheckExclusiveAttributesAndChildren(new String[]{"source", "target"},
                    new String[]{"enrich"}))
            .registerPreProcessor(
                new CheckRequiredAttributesWhenNoChildren(new String[][]{new String[]{"target"}}, "enrich"))
            .addCollection("enrichExpressionPairs");
        registerMuleBeanDefinitionParser("enrich", new ChildDefinitionParser("enrichExpressionPair",
            EnrichExpressionPair.class));

        registerBeanDefinitionParser("async", new ChildDefinitionParser("messageProcessor",
            AsyncMessageProcessorsFactoryBean.class));
        registerBeanDefinitionParser("transactional", new ChildDefinitionParser("messageProcessor",
            TransactionalMessageProcessorsFactoryBean.class));
        registerMuleBeanDefinitionParser("logger", new ChildDefinitionParser("messageProcessor",
            LoggerMessageProcessor.class));

        // Message Sources
        // TODO MULE-4987
        // registerBeanDefinitionParser("custom-source", new ChildDefinitionParser("messageSource", null, MessageSource.class));
        registerBeanDefinitionParser("composite-source", new ChildDefinitionParser("messageSource", CompositeMessageSourceFactoryBean.class));

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

        // Flow Constructs
        registerBeanDefinitionParser("flow", new FlowDefinitionParser());
        registerBeanDefinitionParser("simple-service", new SimpleServiceDefinitionParser());
        registerBeanDefinitionParser("bridge", new BridgeDefinitionParser());
        registerBeanDefinitionParser("validator", new ValidatorDefinitionParser());

        registerBeanDefinitionParser("flow-ref", new FlowRefDefinitionParser());

        // Components
        registerBeanDefinitionParser("component", new ComponentDelegatingDefinitionParser(DefaultJavaComponent.class));
        registerBeanDefinitionParser("pooled-component", new ComponentDelegatingDefinitionParser(PooledJavaComponent.class));

        registerMuleBeanDefinitionParser("binding", new BindingDefinitionParser("interfaceBinding", DefaultInterfaceBinding.class));

        // Simple Components
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
        registerBeanDefinitionParser("inbound", new ChildDefinitionParser("messageSource", ServiceCompositeMessageSource.class, true));
        registerBeanDefinitionParser("outbound", new ChildDefinitionParser("outboundMessageProcessor", DefaultOutboundRouterCollection.class, true));
        registerBeanDefinitionParser("async-reply", new ChildDefinitionParser("asyncReplyMessageSource", ServiceAsyncReplyCompositeMessageSource.class, true));

        //Inbound Routers
        registerBeanDefinitionParser("forwarding-router", new InboundRouterDefinitionParser(ForwardingConsumer.class));
        registerBeanDefinitionParser("idempotent-receiver-router", new InboundRouterDefinitionParser(IdempotentMessageFilter.class));
        registerBeanDefinitionParser("idempotent-secure-hash-receiver-router", new InboundRouterDefinitionParser(IdempotentSecureHashMessageFilter.class));
        registerBeanDefinitionParser("selective-consumer-router", new InboundRouterDefinitionParser(MessageFilter.class));
        registerBeanDefinitionParser("wire-tap-router", new InboundRouterDefinitionParser(WireTap.class));
        registerBeanDefinitionParser("custom-correlation-aggregator-router", new InboundRouterDefinitionParser());
        registerBeanDefinitionParser("collection-aggregator-router", new InboundRouterDefinitionParser(SimpleCollectionAggregator.class));
        registerBeanDefinitionParser("message-chunking-aggregator-router", new InboundRouterDefinitionParser(MessageChunkAggregator.class));
        registerBeanDefinitionParser("correlation-resequencer-router", new InboundRouterDefinitionParser(Resequencer.class));
        registerBeanDefinitionParser("custom-inbound-router", new InboundRouterDefinitionParser(null));

        //Outbound Routers
        registerBeanDefinitionParser("pass-through-router", new RouterDefinitionParser(OutboundPassThroughRouter.class));
        registerBeanDefinitionParser("filtering-router", new RouterDefinitionParser(FilteringOutboundRouter.class));
        registerBeanDefinitionParser("chaining-router", new RouterDefinitionParser(ChainingRouter.class));
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
        registerBeanDefinitionParser("custom-outbound-router", new RouterDefinitionParser(null));
        registerBeanDefinitionParser("reply-to", new EndpointRefParser("replyTo"));
        registerBeanDefinitionParser("sequence-router", new RouterDefinitionParser(SequenceRouter.class));

        //Response Routers
        registerBeanDefinitionParser("custom-async-reply-router", new InboundRouterDefinitionParser(null));
        registerBeanDefinitionParser("single-async-reply-router", new InboundRouterDefinitionParser(NullMessageProcessor.class));
        registerBeanDefinitionParser("collection-async-reply-router", new InboundRouterDefinitionParser(SimpleCollectionAggregator.class));

        // Routing: Intercepting Message Processors
        registerBeanDefinitionParser("idempotent-message-filter", new InboundRouterDefinitionParser(IdempotentMessageFilter.class));
        registerBeanDefinitionParser("idempotent-secure-hash-message-filter", new InboundRouterDefinitionParser(IdempotentSecureHashMessageFilter.class));
        registerBeanDefinitionParser("wire-tap", new InboundRouterDefinitionParser(WireTap.class));
        registerBeanDefinitionParser("custom-aggregator", new AggregatorDefinitionParser());
        registerBeanDefinitionParser("collection-aggregator", new AggregatorDefinitionParser(SimpleCollectionAggregator.class));
        registerBeanDefinitionParser("message-chunk-aggregator", new AggregatorDefinitionParser(MessageChunkAggregator.class));
        registerBeanDefinitionParser("resequencer", new InboundRouterDefinitionParser(Resequencer.class));
        registerBeanDefinitionParser("splitter", new SplitterDefinitionParser(ExpressionSplitter.class));
        registerBeanDefinitionParser("collection-splitter", new SplitterDefinitionParser(CollectionSplitter.class));
        registerBeanDefinitionParser("map-splitter", new SplitterDefinitionParser(MapSplitter.class));
        registerBeanDefinitionParser("message-chunk-splitter", new SplitterDefinitionParser(MessageChunkSplitter.class));
        registerBeanDefinitionParser("custom-splitter", new SplitterDefinitionParser());

        // Routing: Routing Message Processors

        // Routing: Conditional Routers
        registerBeanDefinitionParser("choice", new ChildDefinitionParser("messageProcessor", ChoiceRouterFactoryBean.class));
        registerBeanDefinitionParser("when", (ChildDefinitionParser)new ChildDefinitionParser("route", MessageProcessorFilterPairFactoryBean.class).registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            "expression"}, new String[]{"{http://www.mulesoft.org/schema/mule/core}abstractFilterType"})));
        registerBeanDefinitionParser("otherwise", new ChildDefinitionParser("defaultRoute", MessageProcessorFilterPairFactoryBean.class));

        registerBeanDefinitionParser("all", new ChildDefinitionParser("messageProcessor", MulticastingRouter.class));
        registerBeanDefinitionParser("recipient-list", new ChildDefinitionParser("messageProcessor", ExpressionRecipientList.class));

        registerBeanDefinitionParser("request-reply", new ChildDefinitionParser("messageProcessor", SimpleAsyncRequestReplyRequester.class));
        registerBeanDefinitionParser("first-successful", new ChildDefinitionParser("messageProcessor", FirstSuccessful.class));
        registerBeanDefinitionParser("round-robin", new ChildDefinitionParser("messageProcessor", RoundRobin.class));

        registerBeanDefinitionParser("custom-router", new ChildDefinitionParser("messageProcessor"));

        //Message Info Mappings
        registerBeanDefinitionParser("expression-message-info-mapping", new ChildDefinitionParser("messageInfoMapping", ExpressionMessageInfoMapping.class));
        registerBeanDefinitionParser("custom-message-info-mapping", new ChildDefinitionParser("messageInfoMapping"));

        //Catch all Strategies
        registerBeanDefinitionParser("logging-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", LoggingCatchAllStrategy.class));
        registerBeanDefinitionParser("custom-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", null));
        registerBeanDefinitionParser("forwarding-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", ForwardingCatchAllStrategy.class));
        registerBeanDefinitionParser("custom-forwarding-catch-all-strategy", new ChildDefinitionParser("catchAllStrategy", null));

        //Common Filters
        registerMuleBeanDefinitionParser("filter", new FilterRefDefinitionParser());
        registerBeanDefinitionParser("and-filter", new FilterDefinitionParser(AndFilter.class));
        registerBeanDefinitionParser("or-filter", new FilterDefinitionParser(OrFilter.class));
        registerBeanDefinitionParser("not-filter", new FilterDefinitionParser(NotFilter.class));
        registerBeanDefinitionParser("regex-filter", new RegExFilterDefinitionParser());
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
        registerBeanDefinitionParser("encryption-security-filter", new SecurityFilterDefinitionParser(MuleEncryptionEndpointSecurityFilter.class));
        registerBeanDefinitionParser("custom-security-filter", new SecurityFilterDefinitionParser());
        registerBeanDefinitionParser("username-password-filter", new SecurityFilterDefinitionParser(UsernamePasswordAuthenticationFilter.class));

        //Interceptors
        registerMuleBeanDefinitionParser("interceptor-stack", new InterceptorStackDefinitionParser());
        registerBeanDefinitionParser("custom-interceptor", new InterceptorDefinitionParser());
        registerBeanDefinitionParser("timer-interceptor", new InterceptorDefinitionParser(TimerInterceptor.class));
        registerBeanDefinitionParser("logging-interceptor", new InterceptorDefinitionParser(LoggingInterceptor.class));

        // Cache
        registerBeanDefinitionParser("caching-strategy", new CachingStrategyDefinitionParser(ObjectStoreCachingStrategy.class, true));
        registerBeanDefinitionParser("cache", new CacheDefinitionParser("messageProcessor", CachingMessageProcessor.class));
    }

}
