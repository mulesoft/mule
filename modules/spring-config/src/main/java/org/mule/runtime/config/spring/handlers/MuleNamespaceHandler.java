/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.handlers;

import org.mule.runtime.config.spring.factories.AggregationStrategyDefinitionParser;
import org.mule.runtime.config.spring.factories.ChoiceRouterFactoryBean;
import org.mule.runtime.config.spring.factories.CompositeMessageSourceFactoryBean;
import org.mule.runtime.config.spring.factories.DefaultMemoryQueueStoreFactoryBean;
import org.mule.runtime.config.spring.factories.DefaultPersistentQueueStoreFactoryBean;
import org.mule.runtime.config.spring.factories.FileQueueStoreFactoryBean;
import org.mule.runtime.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorFilterPairFactoryBean;
import org.mule.runtime.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.runtime.config.spring.factories.PollingMessageSourceFactoryBean;
import org.mule.runtime.config.spring.factories.QueueProfileFactoryBean;
import org.mule.runtime.config.spring.factories.ScatterGatherRouterFactoryBean;
import org.mule.runtime.config.spring.factories.SimpleMemoryQueueStoreFactoryBean;
import org.mule.runtime.config.spring.factories.SubflowMessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.TransactionalMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.WatermarkFactoryBean;
import org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.NameTransferDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.runtime.config.spring.parsers.processors.CheckExclusiveAttributeAndText;
import org.mule.runtime.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.runtime.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;
import org.mule.runtime.config.spring.parsers.processors.CheckRequiredAttributesWhenNoChildren;
import org.mule.runtime.config.spring.parsers.specific.AggregatorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.AsyncMessageProcessorsDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.BindingDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ComponentDelegatingDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ConfigurationDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.DefaultNameMuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.DefaultThreadingProfileDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ExceptionStrategyDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ExceptionTXFilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ExpressionComponentDefintionParser;
import org.mule.runtime.config.spring.parsers.specific.ExpressionLanguageDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ExpressionTransformerDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.FilterRefDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.FlowDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.FlowRefDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.GlobalFunctionsDefintionParser;
import org.mule.runtime.config.spring.parsers.specific.GlobalPropertyDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.IgnoreObjectMethodsDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ImportMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.InboundRouterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.InterceptorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.InterceptorStackDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageEnricherDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageFilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorChainDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.NotificationDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.NotificationDisableDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ObjectFactoryDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.PoolingProfileDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.QueueStoreDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ReferenceExceptionStrategyDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.RegExFilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ResponseDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.RetryNotifierDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.RetryPolicyDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ServiceOverridesDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SimpleComponentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SplitterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.StaticComponentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ThreadingProfileDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransactionManagerDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransformerMessageProcessorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TypedPropertyMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.XaTransactionDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.runtime.config.spring.util.SpringBeanLookup;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.component.DefaultInterfaceBinding;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.runtime.core.component.SimpleCallableJavaComponent;
import org.mule.runtime.core.component.simple.EchoComponent;
import org.mule.runtime.core.component.simple.LogComponent;
import org.mule.runtime.core.component.simple.NullComponent;
import org.mule.runtime.core.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.el.ExpressionLanguageComponent;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.core.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.runtime.core.exception.CatchMessagingExceptionStrategy;
import org.mule.runtime.core.exception.ChoiceMessagingExceptionStrategy;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.exception.RedeliveryExceeded;
import org.mule.runtime.core.exception.RollbackMessagingExceptionStrategy;
import org.mule.runtime.core.expression.ExpressionConfig;
import org.mule.runtime.core.expression.transformers.BeanBuilderTransformer;
import org.mule.runtime.core.expression.transformers.ExpressionArgument;
import org.mule.runtime.core.expression.transformers.ExpressionTransformer;
import org.mule.runtime.core.interceptor.LoggingInterceptor;
import org.mule.runtime.core.interceptor.TimerInterceptor;
import org.mule.runtime.core.model.resolvers.ArrayEntryPointResolver;
import org.mule.runtime.core.model.resolvers.CallableEntryPointResolver;
import org.mule.runtime.core.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.runtime.core.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.runtime.core.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.runtime.core.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.runtime.core.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.runtime.core.model.resolvers.ReflectionEntryPointResolver;
import org.mule.runtime.core.object.PrototypeObjectFactory;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.processor.InvokerMessageProcessor;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.processor.strategy.QueuedAsynchronousProcessingStrategy;
import org.mule.runtime.core.processor.strategy.QueuedThreadPerProcessorProcessingStrategy;
import org.mule.runtime.core.processor.strategy.ThreadPerProcessorProcessingStrategy;
import org.mule.runtime.core.retry.notifiers.ConnectNotifier;
import org.mule.runtime.core.retry.policies.RetryForeverPolicyTemplate;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.routing.CollectionSplitter;
import org.mule.runtime.core.routing.DynamicAll;
import org.mule.runtime.core.routing.DynamicFirstSuccessful;
import org.mule.runtime.core.routing.DynamicRoundRobin;
import org.mule.runtime.core.routing.ExpressionMessageInfoMapping;
import org.mule.runtime.core.routing.ExpressionSplitter;
import org.mule.runtime.core.routing.FirstSuccessful;
import org.mule.runtime.core.routing.Foreach;
import org.mule.runtime.core.routing.IdempotentMessageFilter;
import org.mule.runtime.core.routing.IdempotentSecureHashMessageFilter;
import org.mule.runtime.core.routing.MapSplitter;
import org.mule.runtime.core.routing.MessageChunkAggregator;
import org.mule.runtime.core.routing.MessageChunkSplitter;
import org.mule.runtime.core.routing.Resequencer;
import org.mule.runtime.core.routing.RoundRobin;
import org.mule.runtime.core.routing.SimpleCollectionAggregator;
import org.mule.runtime.core.routing.UntilSuccessful;
import org.mule.runtime.core.routing.WireTap;
import org.mule.runtime.core.routing.filters.EqualsFilter;
import org.mule.runtime.core.routing.filters.ExceptionTypeFilter;
import org.mule.runtime.core.routing.filters.ExpressionFilter;
import org.mule.runtime.core.routing.filters.MessagePropertyFilter;
import org.mule.runtime.core.routing.filters.PayloadTypeFilter;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.routing.filters.logic.AndFilter;
import org.mule.runtime.core.routing.filters.logic.NotFilter;
import org.mule.runtime.core.routing.filters.logic.OrFilter;
import org.mule.runtime.core.routing.outbound.ExpressionRecipientList;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;
import org.mule.runtime.core.routing.requestreply.SimpleAsyncRequestReplyRequester;
import org.mule.runtime.core.security.PasswordBasedEncryptionStrategy;
import org.mule.runtime.core.security.SecretKeyEncryptionStrategy;
import org.mule.runtime.core.security.UsernamePasswordAuthenticationFilter;
import org.mule.runtime.core.security.filters.MuleEncryptionEndpointSecurityFilter;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencySchedulerFactory;
import org.mule.runtime.core.transaction.XaTransactionFactory;
import org.mule.runtime.core.transaction.lookup.GenericTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.JBossTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.JRunTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.Resin3TransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.WeblogicTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.WebsphereTransactionManagerLookupFactory;
import org.mule.runtime.core.transformer.codec.Base64Decoder;
import org.mule.runtime.core.transformer.codec.Base64Encoder;
import org.mule.runtime.core.transformer.codec.XmlEntityDecoder;
import org.mule.runtime.core.transformer.codec.XmlEntityEncoder;
import org.mule.runtime.core.transformer.compression.GZipCompressTransformer;
import org.mule.runtime.core.transformer.compression.GZipUncompressTransformer;
import org.mule.runtime.core.transformer.encryption.DecryptionTransformer;
import org.mule.runtime.core.transformer.encryption.EncryptionTransformer;
import org.mule.runtime.core.transformer.simple.AddAttachmentTransformer;
import org.mule.runtime.core.transformer.simple.AddFlowVariableTransformer;
import org.mule.runtime.core.transformer.simple.AddPropertyTransformer;
import org.mule.runtime.core.transformer.simple.AddSessionVariableTransformer;
import org.mule.runtime.core.transformer.simple.AutoTransformer;
import org.mule.runtime.core.transformer.simple.BeanToMap;
import org.mule.runtime.core.transformer.simple.ByteArrayToHexString;
import org.mule.runtime.core.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.transformer.simple.ByteArrayToSerializable;
import org.mule.runtime.core.transformer.simple.CombineCollectionsTransformer;
import org.mule.runtime.core.transformer.simple.CopyAttachmentsTransformer;
import org.mule.runtime.core.transformer.simple.CopyPropertiesTransformer;
import org.mule.runtime.core.transformer.simple.HexStringToByteArray;
import org.mule.runtime.core.transformer.simple.MapToBean;
import org.mule.runtime.core.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.transformer.simple.ObjectToString;
import org.mule.runtime.core.transformer.simple.ParseTemplateTransformer;
import org.mule.runtime.core.transformer.simple.RemoveAttachmentTransformer;
import org.mule.runtime.core.transformer.simple.RemoveFlowVariableTransformer;
import org.mule.runtime.core.transformer.simple.RemovePropertyTransformer;
import org.mule.runtime.core.transformer.simple.RemoveSessionVariableTransformer;
import org.mule.runtime.core.transformer.simple.SerializableToByteArray;
import org.mule.runtime.core.transformer.simple.SetPayloadMessageProcessor;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.runtime.core.util.store.InMemoryObjectStore;
import org.mule.runtime.core.util.store.ManagedObjectStore;
import org.mule.runtime.core.util.store.TextFileObjectStore;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

/**
 * This is the core namespace handler for Mule and configures all Mule configuration elements under the
 * <code>http://www.mulesoft.org/schema/mule/core/${version}</code> Namespace.
 */
public class MuleNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String PATTERNS_DEPRECATION_MESSAGE = "Patterns module is deprecated and will be removed in Mule 4.0.";
    public static final String VARIABLE_NAME_ATTRIBUTE = "variableName";
    public static final String PROPERTY_NAME_ATTRIBUTE = "propertyName";
    public static final String IDENTIFIER_PROPERTY = "identifier";

    @Override
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
        registerBeanDefinitionParser("custom-agent", new DefaultNameMuleOrphanDefinitionParser());
        registerBeanDefinitionParser("expression-language", new ExpressionLanguageDefinitionParser());
        registerBeanDefinitionParser("global-functions", new GlobalFunctionsDefintionParser("globalFunctionsString"));
        registerMuleBeanDefinitionParser("alias", new ChildMapEntryDefinitionParser("aliases")).addAlias("name", "key").addAlias("expression", "value");
        registerMuleBeanDefinitionParser("import", new ImportMapEntryDefinitionParser("import"));

        // Exception Strategies
        registerBeanDefinitionParser("default-exception-strategy", new ExceptionStrategyDefinitionParser(DefaultMessagingExceptionStrategy.class));
        registerBeanDefinitionParser("catch-exception-strategy", new ExceptionStrategyDefinitionParser(CatchMessagingExceptionStrategy.class));
        registerBeanDefinitionParser("rollback-exception-strategy", new ExceptionStrategyDefinitionParser(RollbackMessagingExceptionStrategy.class));
        registerBeanDefinitionParser("on-redelivery-attempts-exceeded", new ChildDefinitionParser("redeliveryExceeded", RedeliveryExceeded.class));
        registerBeanDefinitionParser("choice-exception-strategy", new ExceptionStrategyDefinitionParser(ChoiceMessagingExceptionStrategy.class));
        registerMuleBeanDefinitionParser("exception-strategy", new ReferenceExceptionStrategyDefinitionParser());
        registerBeanDefinitionParser("custom-exception-strategy", new ExceptionStrategyDefinitionParser(null));
        registerBeanDefinitionParser("commit-transaction", new ExceptionTXFilterDefinitionParser("commitTxFilter"));
        registerBeanDefinitionParser("rollback-transaction", new ExceptionTXFilterDefinitionParser("rollbackTxFilter"));

        // Reconnection Strategies
        registerBeanDefinitionParser("reconnect", new RetryPolicyDefinitionParser(SimpleRetryPolicyTemplate.class));
        registerBeanDefinitionParser("reconnect-forever", new RetryPolicyDefinitionParser(RetryForeverPolicyTemplate.class));
        registerBeanDefinitionParser("reconnect-custom-strategy", new RetryPolicyDefinitionParser());
        registerBeanDefinitionParser("reconnect-notifier", new RetryNotifierDefinitionParser(ConnectNotifier.class));
        registerBeanDefinitionParser("reconnect-custom-notifier", new RetryNotifierDefinitionParser());

        // Queue Store
        registerMuleBeanDefinitionParser("queue-store", new ParentDefinitionParser()).addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "queue-store");
        registerMuleBeanDefinitionParser("custom-queue-store", new QueueStoreDefinitionParser()).addIgnored("name");
        registerBeanDefinitionParser("default-in-memory-queue-store", new QueueStoreDefinitionParser(DefaultMemoryQueueStoreFactoryBean.class));
        registerBeanDefinitionParser("default-persistent-queue-store", new QueueStoreDefinitionParser(DefaultPersistentQueueStoreFactoryBean.class));
        registerBeanDefinitionParser("simple-in-memory-queue-store", new QueueStoreDefinitionParser(SimpleMemoryQueueStoreFactoryBean.class));
        registerBeanDefinitionParser("file-queue-store", new QueueStoreDefinitionParser(FileQueueStoreFactoryBean.class));
        
        registerBeanDefinitionParser("pooling-profile", new PoolingProfileDefinitionParser());
        registerBeanDefinitionParser("queue-profile", new ChildDefinitionParser("queueProfile", QueueProfileFactoryBean.class));
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

        registerBeanDefinitionParser("custom-transformer", new TransformerMessageProcessorDefinitionParser());
        registerBeanDefinitionParser("auto-transformer", new TransformerMessageProcessorDefinitionParser(AutoTransformer.class));
        registerMuleBeanDefinitionParser("set-property", new MessageProcessorDefinitionParser(AddPropertyTransformer.class)).addAlias(PROPERTY_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
        registerMuleBeanDefinitionParser("remove-property", new MessageProcessorDefinitionParser(RemovePropertyTransformer.class)).addAlias(PROPERTY_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
        registerBeanDefinitionParser("copy-properties", new MessageProcessorDefinitionParser(CopyPropertiesTransformer.class));
        registerMuleBeanDefinitionParser("set-variable", new MessageProcessorDefinitionParser(AddFlowVariableTransformer.class)).addAlias(VARIABLE_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
        registerMuleBeanDefinitionParser("remove-variable", new MessageProcessorDefinitionParser(RemoveFlowVariableTransformer.class)).addAlias(VARIABLE_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
        registerMuleBeanDefinitionParser("set-session-variable", new MessageProcessorDefinitionParser(AddSessionVariableTransformer.class)).addAlias(VARIABLE_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
        registerMuleBeanDefinitionParser("remove-session-variable", new MessageProcessorDefinitionParser(RemoveSessionVariableTransformer.class)).addAlias(VARIABLE_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
        registerBeanDefinitionParser("set-attachment", new MessageProcessorDefinitionParser(AddAttachmentTransformer.class));
        registerBeanDefinitionParser("remove-attachment", new MessageProcessorDefinitionParser(RemoveAttachmentTransformer.class));
        registerBeanDefinitionParser("copy-attachments", new MessageProcessorDefinitionParser(CopyAttachmentsTransformer.class));

        registerMuleBeanDefinitionParser("expression-transformer", new ExpressionTransformerDefinitionParser(
                ExpressionTransformer.class));

        registerBeanDefinitionParser("return-argument", new ChildDefinitionParser("argument", ExpressionArgument.class));

        registerBeanDefinitionParser("bean-builder-transformer", new TransformerMessageProcessorDefinitionParser(BeanBuilderTransformer.class));

        final ChildDefinitionParser beanPropertyParser = new ChildDefinitionParser("argument", ExpressionArgument.class);
        beanPropertyParser.addAlias("property-name", "name");
        registerBeanDefinitionParser("bean-property", beanPropertyParser);

        registerBeanDefinitionParser("base64-encoder-transformer", new TransformerMessageProcessorDefinitionParser(Base64Encoder.class));
        registerBeanDefinitionParser("base64-decoder-transformer", new TransformerMessageProcessorDefinitionParser(Base64Decoder.class));

        registerBeanDefinitionParser("xml-entity-encoder-transformer", new TransformerMessageProcessorDefinitionParser(XmlEntityEncoder.class));
        registerBeanDefinitionParser("xml-entity-decoder-transformer", new TransformerMessageProcessorDefinitionParser(XmlEntityDecoder.class));
        registerBeanDefinitionParser("gzip-compress-transformer", new TransformerMessageProcessorDefinitionParser(GZipCompressTransformer.class));
        registerBeanDefinitionParser("gzip-uncompress-transformer", new TransformerMessageProcessorDefinitionParser(GZipUncompressTransformer.class));
        registerBeanDefinitionParser("encrypt-transformer", new TransformerMessageProcessorDefinitionParser(EncryptionTransformer.class));
        registerBeanDefinitionParser("decrypt-transformer", new TransformerMessageProcessorDefinitionParser(DecryptionTransformer.class));
        registerBeanDefinitionParser("byte-array-to-hex-string-transformer", new TransformerMessageProcessorDefinitionParser(ByteArrayToHexString.class));
        registerBeanDefinitionParser("hex-string-to-byte-array-transformer", new TransformerMessageProcessorDefinitionParser(HexStringToByteArray.class));

        registerBeanDefinitionParser("byte-array-to-object-transformer", new TransformerMessageProcessorDefinitionParser(ByteArrayToObject.class));
        registerBeanDefinitionParser("object-to-byte-array-transformer", new TransformerMessageProcessorDefinitionParser(ObjectToByteArray.class));
        registerBeanDefinitionParser("object-to-string-transformer", new TransformerMessageProcessorDefinitionParser(ObjectToString.class));
        registerBeanDefinitionParser("byte-array-to-serializable-transformer", new TransformerMessageProcessorDefinitionParser(ByteArrayToSerializable.class));
        registerBeanDefinitionParser("serializable-to-byte-array-transformer", new TransformerMessageProcessorDefinitionParser(SerializableToByteArray.class));
        registerBeanDefinitionParser("byte-array-to-string-transformer", new TransformerMessageProcessorDefinitionParser(ObjectToString.class));
        registerBeanDefinitionParser("string-to-byte-array-transformer", new TransformerMessageProcessorDefinitionParser(ObjectToByteArray.class));
        registerBeanDefinitionParser("parse-template", new MessageProcessorDefinitionParser(ParseTemplateTransformer.class));
        registerBeanDefinitionParser("set-payload", new MessageProcessorDefinitionParser(SetPayloadMessageProcessor.class));

        registerBeanDefinitionParser("append-string-transformer", new TransformerMessageProcessorDefinitionParser(StringAppendTransformer.class));

        registerBeanDefinitionParser("map-to-bean-transformer", new TransformerMessageProcessorDefinitionParser(MapToBean.class));
        registerBeanDefinitionParser("bean-to-map-transformer", new TransformerMessageProcessorDefinitionParser(BeanToMap.class));

        registerMuleBeanDefinitionParser("combine-collections-transformer", new TransformerMessageProcessorDefinitionParser(CombineCollectionsTransformer.class)).addIgnored("name");
        
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
        registerBeanDefinitionParser("outbound-endpoint", new ChildEndpointDefinitionParser(OutboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("custom-transaction", new TransactionDefinitionParser());
        registerBeanDefinitionParser("xa-transaction", new XaTransactionDefinitionParser(XaTransactionFactory.class));
        registerBeanDefinitionParser("idempotent-redelivery-policy", new ChildDefinitionParser("redeliveryPolicy", IdempotentRedeliveryPolicy.class));

        // Message Processors
        registerMuleBeanDefinitionParser("processor", new ParentDefinitionParser()).addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "messageProcessor");
        registerMuleBeanDefinitionParser("custom-processor", new MessageProcessorDefinitionParser()).addIgnored("name");
        registerBeanDefinitionParser("processor-chain", new MessageProcessorChainDefinitionParser());
        registerBeanDefinitionParser("sub-flow", new MuleOrphanDefinitionParser(SubflowMessageProcessorChainFactoryBean.class, false));
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
                new CheckRequiredAttributesWhenNoChildren(new String[][]{new String[]{"target"}}, "enrich", "http://www.mulesoft.org/schema/mule/core"))
            .addCollection("enrichExpressionPairs");
        registerMuleBeanDefinitionParser("enrich", new ChildDefinitionParser("enrichExpressionPair",
            EnrichExpressionPair.class));

        registerBeanDefinitionParser("async", new AsyncMessageProcessorsDefinitionParser());
        registerBeanDefinitionParser("transactional", new ChildDefinitionParser("messageProcessor",
            TransactionalMessageProcessorsFactoryBean.class));
        registerMuleBeanDefinitionParser("logger", new ChildDefinitionParser("messageProcessor",
            LoggerMessageProcessor.class));
        registerMuleBeanDefinitionParser("expression-component",
            new ExpressionComponentDefintionParser("messageProcessor", ExpressionLanguageComponent.class)).registerPreProcessor(
            new CheckExclusiveAttributeAndText("file"));

        // Message Sources
        registerBeanDefinitionParser("custom-source", new ChildDefinitionParser("messageSource", null, MessageSource.class));
        registerBeanDefinitionParser("composite-source", new ChildDefinitionParser("messageSource", CompositeMessageSourceFactoryBean.class));


        registerBeanDefinitionParser("poll", new ChildEndpointDefinitionParser(PollingMessageSourceFactoryBean.class));
        registerBeanDefinitionParser("fixed-frequency-scheduler", new ChildDefinitionParser("schedulerFactory", FixedFrequencySchedulerFactory.class));


        // Poll overrides
        registerBeanDefinitionParser("watermark", new ChildDefinitionParser("override", WatermarkFactoryBean.class));

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

        // Flow Constructs
        registerBeanDefinitionParser("flow", new FlowDefinitionParser());
        registerBeanDefinitionParser("flow-ref", new FlowRefDefinitionParser());
        
        // Processing Strategies
        registerMuleBeanDefinitionParser("asynchronous-processing-strategy",
            new OrphanDefinitionParser(AsynchronousProcessingStrategy.class, false)).addMapping(
            "poolExhaustedAction", ThreadingProfile.POOL_EXHAUSTED_ACTIONS).addIgnored("name");
        registerMuleBeanDefinitionParser("queued-asynchronous-processing-strategy",
            new OrphanDefinitionParser(QueuedAsynchronousProcessingStrategy.class, false)).addMapping(
            "poolExhaustedAction", ThreadingProfile.POOL_EXHAUSTED_ACTIONS).addIgnored("name");
        registerMuleBeanDefinitionParser("thread-per-processor-processing-strategy",
            new OrphanDefinitionParser(ThreadPerProcessorProcessingStrategy.class, false)).addMapping(
            "poolExhaustedAction", ThreadingProfile.POOL_EXHAUSTED_ACTIONS).addIgnored("name");
        registerMuleBeanDefinitionParser("queued-thread-per-processor-processing-strategy",
            new OrphanDefinitionParser(QueuedThreadPerProcessorProcessingStrategy.class, false)).addMapping(
            "poolExhaustedAction", ThreadingProfile.POOL_EXHAUSTED_ACTIONS).addIgnored("name");
        registerMuleBeanDefinitionParser("non-blocking-processing-strategy",
            new OrphanDefinitionParser(NonBlockingProcessingStrategy.class, false)).addMapping(
            "poolExhaustedAction", ThreadingProfile.POOL_EXHAUSTED_ACTIONS).addIgnored("name");
        registerMuleBeanDefinitionParser("custom-processing-strategy", new OrphanDefinitionParser(false)).addIgnored(
            "name");

        // Components
        registerBeanDefinitionParser("component", new ComponentDelegatingDefinitionParser(DefaultJavaComponent.class));
        registerBeanDefinitionParser("pooled-component", new ComponentDelegatingDefinitionParser(PooledJavaComponent.class));

        registerMuleBeanDefinitionParser("binding", new BindingDefinitionParser("interfaceBinding", DefaultInterfaceBinding.class));

        // Simple Components
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
        registerBeanDefinitionParser("custom-object-store",new ChildDefinitionParser("store", null));
        registerBeanDefinitionParser("spring-object-store",(BeanDefinitionParser)new ParentDefinitionParser().addAlias("ref", "store"));
        registerBeanDefinitionParser("managed-store", new ChildDefinitionParser("store",ManagedObjectStore.class));

        // Routing: Intercepting Message Processors
        registerMuleBeanDefinitionParser("idempotent-message-filter", new MessageFilterDefinitionParser(IdempotentMessageFilter.class));
        registerMuleBeanDefinitionParser("idempotent-secure-hash-message-filter", new MessageFilterDefinitionParser(IdempotentSecureHashMessageFilter.class));
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
        registerMuleBeanDefinitionParser("foreach", new ChildDefinitionParser("messageProcessor", Foreach.class)).addAlias("collection", "collectionExpression");

        // Routing: Routing Message Processors

        // Routing: Conditional Routers
        registerBeanDefinitionParser("choice", new ChildDefinitionParser("messageProcessor", ChoiceRouterFactoryBean.class));
        registerBeanDefinitionParser("when", (ChildDefinitionParser)new ChildDefinitionParser("route", MessageProcessorFilterPairFactoryBean.class).registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
            "expression"}, new String[]{"{http://www.mulesoft.org/schema/mule/core}abstractFilterType"})));
        registerBeanDefinitionParser("otherwise", new ChildDefinitionParser("defaultRoute", MessageProcessorFilterPairFactoryBean.class));

        registerBeanDefinitionParser("all", new ChildDefinitionParser("messageProcessor", MulticastingRouter.class));
        registerBeanDefinitionParser("scatter-gather", new ChildDefinitionParser("messageProcessor", ScatterGatherRouterFactoryBean.class));
        registerBeanDefinitionParser("custom-aggregation-strategy", new AggregationStrategyDefinitionParser());
        registerBeanDefinitionParser("recipient-list", new ChildDefinitionParser("messageProcessor", ExpressionRecipientList.class));

        registerBeanDefinitionParser("request-reply", new ChildDefinitionParser("messageProcessor", SimpleAsyncRequestReplyRequester.class));
        registerBeanDefinitionParser("first-successful", new ChildDefinitionParser("messageProcessor", FirstSuccessful.class));
        registerBeanDefinitionParser("until-successful", new ChildDefinitionParser("messageProcessor", UntilSuccessful.class));
        registerBeanDefinitionParser("round-robin", new ChildDefinitionParser("messageProcessor", RoundRobin.class));
        registerBeanDefinitionParser("dynamic-round-robin", new RouterDefinitionParser(DynamicRoundRobin.class));
        registerBeanDefinitionParser("dynamic-first-successful", new RouterDefinitionParser(DynamicFirstSuccessful.class));
        registerBeanDefinitionParser("dynamic-all", new RouterDefinitionParser(DynamicAll.class));
        registerMuleBeanDefinitionParser("custom-route-resolver", new ParentDefinitionParser())
                .addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "dynamicRouteResolver");
        registerMuleBeanDefinitionParser("dead-letter-queue", new ParentDefinitionParser()).addAlias("messageProcessor", "deadLetterQueue");

        registerBeanDefinitionParser("custom-router", new ChildDefinitionParser("messageProcessor"));

        //Message Info Mappings
        registerBeanDefinitionParser("expression-message-info-mapping", new ChildDefinitionParser("messageInfoMapping", ExpressionMessageInfoMapping.class));
        registerBeanDefinitionParser("custom-message-info-mapping", new ChildDefinitionParser("messageInfoMapping"));

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
        registerMuleBeanDefinitionParser("add-message-property", new TypedPropertyMapEntryDefinitionParser("addTypedProperties")).addCollection("addTypedProperties");
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
        registerMuleBeanDefinitionParser("secret-key-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", SecretKeyEncryptionStrategy.class)).registerPreProcessor(
                new CheckExclusiveAttributes(new String[][] {new String[] {"key"}, new String[] {"keyFactory-ref"}}));
        registerBeanDefinitionParser("encryption-security-filter", new SecurityFilterDefinitionParser(MuleEncryptionEndpointSecurityFilter.class));
        registerBeanDefinitionParser("custom-security-filter", new SecurityFilterDefinitionParser());
        registerBeanDefinitionParser("username-password-filter", new SecurityFilterDefinitionParser(UsernamePasswordAuthenticationFilter.class));
        
        //Interceptors
        registerMuleBeanDefinitionParser("interceptor-stack", new InterceptorStackDefinitionParser());
        registerBeanDefinitionParser("custom-interceptor", new InterceptorDefinitionParser());
        registerBeanDefinitionParser("timer-interceptor", new InterceptorDefinitionParser(TimerInterceptor.class));
        registerBeanDefinitionParser("logging-interceptor", new InterceptorDefinitionParser(LoggingInterceptor.class));
    }
}
