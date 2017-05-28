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
import org.mule.runtime.config.spring.factories.MessageProcessorFilterPairFactoryBean;
import org.mule.runtime.config.spring.factories.QueueProfileFactoryBean;
import org.mule.runtime.config.spring.factories.ScatterGatherRouterFactoryBean;
import org.mule.runtime.config.spring.factories.SchedulingMessageSourceFactoryBean;
import org.mule.runtime.config.spring.factories.SubflowMessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.TryProcessorFactoryBean;
import org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildEmbeddedDefinitionParser;
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
import org.mule.runtime.config.spring.parsers.specific.ComponentDelegatingDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ConfigurationDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.DefaultNameMuleOrphanDefinitionParser;
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
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorWithDataTypeDefinitionParser;
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
import org.mule.runtime.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SimpleComponentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SplitterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.StaticComponentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransactionManagerDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransformerMessageProcessorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TypedPropertyMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.XaTransactionDefinitionParser;
import org.mule.runtime.config.spring.util.SpringBeanLookup;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.model.resolvers.ArrayEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.CallableEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.runtime.core.api.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.runtime.core.api.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.ReflectionEntryPointResolver;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.runtime.core.component.SimpleCallableJavaComponent;
import org.mule.runtime.core.component.simple.EchoComponent;
import org.mule.runtime.core.component.simple.LogComponent;
import org.mule.runtime.core.component.simple.NullComponent;
import org.mule.runtime.core.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.el.ExpressionLanguageComponent;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.core.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.runtime.core.exception.ErrorHandler;
import org.mule.runtime.core.exception.OnErrorContinueHandler;
import org.mule.runtime.core.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.exception.RedeliveryExceeded;
import org.mule.runtime.core.expression.ExpressionConfig;
import org.mule.runtime.core.expression.transformers.BeanBuilderTransformer;
import org.mule.runtime.core.expression.transformers.ExpressionArgument;
import org.mule.runtime.core.expression.transformers.ExpressionTransformer;
import org.mule.runtime.core.interceptor.LoggingInterceptor;
import org.mule.runtime.core.interceptor.TimerInterceptor;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.internal.transformer.simple.ObjectToString;
import org.mule.runtime.core.object.PrototypeObjectFactory;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.processor.InvokerMessageProcessor;
import org.mule.runtime.core.processor.simple.AddFlowVariableProcessor;
import org.mule.runtime.core.processor.simple.AddPropertyProcessor;
import org.mule.runtime.core.processor.simple.RemoveFlowVariableProcessor;
import org.mule.runtime.core.processor.simple.RemovePropertyProcessor;
import org.mule.runtime.core.processor.simple.SetPayloadMessageProcessor;
import org.mule.runtime.core.retry.notifiers.ConnectNotifier;
import org.mule.runtime.core.retry.policies.RetryForeverPolicyTemplate;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.routing.Splitter;
import org.mule.runtime.core.routing.FirstSuccessful;
import org.mule.runtime.core.routing.Foreach;
import org.mule.runtime.core.routing.IdempotentMessageValidator;
import org.mule.runtime.core.routing.IdempotentSecureHashMessageValidator;
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
import org.mule.runtime.core.routing.outbound.MulticastingRouter;
import org.mule.runtime.core.routing.requestreply.SimpleAsyncRequestReplyRequester;
import org.mule.runtime.core.security.PasswordBasedEncryptionStrategy;
import org.mule.runtime.core.security.SecretKeyEncryptionStrategy;
import org.mule.runtime.core.security.UsernamePasswordAuthenticationFilter;
import org.mule.runtime.core.security.filters.MuleEncryptionEndpointSecurityFilter;
import org.mule.runtime.core.source.scheduler.schedule.CronScheduler;
import org.mule.runtime.core.source.scheduler.schedule.FixedFrequencyScheduler;
import org.mule.runtime.core.transaction.XaTransactionFactory;
import org.mule.runtime.core.transformer.codec.Base64Decoder;
import org.mule.runtime.core.transformer.codec.Base64Encoder;
import org.mule.runtime.core.transformer.codec.XmlEntityDecoder;
import org.mule.runtime.core.transformer.codec.XmlEntityEncoder;
import org.mule.runtime.core.transformer.compression.GZipCompressTransformer;
import org.mule.runtime.core.transformer.compression.GZipUncompressTransformer;
import org.mule.runtime.core.transformer.encryption.DecryptionTransformer;
import org.mule.runtime.core.transformer.encryption.EncryptionTransformer;
import org.mule.runtime.core.transformer.simple.AutoTransformer;
import org.mule.runtime.core.transformer.simple.BeanToMap;
import org.mule.runtime.core.transformer.simple.ByteArrayToHexString;
import org.mule.runtime.core.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.transformer.simple.ByteArrayToSerializable;
import org.mule.runtime.core.transformer.simple.CombineCollectionsTransformer;
import org.mule.runtime.core.transformer.simple.CopyPropertiesProcessor;
import org.mule.runtime.core.transformer.simple.HexStringToByteArray;
import org.mule.runtime.core.transformer.simple.MapToBean;
import org.mule.runtime.core.transformer.simple.ParseTemplateTransformer;
import org.mule.runtime.core.transformer.simple.SerializableToByteArray;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;

/**
 * This is the core namespace handler for Mule and configures all Mule configuration elements under the
 * <code>http://www.mulesoft.org/schema/mule/core/${version}</code> Namespace.
 */
public class MuleNamespaceHandler extends AbstractMuleNamespaceHandler {

  public static final String PATTERNS_DEPRECATION_MESSAGE = "Patterns module is deprecated and will be removed in Mule 4.0.";
  public static final String VARIABLE_NAME_ATTRIBUTE = "variableName";
  public static final String PROPERTY_NAME_ATTRIBUTE = "propertyName";
  public static final String IDENTIFIER_PROPERTY = "identifier";

  @Override
  public void init() {
    registerIgnoredElement("mule");
    registerIgnoredElement("description");

    // Common elements
    registerBeanDefinitionParser("configuration", new ConfigurationDefinitionParser());
    registerBeanDefinitionParser("global-property", new GlobalPropertyDefinitionParser());
    registerBeanDefinitionParser("custom-agent", new DefaultNameMuleOrphanDefinitionParser());
    registerBeanDefinitionParser("expression-language", new ExpressionLanguageDefinitionParser());
    registerBeanDefinitionParser("global-functions", new GlobalFunctionsDefintionParser("globalFunctionsString"));
    registerMuleBeanDefinitionParser("alias", new ChildMapEntryDefinitionParser("aliases")).addAlias("name", "key")
        .addAlias("expression", "value");
    registerMuleBeanDefinitionParser("import", new ImportMapEntryDefinitionParser("import"));

    // Exception Strategies
    registerBeanDefinitionParser("on-error-continue",
                                 new ExceptionStrategyDefinitionParser(OnErrorContinueHandler.class));
    registerBeanDefinitionParser("on-error-propagate",
                                 new ExceptionStrategyDefinitionParser(OnErrorPropagateHandler.class));
    registerBeanDefinitionParser("on-redelivery-attempts-exceeded",
                                 new ChildDefinitionParser("redeliveryExceeded", RedeliveryExceeded.class));
    registerBeanDefinitionParser("error-handler",
                                 new ExceptionStrategyDefinitionParser(ErrorHandler.class));
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
    registerMuleBeanDefinitionParser("queue-store", new ParentDefinitionParser())
        .addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "queue-store");
    registerMuleBeanDefinitionParser("custom-queue-store", new QueueStoreDefinitionParser()).addIgnored("name");
    registerBeanDefinitionParser("default-in-memory-queue-store",
                                 new QueueStoreDefinitionParser(DefaultMemoryQueueStoreFactoryBean.class));
    registerBeanDefinitionParser("default-persistent-queue-store",
                                 new QueueStoreDefinitionParser(DefaultPersistentQueueStoreFactoryBean.class));

    registerBeanDefinitionParser("pooling-profile", new PoolingProfileDefinitionParser());
    registerBeanDefinitionParser("queue-profile", new ChildDefinitionParser("queueProfile", QueueProfileFactoryBean.class));
    registerMuleBeanDefinitionParser("notifications", new NamedDefinitionParser(MuleProperties.OBJECT_NOTIFICATION_MANAGER))
        .addAlias("dynamic", "notificationDynamic");
    registerBeanDefinitionParser("notification", new NotificationDefinitionParser());
    registerBeanDefinitionParser("disable-notification", new NotificationDisableDefinitionParser());
    registerMuleBeanDefinitionParser("notification-listener",
                                     new ChildDefinitionParser("allListenerSubscriptionPair", ListenerSubscriptionPair.class))
                                         .addAlias("ref", "listener").addReference("listener");

    // Transformer elements

    registerMuleBeanDefinitionParser("transformer", new ParentDefinitionParser())
        .addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "messageProcessor");

    registerBeanDefinitionParser("custom-transformer", new TransformerMessageProcessorDefinitionParser());
    registerBeanDefinitionParser("auto-transformer", new TransformerMessageProcessorDefinitionParser(AutoTransformer.class));
    registerMuleBeanDefinitionParser("set-property",
                                     new MessageProcessorWithDataTypeDefinitionParser(AddPropertyProcessor.class))
                                         .addAlias(PROPERTY_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
    registerMuleBeanDefinitionParser("remove-property", new MessageProcessorDefinitionParser(RemovePropertyProcessor.class))
        .addAlias(PROPERTY_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
    registerBeanDefinitionParser("copy-properties", new MessageProcessorDefinitionParser(CopyPropertiesProcessor.class));
    registerMuleBeanDefinitionParser("set-variable",
                                     new MessageProcessorWithDataTypeDefinitionParser(AddFlowVariableProcessor.class))
                                         .addAlias(VARIABLE_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
    registerMuleBeanDefinitionParser("remove-variable", new MessageProcessorDefinitionParser(RemoveFlowVariableProcessor.class))
        .addAlias(VARIABLE_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);

    registerMuleBeanDefinitionParser("expression-transformer",
                                     new ExpressionTransformerDefinitionParser(ExpressionTransformer.class));

    registerBeanDefinitionParser("return-argument", new ChildDefinitionParser("argument", ExpressionArgument.class));

    registerBeanDefinitionParser("bean-builder-transformer",
                                 new TransformerMessageProcessorDefinitionParser(BeanBuilderTransformer.class));

    final ChildDefinitionParser beanPropertyParser = new ChildDefinitionParser("argument", ExpressionArgument.class);
    beanPropertyParser.addAlias("property-name", "name");
    registerBeanDefinitionParser("bean-property", beanPropertyParser);

    registerBeanDefinitionParser("base64-encoder-transformer",
                                 new TransformerMessageProcessorDefinitionParser(Base64Encoder.class));
    registerBeanDefinitionParser("base64-decoder-transformer",
                                 new TransformerMessageProcessorDefinitionParser(Base64Decoder.class));

    registerBeanDefinitionParser("xml-entity-encoder-transformer",
                                 new TransformerMessageProcessorDefinitionParser(XmlEntityEncoder.class));
    registerBeanDefinitionParser("xml-entity-decoder-transformer",
                                 new TransformerMessageProcessorDefinitionParser(XmlEntityDecoder.class));
    registerBeanDefinitionParser("gzip-compress-transformer",
                                 new TransformerMessageProcessorDefinitionParser(GZipCompressTransformer.class));
    registerBeanDefinitionParser("gzip-uncompress-transformer",
                                 new TransformerMessageProcessorDefinitionParser(GZipUncompressTransformer.class));
    registerBeanDefinitionParser("encrypt-transformer",
                                 new TransformerMessageProcessorDefinitionParser(EncryptionTransformer.class));
    registerBeanDefinitionParser("decrypt-transformer",
                                 new TransformerMessageProcessorDefinitionParser(DecryptionTransformer.class));
    registerBeanDefinitionParser("byte-array-to-hex-string-transformer",
                                 new TransformerMessageProcessorDefinitionParser(ByteArrayToHexString.class));
    registerBeanDefinitionParser("hex-string-to-byte-array-transformer",
                                 new TransformerMessageProcessorDefinitionParser(HexStringToByteArray.class));

    registerBeanDefinitionParser("byte-array-to-object-transformer",
                                 new TransformerMessageProcessorDefinitionParser(ByteArrayToObject.class));
    registerBeanDefinitionParser("object-to-byte-array-transformer",
                                 new TransformerMessageProcessorDefinitionParser(ObjectToByteArray.class));
    registerBeanDefinitionParser("object-to-string-transformer",
                                 new TransformerMessageProcessorDefinitionParser(ObjectToString.class));
    registerBeanDefinitionParser("byte-array-to-serializable-transformer",
                                 new TransformerMessageProcessorDefinitionParser(ByteArrayToSerializable.class));
    registerBeanDefinitionParser("serializable-to-byte-array-transformer",
                                 new TransformerMessageProcessorDefinitionParser(SerializableToByteArray.class));
    registerBeanDefinitionParser("byte-array-to-string-transformer",
                                 new TransformerMessageProcessorDefinitionParser(ObjectToString.class));
    registerBeanDefinitionParser("string-to-byte-array-transformer",
                                 new TransformerMessageProcessorDefinitionParser(ObjectToByteArray.class));
    registerBeanDefinitionParser("parse-template", new MessageProcessorDefinitionParser(ParseTemplateTransformer.class));
    registerBeanDefinitionParser("set-payload",
                                 new MessageProcessorWithDataTypeDefinitionParser(SetPayloadMessageProcessor.class));

    registerBeanDefinitionParser("append-string-transformer",
                                 new TransformerMessageProcessorDefinitionParser(StringAppendTransformer.class));

    registerBeanDefinitionParser("map-to-bean-transformer", new TransformerMessageProcessorDefinitionParser(MapToBean.class));
    registerBeanDefinitionParser("bean-to-map-transformer", new TransformerMessageProcessorDefinitionParser(BeanToMap.class));

    registerMuleBeanDefinitionParser("combine-collections-transformer",
                                     new TransformerMessageProcessorDefinitionParser(CombineCollectionsTransformer.class))
                                         .addIgnored("name");

    // Transaction Managers
    registerBeanDefinitionParser("custom-transaction-manager", new TransactionManagerDefinitionParser());

    registerBeanDefinitionParser("custom-transaction", new TransactionDefinitionParser());
    registerBeanDefinitionParser("xa-transaction", new XaTransactionDefinitionParser(XaTransactionFactory.class));
    registerBeanDefinitionParser("redelivery-policy",
                                 new ChildDefinitionParser("redeliveryPolicy", IdempotentRedeliveryPolicy.class));

    // Message Processors
    registerMuleBeanDefinitionParser("processor", new ParentDefinitionParser())
        .addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "messageProcessor");
    registerMuleBeanDefinitionParser("custom-processor", new MessageProcessorDefinitionParser()).addIgnored("name");
    registerBeanDefinitionParser("processor-chain", new MessageProcessorChainDefinitionParser());
    registerBeanDefinitionParser("sub-flow",
                                 new MuleOrphanDefinitionParser(SubflowMessageProcessorChainFactoryBean.class, false));
    registerBeanDefinitionParser("response", new ResponseDefinitionParser());
    registerMuleBeanDefinitionParser("message-filter", new MessageFilterDefinitionParser());
    registerMuleBeanDefinitionParser("invoke", new MessageProcessorDefinitionParser(InvokerMessageProcessor.class))
        .addAlias("method", "methodName").addAlias("methodArguments", "argumentExpressionsString")
        .addAlias("methodArgumentTypes", "ArgumentTypes");
    registerMuleBeanDefinitionParser("enricher", new MessageEnricherDefinitionParser("messageProcessor", MessageEnricher.class))
        .addIgnored("source").addIgnored("target")
        .registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[] {"source", "target"}, new String[] {"enrich"}))
        .registerPreProcessor(new CheckRequiredAttributesWhenNoChildren(new String[][] {new String[] {"target"}}, "enrich",
                                                                        "http://www.mulesoft.org/schema/mule/core"))
        .addCollection("enrichExpressionPairs");
    registerMuleBeanDefinitionParser("enrich", new ChildDefinitionParser("enrichExpressionPair", EnrichExpressionPair.class));

    registerBeanDefinitionParser("async", new AsyncMessageProcessorsDefinitionParser());
    registerBeanDefinitionParser("transactional",
                                 new ChildDefinitionParser("messageProcessor", TryProcessorFactoryBean.class));
    registerMuleBeanDefinitionParser("logger", new ChildDefinitionParser("messageProcessor", LoggerMessageProcessor.class));
    registerMuleBeanDefinitionParser("expression-component",
                                     new ExpressionComponentDefintionParser("messageProcessor",
                                                                            ExpressionLanguageComponent.class))
                                                                                .registerPreProcessor(new CheckExclusiveAttributeAndText("file"));

    // Message Sources
    registerBeanDefinitionParser("custom-source", new ChildDefinitionParser("messageSource", null, MessageSource.class));
    registerBeanDefinitionParser("composite-source",
                                 new ChildDefinitionParser("messageSource", CompositeMessageSourceFactoryBean.class));

    registerBeanDefinitionParser("scheduler", new ChildEmbeddedDefinitionParser(SchedulingMessageSourceFactoryBean.class));
    registerBeanDefinitionParser("fixed-frequency",
                                 new ChildDefinitionParser("scheduler", FixedFrequencyScheduler.class));
    registerBeanDefinitionParser("cron",
                                 new ChildDefinitionParser("scheduler", CronScheduler.class));

    registerBeanDefinitionParser("entry-point-resolver-set",
                                 new ChildDefinitionParser("entryPointResolverSet", DefaultEntryPointResolverSet.class));
    registerBeanDefinitionParser("legacy-entry-point-resolver-set",
                                 new ChildDefinitionParser("entryPointResolverSet", LegacyEntryPointResolverSet.class));
    registerBeanDefinitionParser("custom-entry-point-resolver-set", new ChildDefinitionParser("entryPointResolverSet"));

    registerBeanDefinitionParser("custom-entry-point-resolver", new ChildDefinitionParser("entryPointResolver"));
    registerBeanDefinitionParser("callable-entry-point-resolver",
                                 new ChildDefinitionParser("entryPointResolver", CallableEntryPointResolver.class));
    registerMuleBeanDefinitionParser("property-entry-point-resolver",
                                     new ChildDefinitionParser("entryPointResolver",
                                                               MethodHeaderPropertyEntryPointResolver.class))
                                                                   .addAlias("property", "methodProperty");
    registerBeanDefinitionParser("method-entry-point-resolver",
                                 new ChildDefinitionParser("entryPointResolver", ExplicitMethodEntryPointResolver.class));
    registerBeanDefinitionParser("reflection-entry-point-resolver",
                                 new ChildDefinitionParser("entryPointResolver", ReflectionEntryPointResolver.class));
    registerBeanDefinitionParser("no-arguments-entry-point-resolver",
                                 new ChildDefinitionParser("entryPointResolver", NoArgumentsEntryPointResolver.class));
    registerBeanDefinitionParser("array-entry-point-resolver",
                                 new ChildDefinitionParser("entryPointResolver", ArrayEntryPointResolver.class));
    registerMuleBeanDefinitionParser("include-entry-point", new ParentDefinitionParser());
    registerMuleBeanDefinitionParser("exclude-entry-point", new ParentDefinitionParser()).addAlias("method", "ignoredMethod");
    registerMuleBeanDefinitionParser("exclude-object-methods", new IgnoreObjectMethodsDefinitionParser());

    // Flow Constructs
    registerBeanDefinitionParser("flow", new FlowDefinitionParser());
    registerBeanDefinitionParser("flow-ref", new FlowRefDefinitionParser());

    // Processing Strategies
    registerMuleBeanDefinitionParser("custom-processing-strategy", new OrphanDefinitionParser(false)).addIgnored("name");

    // Components
    registerBeanDefinitionParser("component", new ComponentDelegatingDefinitionParser(DefaultJavaComponent.class));
    registerBeanDefinitionParser("pooled-component", new ComponentDelegatingDefinitionParser(PooledJavaComponent.class));

    // Simple Components
    registerBeanDefinitionParser("log-component",
                                 new SimpleComponentDefinitionParser(SimpleCallableJavaComponent.class, LogComponent.class));
    registerBeanDefinitionParser("null-component",
                                 new SimpleComponentDefinitionParser(SimpleCallableJavaComponent.class, NullComponent.class));
    registerBeanDefinitionParser("static-component", new StaticComponentDefinitionParser());
    registerIgnoredElement("return-data"); // Handled by StaticComponentDefinitionParser

    // We need to use DefaultJavaComponent for the echo component because some tests invoke EchoComponent with method name and
    // therefore we need an entry point resolver
    registerBeanDefinitionParser("echo-component",
                                 new SimpleComponentDefinitionParser(DefaultJavaComponent.class, EchoComponent.class));

    // Object Factories
    registerBeanDefinitionParser("singleton-object",
                                 new ObjectFactoryDefinitionParser(SingletonObjectFactory.class, "objectFactory"));
    registerBeanDefinitionParser("prototype-object",
                                 new ObjectFactoryDefinitionParser(PrototypeObjectFactory.class, "objectFactory"));
    registerBeanDefinitionParser("spring-object", new ObjectFactoryDefinitionParser(SpringBeanLookup.class, "objectFactory"));

    // Life-cycle Adapters Factories
    registerBeanDefinitionParser("custom-lifecycle-adapter-factory", new ChildDefinitionParser("lifecycleAdapterFactory"));

    // Routing: Intercepting Message Processors
    registerMuleBeanDefinitionParser("idempotent-message-validator",
                                     new MessageFilterDefinitionParser(IdempotentMessageValidator.class));
    registerMuleBeanDefinitionParser("idempotent-secure-hash-message-validator",
                                     new MessageFilterDefinitionParser(IdempotentSecureHashMessageValidator.class));
    registerBeanDefinitionParser("wire-tap", new InboundRouterDefinitionParser(WireTap.class));
    registerBeanDefinitionParser("custom-aggregator", new AggregatorDefinitionParser());
    registerBeanDefinitionParser("collection-aggregator", new AggregatorDefinitionParser(SimpleCollectionAggregator.class));
    registerBeanDefinitionParser("message-chunk-aggregator", new AggregatorDefinitionParser(MessageChunkAggregator.class));
    registerBeanDefinitionParser("resequencer", new InboundRouterDefinitionParser(Resequencer.class));
    registerBeanDefinitionParser("splitter", new SplitterDefinitionParser(Splitter.class));
    registerBeanDefinitionParser("message-chunk-splitter", new SplitterDefinitionParser(MessageChunkSplitter.class));
    registerBeanDefinitionParser("custom-splitter", new SplitterDefinitionParser());
    registerMuleBeanDefinitionParser("foreach", new ChildDefinitionParser("messageProcessor", Foreach.class))
        .addAlias("collection", "collectionExpression");

    // Routing: Routing Message Processors

    // Routing: Conditional Routers
    registerBeanDefinitionParser("choice", new ChildDefinitionParser("messageProcessor", ChoiceRouterFactoryBean.class));
    registerBeanDefinitionParser("when",
                                 (ChildDefinitionParser) new ChildDefinitionParser("route",
                                                                                   MessageProcessorFilterPairFactoryBean.class)
                                                                                       .registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[] {
                                                                                           "expression"}, new String[] {
                                                                                               "{http://www.mulesoft.org/schema/mule/core}abstractFilterType"})));
    registerBeanDefinitionParser("otherwise",
                                 new ChildDefinitionParser("defaultRoute", MessageProcessorFilterPairFactoryBean.class));

    registerBeanDefinitionParser("all", new ChildDefinitionParser("messageProcessor", MulticastingRouter.class));
    registerBeanDefinitionParser("scatter-gather",
                                 new ChildDefinitionParser("messageProcessor", ScatterGatherRouterFactoryBean.class));
    registerBeanDefinitionParser("custom-aggregation-strategy", new AggregationStrategyDefinitionParser());

    registerBeanDefinitionParser("request-reply",
                                 new ChildDefinitionParser("messageProcessor", SimpleAsyncRequestReplyRequester.class));
    registerBeanDefinitionParser("first-successful", new ChildDefinitionParser("messageProcessor", FirstSuccessful.class));
    registerBeanDefinitionParser("until-successful", new ChildDefinitionParser("messageProcessor", UntilSuccessful.class));
    registerBeanDefinitionParser("round-robin", new ChildDefinitionParser("messageProcessor", RoundRobin.class));
    registerMuleBeanDefinitionParser("dead-letter-queue", new ParentDefinitionParser()).addAlias("messageProcessor",
                                                                                                 "deadLetterQueue");

    registerBeanDefinitionParser("custom-router", new ChildDefinitionParser("messageProcessor"));

    // Message Info Mappings
    registerBeanDefinitionParser("custom-message-info-mapping", new ChildDefinitionParser("messageInfoMapping"));

    // Common Filters
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

    // Utils / Standard Types
    registerMuleBeanDefinitionParser("properties", new ChildMapDefinitionParser("properties")).addCollection("properties");
    registerMuleBeanDefinitionParser("property", new ChildMapEntryDefinitionParser("properties")).addCollection("properties");
    registerMuleBeanDefinitionParser("add-message-properties", new ChildMapDefinitionParser("addProperties"))
        .addCollection("addProperties");
    registerMuleBeanDefinitionParser("add-message-property", new TypedPropertyMapEntryDefinitionParser("addTypedProperties"))
        .addCollection("addTypedProperties");
    registerMuleBeanDefinitionParser("rename-message-property", new ChildMapEntryDefinitionParser("renameProperties"))
        .addCollection("renameProperties");
    registerBeanDefinitionParser("delete-message-property",
                                 new ChildListEntryDefinitionParser("deleteProperties", ChildMapEntryDefinitionParser.KEY));
    registerMuleBeanDefinitionParser("jndi-provider-properties", new ChildMapDefinitionParser("jndiProviderProperties"))
        .addCollection("jndiProviderProperties");
    registerMuleBeanDefinitionParser("jndi-provider-property", new ChildMapEntryDefinitionParser("jndiProviderProperties"))
        .addCollection("jndiProviderProperties");
    registerBeanDefinitionParser("environment", new ChildMapDefinitionParser("environment"));
    registerBeanDefinitionParser("expression", new ChildDefinitionParser("expression", ExpressionConfig.class));

    // Security
    registerMuleBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER))
        .addIgnored("type").addIgnored("name");
    registerBeanDefinitionParser("custom-security-provider", new NameTransferDefinitionParser("providers"));
    registerMuleBeanDefinitionParser("custom-encryption-strategy", new NameTransferDefinitionParser("encryptionStrategies"))
        .addAlias("strategy", "encryptionStrategy");
    registerBeanDefinitionParser("password-encryption-strategy",
                                 new ChildDefinitionParser("encryptionStrategy", PasswordBasedEncryptionStrategy.class));
    registerMuleBeanDefinitionParser("secret-key-encryption-strategy",
                                     new ChildDefinitionParser("encryptionStrategy", SecretKeyEncryptionStrategy.class))
                                         .registerPreProcessor(new CheckExclusiveAttributes(new String[][] {new String[] {"key"},
                                             new String[] {"keyFactory-ref"}}));
    registerBeanDefinitionParser("encryption-security-filter",
                                 new SecurityFilterDefinitionParser(MuleEncryptionEndpointSecurityFilter.class));
    registerBeanDefinitionParser("custom-security-filter", new SecurityFilterDefinitionParser());
    registerBeanDefinitionParser("username-password-filter",
                                 new SecurityFilterDefinitionParser(UsernamePasswordAuthenticationFilter.class));

    // Interceptors
    registerMuleBeanDefinitionParser("interceptor-stack", new InterceptorStackDefinitionParser());
    registerBeanDefinitionParser("custom-interceptor", new InterceptorDefinitionParser());
    registerBeanDefinitionParser("timer-interceptor", new InterceptorDefinitionParser(TimerInterceptor.class));
    registerBeanDefinitionParser("logging-interceptor", new InterceptorDefinitionParser(LoggingInterceptor.class));
  }
}
