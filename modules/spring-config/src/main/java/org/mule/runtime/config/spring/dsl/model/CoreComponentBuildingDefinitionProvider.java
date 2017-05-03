/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.model;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_ACTIVE;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_IDLE;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_WAIT;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_POOL_EXHAUSTED_ACTION;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_POOL_INITIALISATION_POLICY;
import static org.mule.runtime.api.config.PoolingProfile.POOL_EXHAUSTED_ACTIONS;
import static org.mule.runtime.api.config.PoolingProfile.POOL_INITIALISATION_POLICIES;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSING_STRATEGY_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSING_STRATEGY_FACTORY_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROTOTYPE_OBJECT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SINGLETON_OBJECT_ELEMENT;
import static org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.util.ClassUtils.instanciateClass;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildMapConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedReference;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromTextContent;
import static org.mule.runtime.dsl.api.component.CommonTypeConverters.stringToClassConverter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromMapEntryType;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTES_STREAMING_MAX_BUFFER_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTE_STREAMING_BUFFER_INCREMENT_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_BYTE_STREAMING_BUFFER_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_BUFFER_INCREMENT_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_BUFFER_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_MAX_BUFFER_SIZE;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_BYTE_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.POOLING_PROFILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.DataUnit;
import org.mule.runtime.config.spring.MuleConfigurationConfigurator;
import org.mule.runtime.config.spring.NotificationConfig;
import org.mule.runtime.config.spring.ServerNotificationManagerConfigurator;
import org.mule.runtime.config.spring.dsl.processor.AddVariablePropertyConfigurator;
import org.mule.runtime.config.spring.dsl.processor.CustomSecurityFilterObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.EncryptionSecurityFilterObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.EnvironmentPropertyObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.ExplicitMethodEntryPointResolverObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.MessageEnricherObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.MethodEntryPoint;
import org.mule.runtime.config.spring.dsl.processor.NoArgumentsEntryPointResolverObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.RetryPolicyTemplateObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.TransformerConfigurator;
import org.mule.runtime.config.spring.dsl.processor.UsernamePasswordFilterObjectFactory;
import org.mule.runtime.config.spring.dsl.spring.ComponentObjectFactory;
import org.mule.runtime.config.spring.dsl.spring.ConfigurableInstanceFactory;
import org.mule.runtime.config.spring.dsl.spring.ConfigurableObjectFactory;
import org.mule.runtime.config.spring.dsl.spring.ExcludeDefaultObjectMethods;
import org.mule.runtime.config.spring.dsl.spring.PooledComponentObjectFactory;
import org.mule.runtime.config.spring.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.ChoiceRouterFactoryBean;
import org.mule.runtime.config.spring.factories.FlowRefFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorFilterPairFactoryBean;
import org.mule.runtime.config.spring.factories.ModuleOperationMessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.QueueProfileFactoryBean;
import org.mule.runtime.config.spring.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.ScatterGatherRouterFactoryBean;
import org.mule.runtime.config.spring.factories.SchedulingMessageSourceFactoryBean;
import org.mule.runtime.config.spring.factories.SubflowMessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.TryProcessorFactoryBean;
import org.mule.runtime.config.spring.factories.streaming.InMemoryCursorIteratorProviderObjectFactory;
import org.mule.runtime.config.spring.factories.streaming.InMemoryCursorStreamProviderObjectFactory;
import org.mule.runtime.config.spring.factories.streaming.NullCursorIteratorProviderObjectFactory;
import org.mule.runtime.config.spring.factories.streaming.NullCursorStreamProviderObjectFactory;
import org.mule.runtime.config.spring.util.SpringBeanLookup;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.component.LifecycleAdapterFactory;
import org.mule.runtime.core.api.config.ConfigurationExtension;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.model.EntryPointResolver;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.api.model.resolvers.ArrayEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.CallableEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.runtime.core.api.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.runtime.core.api.model.resolvers.ReflectionEntryPointResolver;
import org.mule.runtime.core.api.object.ObjectFactory;
import org.mule.runtime.core.api.processor.AbstractProcessor;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.RetryNotifier;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.source.polling.PeriodicScheduler;
import org.mule.runtime.core.api.store.QueueStoreObjectFactory;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.runtime.core.component.simple.EchoComponent;
import org.mule.runtime.core.component.simple.LogComponent;
import org.mule.runtime.core.component.simple.NullComponent;
import org.mule.runtime.core.component.simple.StaticComponent;
import org.mule.runtime.core.config.QueueProfile;
import org.mule.runtime.core.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.el.ExpressionLanguageComponent;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.exception.ErrorHandler;
import org.mule.runtime.core.exception.OnErrorContinueHandler;
import org.mule.runtime.core.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.exception.RedeliveryExceeded;
import org.mule.runtime.core.expression.ExpressionConfig;
import org.mule.runtime.core.expression.transformers.AbstractExpressionTransformer;
import org.mule.runtime.core.expression.transformers.BeanBuilderTransformer;
import org.mule.runtime.core.expression.transformers.ExpressionArgument;
import org.mule.runtime.core.expression.transformers.ExpressionTransformer;
import org.mule.runtime.core.interceptor.LoggingInterceptor;
import org.mule.runtime.core.interceptor.TimerInterceptor;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.internal.transformer.simple.ObjectToString;
import org.mule.runtime.core.object.PrototypeObjectFactory;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.runtime.core.processor.AnnotatedProcessor;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.processor.InvokerMessageProcessor;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.core.processor.simple.AbstractAddVariablePropertyProcessor;
import org.mule.runtime.core.processor.simple.AddFlowVariableProcessor;
import org.mule.runtime.core.processor.simple.AddPropertyProcessor;
import org.mule.runtime.core.processor.simple.RemoveFlowVariableProcessor;
import org.mule.runtime.core.processor.simple.RemovePropertyProcessor;
import org.mule.runtime.core.processor.simple.SetPayloadMessageProcessor;
import org.mule.runtime.core.retry.notifiers.ConnectNotifier;
import org.mule.runtime.core.routing.AggregationStrategy;
import org.mule.runtime.core.routing.ChoiceRouter;
import org.mule.runtime.core.routing.CollectionSplitter;
import org.mule.runtime.core.routing.ExpressionSplitter;
import org.mule.runtime.core.routing.FirstSuccessful;
import org.mule.runtime.core.routing.Foreach;
import org.mule.runtime.core.routing.IdempotentMessageFilter;
import org.mule.runtime.core.routing.IdempotentSecureHashMessageFilter;
import org.mule.runtime.core.routing.MapSplitter;
import org.mule.runtime.core.routing.MessageChunkAggregator;
import org.mule.runtime.core.routing.MessageChunkSplitter;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.routing.MessageProcessorFilterPair;
import org.mule.runtime.core.routing.Resequencer;
import org.mule.runtime.core.routing.RoundRobin;
import org.mule.runtime.core.routing.ScatterGatherRouter;
import org.mule.runtime.core.routing.SimpleCollectionAggregator;
import org.mule.runtime.core.routing.UntilSuccessful;
import org.mule.runtime.core.routing.WireTap;
import org.mule.runtime.core.routing.filters.EqualsFilter;
import org.mule.runtime.core.routing.filters.ExceptionTypeFilter;
import org.mule.runtime.core.routing.filters.ExpressionFilter;
import org.mule.runtime.core.routing.filters.MessagePropertyFilter;
import org.mule.runtime.core.routing.filters.NotWildcardFilter;
import org.mule.runtime.core.routing.filters.PayloadTypeFilter;
import org.mule.runtime.core.routing.filters.RegExFilter;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.routing.filters.logic.AndFilter;
import org.mule.runtime.core.routing.filters.logic.NotFilter;
import org.mule.runtime.core.routing.filters.logic.OrFilter;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;
import org.mule.runtime.core.routing.requestreply.SimpleAsyncRequestReplyRequester;
import org.mule.runtime.core.source.StartableCompositeMessageSource;
import org.mule.runtime.core.source.scheduler.DefaultSchedulerMessageSource;
import org.mule.runtime.core.source.scheduler.schedule.FixedFrequencyScheduler;
import org.mule.runtime.core.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.streaming.object.CursorIteratorProviderFactory;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.core.transaction.TransactionType;
import org.mule.runtime.core.transaction.XaTransactionFactory;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.transformer.codec.Base64Decoder;
import org.mule.runtime.core.transformer.codec.Base64Encoder;
import org.mule.runtime.core.transformer.codec.XmlEntityDecoder;
import org.mule.runtime.core.transformer.codec.XmlEntityEncoder;
import org.mule.runtime.core.transformer.compression.GZipCompressTransformer;
import org.mule.runtime.core.transformer.compression.GZipUncompressTransformer;
import org.mule.runtime.core.transformer.encryption.AbstractEncryptionTransformer;
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
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.queue.QueueStore;
import org.mule.runtime.core.util.store.TextFileObjectStore;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.dsl.api.component.TypeConverter;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * {@link ComponentBuildingDefinition} definitions for the components provided by the core runtime.
 *
 * @since 4.0
 */
public class CoreComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  private static final String MESSAGE_PROCESSORS = "messageProcessors";
  private static final String NAME = "name";
  private static final String EXCEPTION_STRATEGY = "exception-strategy";
  private static final String ON_ERROR_CONTINUE = "on-error-continue";
  private static final String WHEN = "when";
  private static final String ON_ERROR_PROPAGATE = "on-error-propagate";
  private static final String DEFAULT_EXCEPTION_STRATEGY = "default-exception-strategy";
  private static final String NAME_EXCEPTION_STRATEGY_ATTRIBUTE = "globalName";
  private static final String CUSTOM_EXCEPTION_STRATEGY = "custom-exception-strategy";
  private static final String ERROR_HANDLER = "error-handler";
  private static final String SET_PAYLOAD = "set-payload";
  private static final String LOGGER = "logger";
  private static final String PROCESSOR_CHAIN = "processor-chain";
  private static final String PROCESSOR = "processor";
  private static final String TRANSFORMER = "transformer";
  private static final String FILTER = "filter";
  private static final String CUSTOM_PROCESSOR = "custom-processor";
  private static final String CLASS_ATTRIBUTE = "class";
  private static final String SUB_FLOW = "sub-flow";
  private static final String RESPONSE = "response";
  private static final String MESSAGE_FILTER = "message-filter";
  private static final String FLOW = "flow";
  private static final String FLOW_REF = "flow-ref";
  private static final String EXCEPTION_LISTENER_ATTRIBUTE = "exceptionListener";
  private static final String SCATTER_GATHER = "scatter-gather";
  private static final String WIRE_TAP = "wire-tap";
  private static final String ENRICHER = "enricher";
  private static final String ASYNC = "async";
  private static final String TRY = "try";
  private static final String UNTIL_SUCCESSFUL = "until-successful";
  private static final String FOREACH = "foreach";
  private static final String FIRST_SUCCESSFUL = "first-successful";
  private static final String ROUND_ROBIN = "round-robin";
  private static final String CHOICE = "choice";
  private static final String OTHERWISE = "otherwise";
  private static final String ALL = "all";
  private static final String SCHEDULER = "scheduler";
  private static final String REQUEST_REPLY = "request-reply";
  private static final String ERROR_TYPE = "errorType";
  private static final String TYPE = "type";
  private static final String TX_ACTION = "transactionalAction";
  private static final String TX_TYPE = "transactionType";

  private Map<String, Integer> regExFlagsMapping = ImmutableMap.<String, Integer>builder()
      .put("CANON_EQ", Integer.valueOf(Pattern.CANON_EQ))
      .put("CASE_INSENSITIVE", Integer.valueOf(Pattern.CASE_INSENSITIVE))
      .put("DOTALL", Integer.valueOf(Pattern.DOTALL))
      .put("MULTILINE", Integer.valueOf(Pattern.MULTILINE))
      .put("UNICODE_CASE", Integer.valueOf(Pattern.UNICODE_CASE))
      .build();

  private static ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(CORE_PREFIX);
  private ComponentBuildingDefinition.Builder transactionManagerBaseDefinition;

  @Override
  public void init() {
    transactionManagerBaseDefinition = baseDefinition.copy();
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {

    LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();

    AttributeDefinition messageProcessorListAttributeDefinition =
        fromChildCollectionConfiguration(Processor.class).build();
    ComponentBuildingDefinition.Builder exceptionStrategyBaseBuilder =
        baseDefinition.copy().withSetterParameterDefinition(MESSAGE_PROCESSORS, messageProcessorListAttributeDefinition)
            .withSetterParameterDefinition("globalName", fromSimpleParameter(NAME).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(EXCEPTION_STRATEGY).withTypeDefinition(fromType(Object.class))
            .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build()).build());
    componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy().withIdentifier(ON_ERROR_CONTINUE)
        .withTypeDefinition(fromType(OnErrorContinueHandler.class))
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .withSetterParameterDefinition(WHEN, fromSimpleParameter(WHEN).build())
        .withSetterParameterDefinition(ERROR_TYPE, fromSimpleParameter(TYPE).build())
        .asPrototype().build());
    componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy().withIdentifier(ON_ERROR_PROPAGATE)
        .withTypeDefinition(fromType(OnErrorPropagateHandler.class))
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .withSetterParameterDefinition(WHEN, fromSimpleParameter(WHEN).build())
        .withSetterParameterDefinition(ERROR_TYPE, fromSimpleParameter(TYPE).build())
        .withSetterParameterDefinition("maxRedeliveryAttempts", fromSimpleParameter("maxRedeliveryAttempts").build())
        .withSetterParameterDefinition("redeliveryExceeded", fromChildConfiguration(RedeliveryExceeded.class).build())
        .asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("on-redelivery-attempts-exceeded")
        .withTypeDefinition(fromType(RedeliveryExceeded.class))
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asScope().build());
    componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy().withIdentifier(DEFAULT_EXCEPTION_STRATEGY)
        .withTypeDefinition(fromType(DefaultMessagingExceptionStrategy.class))
        .withSetterParameterDefinition(NAME_EXCEPTION_STRATEGY_ATTRIBUTE, fromSimpleParameter(NAME).build())
        .withSetterParameterDefinition("stopMessageProcessing", fromSimpleParameter("stopMessageProcessing").build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .withSetterParameterDefinition("commitTxFilter", fromChildConfiguration(WildcardFilter.class).build())
        .withSetterParameterDefinition("rollbackTxFilter", fromChildConfiguration(WildcardFilter.class).build()).asPrototype()
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier("commit-transaction").withTypeDefinition(fromType(WildcardFilter.class))
            .withSetterParameterDefinition("pattern", fromSimpleParameter("exception-pattern").build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier("rollback-transaction").withTypeDefinition(fromType(NotWildcardFilter.class))
            .withSetterParameterDefinition("pattern", fromSimpleParameter("exception-pattern").build()).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(CUSTOM_EXCEPTION_STRATEGY)
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(ERROR_HANDLER)
        .withTypeDefinition(fromType(ErrorHandler.class))
        .withSetterParameterDefinition("globalName", fromSimpleParameter(NAME).build())
        .withSetterParameterDefinition("exceptionListeners",
                                       fromChildCollectionConfiguration(MessagingExceptionHandler.class).build())
        .asPrototype()
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(SET_PAYLOAD).withTypeDefinition(fromType(SetPayloadMessageProcessor.class))
            .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
            .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
            .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build()).build());

    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(LOGGER).withTypeDefinition(fromType(LoggerMessageProcessor.class))
            .withSetterParameterDefinition("message", fromSimpleParameter("message").build())
            .withSetterParameterDefinition("category", fromSimpleParameter("category").build())
            .withSetterParameterDefinition("level", fromSimpleParameter("level").build()).build());

    componentBuildingDefinitions
        .add(getSetVariablePropertyBaseBuilder(getAddFlowVariableTransformerInstanceFactory(AddPropertyProcessor.class),
                                               AddPropertyProcessor.class,
                                               newBuilder()
                                                   .withKey("identifier")
                                                   .withAttributeDefinition(fromSimpleParameter("propertyName").build())
                                                   .build(),
                                               newBuilder()
                                                   .withKey("value")
                                                   .withAttributeDefinition(fromSimpleParameter("value").build())
                                                   .build())
                                                       .withIdentifier("set-property")
                                                       .withTypeDefinition(fromType(AddPropertyProcessor.class))
                                                       .build());
    componentBuildingDefinitions
        .add(getSetVariablePropertyBaseBuilder(getAddFlowVariableTransformerInstanceFactory(AddFlowVariableProcessor.class),
                                               AddFlowVariableProcessor.class,
                                               newBuilder()
                                                   .withKey("identifier")
                                                   .withAttributeDefinition(fromSimpleParameter("variableName").build())
                                                   .build(),
                                               newBuilder()
                                                   .withKey("value")
                                                   .withAttributeDefinition(fromSimpleParameter("value").build())
                                                   .build())
                                                       .withIdentifier("set-variable")
                                                       .withTypeDefinition(fromType(AddFlowVariableProcessor.class))
                                                       .build());
    componentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("remove-property")
        .withTypeDefinition(fromType(RemovePropertyProcessor.class))
        .withSetterParameterDefinition("identifier", fromSimpleParameter("propertyName").build())
        .build());
    componentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("remove-variable")
        .withTypeDefinition(fromType(RemoveFlowVariableProcessor.class))
        .withSetterParameterDefinition("identifier", fromSimpleParameter("variableName").build())
        .build());
    componentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("copy-properties")
        .withTypeDefinition(fromType(CopyPropertiesProcessor.class))
        .withSetterParameterDefinition("propertyName", fromSimpleParameter("propertyName").build())
        .build());

    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(PROCESSOR).withTypeDefinition(fromType(AbstractProcessor.class)).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(TRANSFORMER).withTypeDefinition(fromType(Transformer.class)).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(FILTER).withTypeDefinition(fromType(Object.class)).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(CUSTOM_PROCESSOR)
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE)).asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(PROCESSOR_CHAIN)
        .withTypeDefinition(fromType(Processor.class)).withObjectFactoryType(MessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asPrototype().build());
    addModuleOperationChainParser(componentBuildingDefinitions);
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(SUB_FLOW)
        .withTypeDefinition(fromType(Processor.class)).withObjectFactoryType(SubflowMessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build()).asPrototype().build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(RESPONSE).withTypeDefinition(fromType(ResponseMessageProcessorAdapter.class))
            .withObjectFactoryType(ResponseMessageProcessorsFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(MESSAGE_FILTER).withTypeDefinition(fromType(MessageFilter.class))
            .withConstructorParameterDefinition(fromChildConfiguration(Filter.class).build())
            .withConstructorParameterDefinition(fromSimpleParameter("throwOnUnaccepted").withDefaultValue(false).build())
            .withConstructorParameterDefinition(fromSimpleReferenceParameter("onUnaccepted").build()).asPrototype().build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(FLOW).withTypeDefinition(fromType(DefaultFlowBuilder.DefaultFlow.class))
            .withConstructorParameterDefinition(fromSimpleParameter(NAME).build())
            .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
            .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
            .withSetterParameterDefinition("messageSource", fromChildConfiguration(MessageSource.class).build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition(EXCEPTION_LISTENER_ATTRIBUTE,
                                           fromChildConfiguration(MessagingExceptionHandler.class).build())
            .withSetterParameterDefinition(PROCESSING_STRATEGY_FACTORY_ATTRIBUTE,
                                           fromSimpleReferenceParameter(PROCESSING_STRATEGY_ATTRIBUTE).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(FLOW_REF)
        .withTypeDefinition(fromType(AnnotatedProcessor.class))
        .withObjectFactoryType(FlowRefFactoryBean.class)
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(SCATTER_GATHER)
        .withTypeDefinition(fromType(ScatterGatherRouter.class)).withObjectFactoryType(ScatterGatherRouterFactoryBean.class)
        .withSetterParameterDefinition("parallel", fromSimpleParameter("parallel").build())
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("aggregationStrategy", fromChildConfiguration(AggregationStrategy.class).build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asScope().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(WIRE_TAP).withTypeDefinition(fromType(WireTap.class))
        .withSetterParameterDefinition("tap", fromChildConfiguration(Processor.class).build())
        .withSetterParameterDefinition("filter", fromChildConfiguration(Filter.class).build()).asScope().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(ENRICHER)
        .withObjectFactoryType(MessageEnricherObjectFactory.class).withTypeDefinition(fromType(MessageEnricher.class))
        .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(Processor.class).build())
        .withSetterParameterDefinition("enrichExpressionPairs",
                                       fromChildCollectionConfiguration(MessageEnricher.EnrichExpressionPair.class).build())
        .withSetterParameterDefinition("source", fromSimpleParameter("source").build())
        .withSetterParameterDefinition("target", fromSimpleParameter("target").build()).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("enrich")
        .withTypeDefinition(fromType(MessageEnricher.EnrichExpressionPair.class))
        .withConstructorParameterDefinition(fromSimpleParameter("source").build())
        .withConstructorParameterDefinition(fromSimpleParameter("target").build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(ASYNC).withTypeDefinition(fromType(AsyncDelegateMessageProcessor.class))
            .withObjectFactoryType(AsyncMessageProcessorsFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(TRY).withTypeDefinition(fromType(MessageProcessorChain.class))
            .withObjectFactoryType(TryProcessorFactoryBean.class)
            .withSetterParameterDefinition("exceptionListener", fromChildConfiguration(MessagingExceptionHandler.class).build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition(TX_ACTION, fromSimpleParameter(TX_ACTION).build())
            .withSetterParameterDefinition(TX_TYPE, fromSimpleParameter(TX_TYPE, getTransactionTypeConverter()).build()).build());

    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(UNTIL_SUCCESSFUL).withTypeDefinition(fromType(UntilSuccessful.class))
            .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("objectStore-ref").build())
            .withSetterParameterDefinition("maxRetries", fromSimpleParameter("maxRetries").withDefaultValue(5).build())
            .withSetterParameterDefinition("millisBetweenRetries", fromSimpleParameter("millisBetweenRetries").build())
            .withSetterParameterDefinition("secondsBetweenRetries", fromSimpleParameter("secondsBetweenRetries").build())
            .withSetterParameterDefinition("failureExpression", fromSimpleParameter("failureExpression").build())
            .withSetterParameterDefinition("ackExpression", fromSimpleParameter("ackExpression").build())
            .withSetterParameterDefinition("synchronous", fromSimpleParameter("synchronous").withDefaultValue(false).build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(FOREACH).withTypeDefinition(fromType(Foreach.class))
        .withSetterParameterDefinition("collectionExpression", fromSimpleParameter("collection").build())
        .withSetterParameterDefinition("batchSize", fromSimpleParameter("batchSize").build())
        .withSetterParameterDefinition("rootMessageVariableName", fromSimpleParameter("rootMessageVariableName").build())
        .withSetterParameterDefinition("counterVariableName", fromSimpleParameter("counterVariableName").build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(FIRST_SUCCESSFUL).withTypeDefinition(fromType(FirstSuccessful.class))
            .withSetterParameterDefinition("failureExpression", fromSimpleParameter("failureExpression").build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(ROUND_ROBIN).withTypeDefinition(fromType(RoundRobin.class))
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(CHOICE).withTypeDefinition(fromType(ChoiceRouter.class))
        .withObjectFactoryType(ChoiceRouterFactoryBean.class)
        .withSetterParameterDefinition("routes", fromChildCollectionConfiguration(MessageProcessorFilterPair.class).build())
        .withSetterParameterDefinition("defaultRoute", fromChildConfiguration(MessageProcessorFilterPair.class).build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(WHEN).withTypeDefinition(fromType(MessageProcessorFilterPair.class))
            .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(OTHERWISE).withTypeDefinition(fromType(MessageProcessorFilterPair.class))
            .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition("expression", fromFixedValue("true").build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(ALL).withTypeDefinition(fromType(MulticastingRouter.class))
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("message-chunk-splitter")
        .withTypeDefinition(fromType(MessageChunkSplitter.class))
        .withSetterParameterDefinition("messageSize", fromSimpleParameter("messageSize").build()).build());

    ComponentBuildingDefinition.Builder baseAggregatorDefinition = baseDefinition.copy()
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("failOnTimeout", fromSimpleParameter("failOnTimeout").build())
        .withSetterParameterDefinition("processedGroupsObjectStore",
                                       fromSimpleReferenceParameter("processed-groups-object-store-ref").build())
        .withSetterParameterDefinition("eventGroupsObjectStore",
                                       fromSimpleReferenceParameter("event-groups-object-store-ref").build())
        .withSetterParameterDefinition("persistentStores", fromSimpleParameter("persistentStores").build())
        .withSetterParameterDefinition("storePrefix", fromSimpleParameter("storePrefix").build());

    componentBuildingDefinitions.add(baseAggregatorDefinition.copy().withIdentifier("message-chunk-aggregator")
        .withTypeDefinition(fromType(MessageChunkAggregator.class)).build());

    componentBuildingDefinitions.add(baseAggregatorDefinition.copy().withIdentifier("collection-aggregator")
        .withTypeDefinition(fromType(SimpleCollectionAggregator.class))
        .build());

    componentBuildingDefinitions.add(baseAggregatorDefinition.copy().withIdentifier("resequencer")
        .withTypeDefinition(fromType(Resequencer.class))
        .build());

    componentBuildingDefinitions.add(baseAggregatorDefinition.copy().withIdentifier("custom-aggregator")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .build());


    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("collection-splitter")
        .withTypeDefinition(fromType(CollectionSplitter.class)).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("splitter")
        .withTypeDefinition(fromType(ExpressionSplitter.class))
        .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("map-splitter")
        .withTypeDefinition(fromType(MapSplitter.class))
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("custom-splitter")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(REQUEST_REPLY)
        .withTypeDefinition(fromType(SimpleAsyncRequestReplyRequester.class))
        .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(Processor.class).build())
        .withSetterParameterDefinition("messageSource", fromChildConfiguration(MessageSource.class).build())
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("storePrefix", fromSimpleParameter("storePrefix").build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(SCHEDULER)
        .withTypeDefinition(fromType(DefaultSchedulerMessageSource.class))
        .withObjectFactoryType(SchedulingMessageSourceFactoryBean.class)
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
        .withSetterParameterDefinition("scheduler", fromChildConfiguration(PeriodicScheduler.class).build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("fixed-frequency-scheduler")
        .withTypeDefinition(fromType(FixedFrequencyScheduler.class))
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
        .withSetterParameterDefinition("startDelay", fromSimpleParameter("startDelay").build())
        .withSetterParameterDefinition("timeUnit", fromSimpleParameter("timeUnit").build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(REDELIVERY_POLICY_ELEMENT_IDENTIFIER)
        .withTypeDefinition(fromType(IdempotentRedeliveryPolicy.class))
        .withSetterParameterDefinition("useSecureHash", fromSimpleParameter("useSecureHash").build())
        .withSetterParameterDefinition("messageDigestAlgorithm", fromSimpleParameter("messageDigestAlgorithm").build())
        .withSetterParameterDefinition("maxRedeliveryCount", fromSimpleParameter("maxRedeliveryCount").build())
        .withSetterParameterDefinition("idExpression", fromSimpleParameter("idExpression").build())
        .withSetterParameterDefinition("idExpression", fromSimpleParameter("idExpression").build())
        .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("object-store-ref").build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("composite-source")
        .withTypeDefinition(fromType(StartableCompositeMessageSource.class))
        .withSetterParameterDefinition("messageSources", fromChildCollectionConfiguration(MessageSource.class).build())
        .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build()).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("configuration")
        .withTypeDefinition(fromType(MuleConfiguration.class)).withObjectFactoryType(MuleConfigurationConfigurator.class)
        .withSetterParameterDefinition("defaultErrorHandlerName",
                                       fromSimpleParameter("defaultErrorHandler-ref").build())
        .withSetterParameterDefinition("defaultProcessingStrategy",
                                       fromSimpleReferenceParameter("defaultProcessingStrategy").build())
        .withSetterParameterDefinition("defaultResponseTimeout", fromSimpleParameter("defaultResponseTimeout").build())
        .withSetterParameterDefinition("maxQueueTransactionFilesSize",
                                       fromSimpleParameter("maxQueueTransactionFilesSize").build())
        .withSetterParameterDefinition("defaultTransactionTimeout", fromSimpleParameter("defaultTransactionTimeout").build())
        .withSetterParameterDefinition("shutdownTimeout", fromSimpleParameter("shutdownTimeout").build())
        .withSetterParameterDefinition("defaultTransactionTimeout", fromSimpleParameter("defaultTransactionTimeout").build())
        .withSetterParameterDefinition("useExtendedTransformations", fromSimpleParameter("useExtendedTransformations").build())
        .withSetterParameterDefinition("flowEndingWithOneWayEndpointReturnsNull",
                                       fromSimpleParameter("flowEndingWithOneWayEndpointReturnsNull").build())
        .withSetterParameterDefinition("enricherPropagatesSessionVariableChanges",
                                       fromSimpleParameter("enricherPropagatesSessionVariableChanges").build())
        .withSetterParameterDefinition("extensions", fromChildCollectionConfiguration(Object.class).build())
        .withSetterParameterDefinition("defaultObjectSerializer",
                                       fromSimpleReferenceParameter("defaultObjectSerializer-ref").build())
        .withSetterParameterDefinition("extensions", fromChildCollectionConfiguration(ConfigurationExtension.class).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("notifications")
        .withTypeDefinition(fromType(ServerNotificationManagerConfigurator.class))
        .withSetterParameterDefinition("notificationDynamic", fromSimpleParameter("dynamic").build())
        .withSetterParameterDefinition("enabledNotifications",
                                       fromChildCollectionConfiguration(NotificationConfig.EnabledNotificationConfig.class)
                                           .build())
        .withSetterParameterDefinition("disabledNotifications",
                                       fromChildCollectionConfiguration(NotificationConfig.DisabledNotificationConfig.class)
                                           .build())
        .withSetterParameterDefinition("notificationListeners",
                                       fromChildCollectionConfiguration(ListenerSubscriptionPair.class).build())
        .build());

    ComponentBuildingDefinition.Builder baseNotificationDefinition =
        baseDefinition.copy().withSetterParameterDefinition("interfaseName", fromSimpleParameter("interface").build())
            .withSetterParameterDefinition("eventName", fromSimpleParameter("event").build())
            .withSetterParameterDefinition("interfaceClass", fromSimpleParameter("interface-class").build())
            .withSetterParameterDefinition("eventClass", fromSimpleParameter("event-class").build());

    componentBuildingDefinitions.add(baseNotificationDefinition.copy()
        .withTypeDefinition(fromType(NotificationConfig.EnabledNotificationConfig.class)).withIdentifier("notification").build());

    componentBuildingDefinitions
        .add(baseNotificationDefinition.copy().withTypeDefinition(fromType(NotificationConfig.DisabledNotificationConfig.class))
            .withIdentifier("disable-notification").build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("notification-listener")
        .withTypeDefinition(fromType(ListenerSubscriptionPair.class))
        .withSetterParameterDefinition("listener", fromSimpleReferenceParameter("ref").build())
        .withSetterParameterDefinition("subscription", fromSimpleParameter("subscription").build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("username-password-filter")
        .withTypeDefinition(fromType(SecurityFilterMessageProcessor.class))
        .withObjectFactoryType(UsernamePasswordFilterObjectFactory.class)
        .withSetterParameterDefinition("username", fromSimpleParameter("username").build())
        .withSetterParameterDefinition("password", fromSimpleParameter("password").build())
        .withIgnoredConfigurationParameter(NAME)
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("custom-security-filter")
        .withTypeDefinition(fromType(SecurityFilterMessageProcessor.class))
        .withObjectFactoryType(CustomSecurityFilterObjectFactory.class)
        .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build())
        .withIgnoredConfigurationParameter(NAME)
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("encryption-security-filter")
        .withTypeDefinition(fromType(SecurityFilterMessageProcessor.class))
        .withObjectFactoryType(EncryptionSecurityFilterObjectFactory.class)
        .withConstructorParameterDefinition(fromSimpleReferenceParameter("strategy-ref").build())
        .withIgnoredConfigurationParameter(NAME)
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("queue-profile")
        .withTypeDefinition(fromType(QueueProfile.class))
        .withObjectFactoryType(QueueProfileFactoryBean.class)
        .withSetterParameterDefinition("maxOutstandingMessages", fromSimpleParameter("maxOutstandingMessages").build())
        .withSetterParameterDefinition("queueStore", fromChildConfiguration(QueueStore.class).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("redelivery-policy")
        .withTypeDefinition(fromType(IdempotentRedeliveryPolicy.class))
        .withSetterParameterDefinition("maxRedeliveryCount", fromSimpleParameter("maxRedeliveryCount").build())
        .withSetterParameterDefinition("useSecureHash", fromSimpleParameter("useSecureHash").build())
        .withSetterParameterDefinition("messageDigestAlgorithm", fromSimpleParameter("messageDigestAlgorithm").build())
        .withSetterParameterDefinition("idExpression", fromSimpleParameter("idExpression").build())
        .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("object-store-ref").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("custom-router")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("singleton-object")
        .withTypeDefinition(fromType(SingletonObjectFactory.class))
        .withConstructorParameterDefinition(fromSimpleParameter(CLASS_ATTRIBUTE).build())
        .withConstructorParameterDefinition(fromChildConfiguration(Map.class).withDefaultValue(new HashMap<>()).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("prototype-object")
        .withTypeDefinition(fromType(PrototypeObjectFactory.class))
        .withConstructorParameterDefinition(fromSimpleParameter(CLASS_ATTRIBUTE).build())
        .withConstructorParameterDefinition(fromChildConfiguration(Map.class).withDefaultValue(new HashMap<>()).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("pooling-profile")
        .withTypeDefinition(fromType(PoolingProfile.class))
        .withSetterParameterDefinition("maxActive", fromSimpleParameter("maxActive").build())
        .withSetterParameterDefinition("maxIdle", fromSimpleParameter("maxIdle").build())
        .withSetterParameterDefinition("exhaustedAction", fromSimpleParameter("exhaustedAction",
                                                                              PoolingProfile.POOL_EXHAUSTED_ACTIONS::get).build())
        .withSetterParameterDefinition("maxWait", fromSimpleParameter("maxWait").build())
        .withSetterParameterDefinition("evictionCheckIntervalMillis", fromSimpleParameter("evictionCheckIntervalMillis").build())
        .withSetterParameterDefinition("minEvictionMillis", fromSimpleParameter("minEvictionMillis").build())
        .withSetterParameterDefinition("disabled", fromSimpleParameter("disabled").build())
        .withSetterParameterDefinition("initialisationPolicy", fromSimpleParameter("initialisationPolicy",
                                                                                   PoolingProfile.POOL_INITIALISATION_POLICIES::get)
                                                                                       .build())
        .build());

    ComponentBuildingDefinition.Builder baseObjectStoreDefinition = baseDefinition.copy()
        .withSetterParameterDefinition("maxEntries", fromSimpleParameter("maxEntries").build())
        .withSetterParameterDefinition("entryTTL", fromSimpleParameter("entryTTL").build())
        .withSetterParameterDefinition("expirationInterval", fromSimpleParameter("expirationInterval").build())
        .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build());

    componentBuildingDefinitions.add(baseObjectStoreDefinition.copy().withIdentifier("simple-text-file-store")
        .withTypeDefinition(fromType(TextFileObjectStore.class))
        .withSetterParameterDefinition("directory", fromSimpleParameter("directory").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("custom-object-store")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("custom-queue-store")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .withIgnoredConfigurationParameter(NAME)
        .build());

    componentBuildingDefinitions.add(
                                     baseDefinition.copy().withIdentifier("expression-component")
                                         .withTypeDefinition(fromType(ExpressionLanguageComponent.class))
                                         .withSetterParameterDefinition("expression", fromTextContent().build())
                                         .withSetterParameterDefinition("expressionFile", fromSimpleParameter("file").build())
                                         .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("expression-component")
        .withTypeDefinition(fromType(ExpressionLanguageComponent.class))
        .withSetterParameterDefinition("expression", fromTextContent().build())
        .withSetterParameterDefinition("expressionFile", fromSimpleParameter("file").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("invoke")
        .withTypeDefinition(fromType(InvokerMessageProcessor.class))
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("methodName", fromSimpleParameter("method").build())
        .withSetterParameterDefinition("argumentExpressionsString",
                                       fromSimpleParameter("methodArguments").build())
        .withSetterParameterDefinition("object",
                                       fromSimpleReferenceParameter("object-ref").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("custom-source")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .asPrototype()
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("custom-agent")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .asPrototype()
        .build());

    componentBuildingDefinitions.addAll(getTransformersBuildingDefinitions());
    componentBuildingDefinitions.addAll(getComponentsDefinitions());
    componentBuildingDefinitions.addAll(getEntryPointResolversDefinitions());
    componentBuildingDefinitions.addAll(getStreamingDefinitions());
    componentBuildingDefinitions.addAll(getFiltersDefinitions());
    componentBuildingDefinitions.addAll(getReconnectionDefinitions());
    componentBuildingDefinitions.addAll(getTransactionDefinitions());
    componentBuildingDefinitions.addAll(getQueueStoreDefinitions());
    return componentBuildingDefinitions;
  }

  private TypeConverter<String, TransactionType> getTransactionTypeConverter() {
    return TransactionType::valueOf;
  }

  private List<ComponentBuildingDefinition> getFiltersDefinitions() {
    List<ComponentBuildingDefinition> definitions = new LinkedList<>();

    definitions.add(baseDefinition.copy()
        .withIdentifier("and-filter")
        .withTypeDefinition(fromType(AndFilter.class))
        .withConstructorParameterDefinition(fromChildCollectionConfiguration(Filter.class).build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("or-filter")
        .withTypeDefinition(fromType(OrFilter.class))
        .withConstructorParameterDefinition(fromChildCollectionConfiguration(Filter.class).build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("not-filter")
        .withTypeDefinition(fromType(NotFilter.class))
        .withConstructorParameterDefinition(fromChildConfiguration(Filter.class).build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("regex-filter")
        .withTypeDefinition(fromType(RegExFilter.class))
        .withSetterParameterDefinition("flags", fromSimpleParameter("flags", value -> {
          String flags = (String) value;
          int combinedFlags = 0;
          String[] flagStrings = StringUtils.split(flags, ',');
          for (String flagString : flagStrings) {
            Integer flag = regExFlagsMapping.get(flagString);
            if (flag == null) {
              String message =
                  format("Invalid flag '%1s'. Must be one of %2s", flagString, regExFlagsMapping.keySet().toString());
              throw new IllegalArgumentException(message);
            }
            combinedFlags = combinedFlags | flag.intValue();
          }
          return Integer.valueOf(combinedFlags);
        }).build())
        .withSetterParameterDefinition("pattern", fromSimpleParameter("pattern").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("exception-type-filter")
        .withTypeDefinition(fromType(ExceptionTypeFilter.class))
        .withConstructorParameterDefinition(fromSimpleParameter("expectedType", stringToClassConverter()).build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("message-property-filter")
        .withTypeDefinition(fromType(MessagePropertyFilter.class))
        .withConstructorParameterDefinition(fromSimpleParameter("pattern").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
        .withSetterParameterDefinition("scope", fromSimpleParameter("scope").build())
        .withSetterParameterDefinition("caseSensitive", fromSimpleParameter("caseSensitive").build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("payload-type-filter")
        .withTypeDefinition(fromType(PayloadTypeFilter.class))
        .withConstructorParameterDefinition(fromSimpleParameter("expectedType", stringToClassConverter()).build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("wildcard-filter")
        .withTypeDefinition(fromType(WildcardFilter.class))
        .withSetterParameterDefinition("caseSensitive", fromSimpleParameter("caseSensitive").build())
        .withSetterParameterDefinition("pattern", fromSimpleParameter("pattern").build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("equals-filter")
        .withTypeDefinition(fromType(EqualsFilter.class))
        .withSetterParameterDefinition("pattern", fromSimpleParameter("pattern").build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("expression-filter")
        .withTypeDefinition(fromType(ExpressionFilter.class))
        .withConstructorParameterDefinition(fromSimpleParameter("expression").build())
        .withSetterParameterDefinition("nullReturnsTrue", fromSimpleParameter("nullReturnsTrue").build())
        .build());

    definitions.add(baseDefinition.copy()
        .withIdentifier("custom-filter")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .asPrototype()
        .build());

    ComponentBuildingDefinition.Builder baseIdempotentMessageFilterDefinition = baseDefinition.copy()
        .withSetterParameterDefinition("idExpression", fromSimpleParameter("idExpression").build())
        .withSetterParameterDefinition("valueExpression", fromSimpleParameter("valueExpression").build())
        .withSetterParameterDefinition("storePrefix", fromSimpleParameter("storePrefix").build())
        .withSetterParameterDefinition("throwOnUnaccepted", fromSimpleParameter("throwOnUnaccepted").build())
        .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("objectStore-ref").build())
        .withSetterParameterDefinition("unacceptedMessageProcessor", fromSimpleReferenceParameter("onUnaccepted").build());

    definitions.add(baseIdempotentMessageFilterDefinition.copy()
        .withIdentifier("idempotent-message-filter")
        .withTypeDefinition(fromType(IdempotentMessageFilter.class))
        .build());

    definitions.add(baseIdempotentMessageFilterDefinition.copy()
        .withIdentifier("idempotent-secure-hash-message-filter")
        .withTypeDefinition(fromType(IdempotentSecureHashMessageFilter.class))
        .withSetterParameterDefinition("messageDigestAlgorithm", fromSimpleParameter("messageDigestAlgorithm").build())
        .build());

    return definitions;
  }

  private List<ComponentBuildingDefinition> getTransformersBuildingDefinitions() {
    List<ComponentBuildingDefinition> transformerComponentBuildingDefinitions = new ArrayList<>();
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(Base64Encoder.class)
        .withIdentifier("base64-encoder-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(Base64Decoder.class)
        .withIdentifier("base64-decoder-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(XmlEntityEncoder.class)
        .withIdentifier("xml-entity-encoder-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(XmlEntityDecoder.class)
        .withIdentifier("xml-entity-decoder-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(GZipCompressTransformer.class)
        .withIdentifier("gzip-compress-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(GZipUncompressTransformer.class)
        .withIdentifier("gzip-uncompress-transformer")
        .build());
    KeyAttributeDefinitionPair strategyParameterDefinition = newBuilder()
        .withKey("strategy")
        .withAttributeDefinition(fromSimpleReferenceParameter("strategy-ref").build())
        .build();
    transformerComponentBuildingDefinitions
        .add(getTransformerBaseBuilder(getEncryptionTransformerConfigurationFactory(EncryptionTransformer.class),
                                       EncryptionTransformer.class, strategyParameterDefinition)
                                           .withIdentifier("encrypt-transformer")
                                           .build());
    transformerComponentBuildingDefinitions
        .add(getTransformerBaseBuilder(getEncryptionTransformerConfigurationFactory(DecryptionTransformer.class),
                                       DecryptionTransformer.class, strategyParameterDefinition)
                                           .withIdentifier("decrypt-transformer")
                                           .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(ByteArrayToHexString.class)
        .withIdentifier("byte-array-to-hex-string-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(HexStringToByteArray.class)
        .withIdentifier("hex-string-to-byte-array-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(ByteArrayToObject.class)
        .withIdentifier("byte-array-to-object-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(ObjectToByteArray.class)
        .withIdentifier("object-to-byte-array-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(ObjectToString.class)
        .withIdentifier("object-to-string-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(ByteArrayToSerializable.class)
        .withIdentifier("byte-array-to-serializable-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(SerializableToByteArray.class)
        .withIdentifier("serializable-to-byte-array-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(ObjectToString.class)
        .withIdentifier("byte-array-to-string-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(ObjectToByteArray.class)
        .withIdentifier("string-to-byte-array-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("parse-template")
        .withTypeDefinition(fromType(ParseTemplateTransformer.class))
        .withSetterParameterDefinition("location", fromSimpleParameter("location").build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(AutoTransformer.class)
        .withIdentifier("auto-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(MapToBean.class)
        .withIdentifier("map-to-bean-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(BeanToMap.class)
        .withIdentifier("bean-to-map-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("combine-collections-transformer")
        .withTypeDefinition(fromType(CombineCollectionsTransformer.class))
        .asPrototype()
        .build());
    transformerComponentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("append-string-transformer")
        .withTypeDefinition(fromType(StringAppendTransformer.class))
        .withSetterParameterDefinition("message", fromSimpleParameter("message").build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(getCustomTransformerConfigurationFactory(),
                                                                          Transformer.class,
                                                                          newBuilder()
                                                                              .withKey("class")
                                                                              .withAttributeDefinition(fromSimpleParameter("class")
                                                                                  .build())
                                                                              .build())
                                                                                  .withTypeDefinition(fromConfigurationAttribute("class"))
                                                                                  .withIdentifier("custom-transformer")
                                                                                  .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(getBeanBuilderTransformerConfigurationfactory(),
                                                                          BeanBuilderTransformer.class,
                                                                          newBuilder()
                                                                              .withKey("beanClass")
                                                                              .withAttributeDefinition(fromSimpleParameter("beanClass",
                                                                                                                           stringToClassConverter())
                                                                                                                               .build())
                                                                              .build(),
                                                                          newBuilder()
                                                                              .withKey("beanFactory")
                                                                              .withAttributeDefinition(fromSimpleReferenceParameter("beanFactory-ref")
                                                                                  .build())
                                                                              .build(),
                                                                          newBuilder()
                                                                              .withKey("arguments")
                                                                              .withAttributeDefinition(fromChildCollectionConfiguration(ExpressionArgument.class)
                                                                                  .build())
                                                                              .build())
                                                                                  .withIdentifier("bean-builder-transformer")
                                                                                  .build());

    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(getExpressionTransformerConfigurationfactory(),
                                                                          ExpressionTransformer.class,
                                                                          newBuilder()
                                                                              .withKey("returnSourceIfNull")
                                                                              .withAttributeDefinition(fromSimpleParameter("returnSourceIfNull")
                                                                                  .build())
                                                                              .build(),
                                                                          newBuilder()
                                                                              .withKey("expression")
                                                                              .withAttributeDefinition(fromSimpleParameter("expression")
                                                                                  .build())
                                                                              .build(),
                                                                          newBuilder()
                                                                              .withKey("arguments")
                                                                              .withAttributeDefinition(fromChildCollectionConfiguration(ExpressionArgument.class)
                                                                                  .build())
                                                                              .build())
                                                                                  .withIdentifier("expression-transformer")
                                                                                  .withTypeDefinition(fromType(ExpressionTransformer.class))
                                                                                  .build());
    transformerComponentBuildingDefinitions.add(baseDefinition.copy()
        .withObjectFactoryType(ConfigurableObjectFactory.class)
        .withIdentifier("return-argument")
        .withTypeDefinition(fromType(ExpressionArgument.class))
        .withSetterParameterDefinition("factory", fromFixedValue(getExpressionArgumentConfigurationFactory()).build())
        .withSetterParameterDefinition("parameters", fromMultipleDefinitions(
                                                                             newBuilder()
                                                                                 .withKey("optional")
                                                                                 .withAttributeDefinition(fromSimpleParameter("optional")
                                                                                     .build())
                                                                                 .build(),
                                                                             newBuilder()
                                                                                 .withKey("expression")
                                                                                 .withAttributeDefinition(fromSimpleParameter("expression")
                                                                                     .build())
                                                                                 .build(),
                                                                             newBuilder()
                                                                                 .withKey("muleContext")
                                                                                 .withAttributeDefinition(fromReferenceObject(MuleContext.class)
                                                                                     .build())
                                                                                 .build())
                                                                                     .build())
        .build());
    transformerComponentBuildingDefinitions.add(baseDefinition.copy()
        .withObjectFactoryType(ConfigurableObjectFactory.class)
        .withIdentifier("bean-property")
        .withTypeDefinition(fromType(ExpressionArgument.class))
        .withSetterParameterDefinition("factory", fromFixedValue(getExpressionArgumentConfigurationFactory()).build())
        .withSetterParameterDefinition("parameters", fromMultipleDefinitions(
                                                                             newBuilder()
                                                                                 .withKey("optional")
                                                                                 .withAttributeDefinition(fromSimpleParameter("optional")
                                                                                     .build())
                                                                                 .build(),
                                                                             newBuilder()
                                                                                 .withKey("expression")
                                                                                 .withAttributeDefinition(fromSimpleParameter("expression")
                                                                                     .build())
                                                                                 .build(),
                                                                             newBuilder()
                                                                                 .withKey("muleContext")
                                                                                 .withAttributeDefinition(fromReferenceObject(MuleContext.class)
                                                                                     .build())
                                                                                 .build(),
                                                                             newBuilder()
                                                                                 .withKey("propertyName")
                                                                                 .withAttributeDefinition(fromSimpleParameter("property-name")
                                                                                     .build())
                                                                                 .build())
                                                                                     .build())
        .build());

    return transformerComponentBuildingDefinitions;
  }

  private ConfigurableInstanceFactory getAddFlowVariableTransformerInstanceFactory(Class<? extends AbstractAddVariablePropertyProcessor> transformerType) {
    return parameters -> {
      AbstractAddVariablePropertyProcessor transformer =
          (AbstractAddVariablePropertyProcessor) createNewInstance(transformerType);
      transformer.setIdentifier((String) parameters.get("identifier"));
      transformer.setValue((String) parameters.get("value"));
      return transformer;
    };
  }

  private ConfigurableInstanceFactory getEncryptionTransformerConfigurationFactory(Class<? extends AbstractEncryptionTransformer> abstractEncryptionTransformerType) {
    return parameters -> {
      AbstractEncryptionTransformer encryptionTransformer =
          (AbstractEncryptionTransformer) createNewInstance(abstractEncryptionTransformerType);
      encryptionTransformer.setStrategy((EncryptionStrategy) parameters.get("strategy"));
      return encryptionTransformer;
    };
  }

  private ConfigurableInstanceFactory getCustomTransformerConfigurationFactory() {
    return parameters -> {
      String className = (String) parameters.get("class");
      checkState(className != null, "custom-transformer class attribute cannot be null");
      return createNewInstance(className);
    };
  }

  private static Object createNewInstance(Class classType) {
    try {
      return instanciateClass(classType);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }


  private static Object createNewInstance(String className) {
    try {
      return instanciateClass(className, new Object[0]);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private ConfigurableInstanceFactory getExpressionArgumentConfigurationFactory() {
    return parameters -> {
      String name = (String) parameters.get("propertyName");
      ExpressionArgument expressionArgument =
          new ExpressionArgument(name, new ExpressionConfig((String) parameters.get("expression")),
                                 parseBoolean((String) ofNullable(parameters.get("optional")).orElse("false")));
      expressionArgument.setMuleContext((MuleContext) parameters.get("muleContext"));
      return expressionArgument;
    };
  }

  private ConfigurableInstanceFactory getExpressionTransformerConfigurationfactory() {
    return getAbstractTransformerConfigurationfactory(parameters -> {
      ExpressionTransformer expressionTransformer = new ExpressionTransformer();
      Boolean returnSourceIfNull = (Boolean) parameters.get("returnSourceIfNull");
      if (returnSourceIfNull != null) {
        expressionTransformer.setReturnSourceIfNull(returnSourceIfNull);
      }
      return expressionTransformer;
    });
  }

  private ConfigurableInstanceFactory getBeanBuilderTransformerConfigurationfactory() {
    return getAbstractTransformerConfigurationfactory(parameters -> {
      BeanBuilderTransformer beanBuilderTransformer = new BeanBuilderTransformer();
      beanBuilderTransformer.setBeanClass((Class<?>) parameters.get("beanClass"));
      beanBuilderTransformer.setBeanFactory((ObjectFactory) parameters.get("beanFactory"));
      return beanBuilderTransformer;
    });
  }

  private ConfigurableInstanceFactory getAbstractTransformerConfigurationfactory(Function<Map<String, Object>, AbstractExpressionTransformer> abstractExpressionTransformerFactory) {
    return parameters -> {
      List<ExpressionArgument> arguments = (List<ExpressionArgument>) parameters.get("arguments");
      String expression = (String) parameters.get("expression");
      AbstractExpressionTransformer abstractExpressionTransformer = abstractExpressionTransformerFactory.apply(parameters);
      if (expression != null && arguments != null) {
        throw new MuleRuntimeException(createStaticMessage("Expression transformer do not support expression attribute or return-data child element at the same time."));
      }
      if (expression != null) {
        arguments = asList(new ExpressionArgument("single", new ExpressionConfig(expression), false));
      }
      abstractExpressionTransformer.setArguments(arguments);
      return abstractExpressionTransformer;
    };
  }

  public static ComponentBuildingDefinition.Builder getSetVariablePropertyBaseBuilder(ConfigurableInstanceFactory configurableInstanceFactory,
                                                                                      Class<? extends AbstractAddVariablePropertyProcessor> setterClass,
                                                                                      KeyAttributeDefinitionPair... configurationAttributes) {
    KeyAttributeDefinitionPair[] commonTransformerParameters = {
        newBuilder()
            .withKey("encoding")
            .withAttributeDefinition(fromSimpleParameter("encoding").build())
            .build(),
        newBuilder()
            .withKey("mimeType")
            .withAttributeDefinition(fromSimpleParameter("mimeType").build())
            .build(),
        newBuilder()
            .withKey("muleContext")
            .withAttributeDefinition(fromReferenceObject(MuleContext.class).build())
            .build()
    };
    return baseDefinition.copy()
        .withTypeDefinition(fromType(setterClass))
        .withObjectFactoryType(new ConfigurableObjectFactory<>().getClass())
        .withSetterParameterDefinition("factory", fromFixedValue(configurableInstanceFactory).build())
        .withSetterParameterDefinition("commonConfiguratorType", fromFixedValue(AddVariablePropertyConfigurator.class).build())
        .withSetterParameterDefinition("parameters",
                                       fromMultipleDefinitions(addAll(commonTransformerParameters, configurationAttributes))
                                           .build())
        .asPrototype()
        .copy();
  }

  public static ComponentBuildingDefinition.Builder getTransformerBaseBuilder(ConfigurableInstanceFactory configurableInstanceFactory,
                                                                              Class<? extends Transformer> transformerClass,
                                                                              KeyAttributeDefinitionPair... configurationAttributes) {
    KeyAttributeDefinitionPair[] commonTransformerParameters = {newBuilder()
        .withKey("encoding")
        .withAttributeDefinition(fromSimpleParameter("encoding").build())
        .build(),
        newBuilder()
            .withKey("name")
            .withAttributeDefinition(fromSimpleParameter("name").build())
            .build(),
        newBuilder()
            .withKey("ignoreBadInput")
            .withAttributeDefinition(fromSimpleParameter("ignoreBadInput").build())
            .build(),
        newBuilder()
            .withKey("mimeType")
            .withAttributeDefinition(fromSimpleParameter("mimeType").build())
            .build(),
        newBuilder()
            .withKey("returnClass")
            .withAttributeDefinition(fromSimpleParameter("returnClass").build())
            .build(),
        newBuilder()
            .withKey("muleContext")
            .withAttributeDefinition(fromReferenceObject(MuleContext.class).build())
            .build()};
    return baseDefinition.copy()
        .withTypeDefinition(fromType(transformerClass))
        .withObjectFactoryType(new ConfigurableObjectFactory<>().getClass())
        .withSetterParameterDefinition("factory", fromFixedValue(configurableInstanceFactory).build())
        .withSetterParameterDefinition("commonConfiguratorType", fromFixedValue(TransformerConfigurator.class).build())
        .withSetterParameterDefinition("parameters",
                                       fromMultipleDefinitions(addAll(commonTransformerParameters, configurationAttributes))
                                           .build())
        .asPrototype()
        .copy();
  }


  private List<ComponentBuildingDefinition> getComponentsDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    final ComponentBuildingDefinition.Builder componentDefinition = baseDefinition.copy()
        .withSetterParameterDefinition("clazz", fromSimpleParameter("class").build())
        .withSetterParameterDefinition("objectFactory",
                                       fromChildConfiguration(org.mule.runtime.core.api.object.ObjectFactory.class).build())
        .withSetterParameterDefinition("entryPointResolverSet", fromChildConfiguration(EntryPointResolverSet.class).build())
        .withSetterParameterDefinition("entryPointResolver", fromChildConfiguration(EntryPointResolver.class).build())
        .withSetterParameterDefinition("lifecycleAdapterFactory", fromChildConfiguration(LifecycleAdapterFactory.class).build())
        .withSetterParameterDefinition("interceptors", fromChildCollectionConfiguration(Interceptor.class).build());

    buildingDefinitions.add(componentDefinition
        .copy()
        .withIdentifier("component")
        .withTypeDefinition(fromType(DefaultJavaComponent.class))
        .withObjectFactoryType(ComponentObjectFactory.class)
        .build());

    buildingDefinitions.add(componentDefinition
        .copy()
        .withIdentifier("pooled-component")
        .withTypeDefinition(fromType(PooledJavaComponent.class))
        .withObjectFactoryType(PooledComponentObjectFactory.class)
        .withSetterParameterDefinition("poolingProfile", fromChildConfiguration(PoolingProfile.class).build())
        .build());

    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("custom-interceptor")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .build());

    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("timer-interceptor")
        .withTypeDefinition(fromType(TimerInterceptor.class))
        .build());

    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("logging-interceptor")
        .withTypeDefinition(fromType(LoggingInterceptor.class))
        .build());

    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("log-component")
        .withTypeDefinition(fromType(DefaultJavaComponent.class))
        .withObjectFactoryType(ComponentObjectFactory.class)
        .withSetterParameterDefinition("usePrototypeObjectFactory", fromFixedValue(false).build())
        .withSetterParameterDefinition("clazz", fromFixedValue(LogComponent.class).build())
        .build());

    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("null-component")
        .withTypeDefinition(fromType(DefaultJavaComponent.class))
        .withObjectFactoryType(ComponentObjectFactory.class)
        .withSetterParameterDefinition("usePrototypeObjectFactory", fromFixedValue(false).build())
        .withSetterParameterDefinition("clazz", fromFixedValue(NullComponent.class).build())
        .build());

    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("static-component")
        .withTypeDefinition(fromType(DefaultJavaComponent.class))
        .withObjectFactoryType(ComponentObjectFactory.class)
        .withSetterParameterDefinition("usePrototypeObjectFactory", fromFixedValue(false).build())
        .withSetterParameterDefinition("clazz", fromFixedValue(StaticComponent.class).build())
        .withSetterParameterDefinition("staticData", fromChildConfiguration(String.class).build())
        .build());

    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("echo-component")
        .withTypeDefinition(fromType(DefaultJavaComponent.class))
        .withObjectFactoryType(ComponentObjectFactory.class)
        .withSetterParameterDefinition("usePrototypeObjectFactory", fromFixedValue(false).build())
        .withSetterParameterDefinition("clazz", fromFixedValue(EchoComponent.class).build())
        .build());

    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier(POOLING_PROFILE_ELEMENT_IDENTIFIER)
        .withTypeDefinition(fromType(PoolingProfile.class))
        .withConstructorParameterDefinition(fromSimpleParameter("maxActive").withDefaultValue(DEFAULT_MAX_POOL_ACTIVE).build())
        .withConstructorParameterDefinition(fromSimpleParameter("maxIdle").withDefaultValue(DEFAULT_MAX_POOL_IDLE).build())
        .withConstructorParameterDefinition(fromSimpleParameter("maxWait", value -> Long.valueOf((String) value))
            .withDefaultValue(valueOf(DEFAULT_MAX_POOL_WAIT)).build())
        .withConstructorParameterDefinition(fromSimpleParameter("exhaustedAction", POOL_EXHAUSTED_ACTIONS::get)
            .withDefaultValue(valueOf(DEFAULT_POOL_EXHAUSTED_ACTION)).build())
        .withConstructorParameterDefinition(fromSimpleParameter("initialisationPolicy", POOL_INITIALISATION_POLICIES::get)
            .withDefaultValue(valueOf(DEFAULT_POOL_INITIALISATION_POLICY)).build())
        .withSetterParameterDefinition("disabled", fromSimpleParameter("disabled").build())
        .build());

    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("return-data")
        .withTypeDefinition(fromType(String.class))
        .build());



    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier(SINGLETON_OBJECT_ELEMENT)
        .withTypeDefinition(fromType(SingletonObjectFactory.class))
        .withConstructorParameterDefinition(fromSimpleParameter(CLASS_ATTRIBUTE, stringToClassConverter()).build())
        .withConstructorParameterDefinition(fromChildConfiguration(Map.class).withDefaultValue(new HashMap<>()).build())
        .build());

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier(PROTOTYPE_OBJECT_ELEMENT)
        .withTypeDefinition(fromType(PrototypeObjectFactory.class))
        .withConstructorParameterDefinition(fromSimpleParameter(CLASS_ATTRIBUTE, stringToClassConverter()).build())
        .withConstructorParameterDefinition(fromChildConfiguration(Map.class).withDefaultValue(new HashMap<>()).build())
        .build());

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("spring-object")
        .withTypeDefinition(fromType(SpringBeanLookup.class))
        .withSetterParameterDefinition("bean", fromSimpleParameter("bean").build())
        .build());

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("custom-lifecycle-adapter-factory")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .build());

    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getStreamingDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    buildingDefinitions.addAll(getBytesStreamingDefinitions());
    buildingDefinitions.addAll(getObjectsStreamingDefinitions());

    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getBytesStreamingDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier(REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS)
        .withTypeDefinition(fromType(CursorStreamProviderFactory.class))
        .withObjectFactoryType(InMemoryCursorStreamProviderObjectFactory.class)
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("initialBufferSize")
                                                .withDefaultValue(DEFAULT_BYTE_STREAMING_BUFFER_SIZE)
                                                .build())
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("bufferSizeIncrement")
                                                .withDefaultValue(DEFAULT_BYTE_STREAMING_BUFFER_INCREMENT_SIZE)
                                                .build())
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("maxInMemorySize")
                                                .withDefaultValue(DEFAULT_BYTES_STREAMING_MAX_BUFFER_SIZE)
                                                .build())
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("bufferUnit", value -> DataUnit.valueOf((String) value))
                                                .withDefaultValue(DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT).build())
        .build());

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier(NON_REPEATABLE_BYTE_STREAM_ALIAS)
        .withTypeDefinition(fromType(CursorStreamProviderFactory.class))
        .withObjectFactoryType(NullCursorStreamProviderObjectFactory.class)
        .build());

    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getObjectsStreamingDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier(REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS)
        .withTypeDefinition(fromType(CursorIteratorProviderFactory.class))
        .withObjectFactoryType(InMemoryCursorIteratorProviderObjectFactory.class)
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("initialBufferSize")
                                                .withDefaultValue(DEFAULT_OBJECT_STREAMING_BUFFER_SIZE)
                                                .build())
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("bufferSizeIncrement")
                                                .withDefaultValue(DEFAULT_OBJECT_STREAMING_BUFFER_INCREMENT_SIZE)
                                                .build())
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("maxInMemoryInstances")
                                                .withDefaultValue(DEFAULT_OBJECT_STREAMING_MAX_BUFFER_SIZE)
                                                .build())
        .build());

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier(NON_REPEATABLE_OBJECTS_STREAM_ALIAS)
        .withTypeDefinition(fromType(CursorIteratorProviderFactory.class))
        .withObjectFactoryType(NullCursorIteratorProviderObjectFactory.class)
        .build());

    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getEntryPointResolversDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("custom-entry-point-resolver-set")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .withSetterParameterDefinition("entryPointResolvers", fromChildCollectionConfiguration(EntryPointResolver.class).build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("entry-point-resolver-set")
        .withTypeDefinition(fromType(DefaultEntryPointResolverSet.class))
        .withSetterParameterDefinition("entryPointResolvers", fromChildCollectionConfiguration(EntryPointResolver.class).build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("custom-entry-point-resolver")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("callable-entry-point-resolver")
        .withTypeDefinition(fromType(CallableEntryPointResolver.class))
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("method-entry-point-resolver")
        .withTypeDefinition(fromType(ExplicitMethodEntryPointResolver.class))
        .withObjectFactoryType(ExplicitMethodEntryPointResolverObjectFactory.class)
        .withSetterParameterDefinition("methodEntryPoints", fromChildCollectionConfiguration(MethodEntryPoint.class).build())
        .withSetterParameterDefinition("acceptVoidMethods", fromSimpleParameter("acceptVoidMethods").build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("include-entry-point")
        .withTypeDefinition(fromType(MethodEntryPoint.class))
        .withSetterParameterDefinition("enabled", fromFixedValue(true).build())
        .withSetterParameterDefinition("method", fromSimpleParameter("method").build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("exclude-entry-point")
        .withTypeDefinition(fromType(MethodEntryPoint.class))
        .withSetterParameterDefinition("enabled", fromFixedValue(false).build())
        .withSetterParameterDefinition("method", fromSimpleParameter("method").build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("property-entry-point-resolver")
        .withTypeDefinition(fromType(MethodHeaderPropertyEntryPointResolver.class))
        .withSetterParameterDefinition("methodProperty", fromSimpleParameter("property").build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("property-entry-point-resolver")
        .withTypeDefinition(fromType(MethodHeaderPropertyEntryPointResolver.class))
        .withSetterParameterDefinition("methodProperty", fromSimpleParameter("property").build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("reflection-entry-point-resolver")
        .withTypeDefinition(fromType(ReflectionEntryPointResolver.class))
        .withSetterParameterDefinition("ignoredMethods",
                                       fromChildConfiguration(List.class).withIdentifier("exclude-object-methods").build())
        .withSetterParameterDefinition("ignoredMethods",
                                       fromChildConfiguration(List.class).withIdentifier("exclude-entry-point").build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("exclude-object-methods")
        .withTypeDefinition(fromType(ExcludeDefaultObjectMethods.class))
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("no-arguments-entry-point-resolver")
        .withTypeDefinition(fromType(NoArgumentsEntryPointResolver.class))
        .withObjectFactoryType(NoArgumentsEntryPointResolverObjectFactory.class)
        .withSetterParameterDefinition("excludeDefaultObjectMethods",
                                       fromChildConfiguration(ExcludeDefaultObjectMethods.class).build())
        .withSetterParameterDefinition("methodEntryPoints", fromChildCollectionConfiguration(MethodEntryPoint.class).build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("array-entry-point-resolver")
        .withTypeDefinition(fromType(ArrayEntryPointResolver.class))
        .build());
    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getReconnectionDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    ComponentBuildingDefinition.Builder baseReconnectDefinition = baseDefinition.copy()
        .withTypeDefinition(fromType(RetryPolicyTemplate.class)).withObjectFactoryType(RetryPolicyTemplateObjectFactory.class)
        .withSetterParameterDefinition("blocking", fromSimpleParameter("blocking").build())
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build());

    buildingDefinitions.add(baseReconnectDefinition.copy().withIdentifier(RECONNECT_FOREVER_ELEMENT_IDENTIFIER)
        .withSetterParameterDefinition("count", fromFixedValue(RETRY_COUNT_FOREVER).build()).build());
    buildingDefinitions.add(baseReconnectDefinition.copy().withIdentifier(RECONNECT_ELEMENT_IDENTIFIER)
        .withSetterParameterDefinition("retryNotifier", fromChildConfiguration(RetryNotifier.class).build())
        .withSetterParameterDefinition("count", fromSimpleParameter("count").build()).build());

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("reconnect-custom-strategy")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .asPrototype()
        .build());

    buildingDefinitions.add(baseDefinition.copy().withIdentifier("reconnect-notifier")
        .withTypeDefinition(fromType(ConnectNotifier.class))
        .build());

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("reconnect-custom-notifier")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .asPrototype()
        .build());

    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getTransactionDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    buildingDefinitions.add(baseDefinition.copy().withIdentifier("xa-transaction")
        .withTypeDefinition(fromType(MuleTransactionConfig.class))
        .withSetterParameterDefinition("factory", fromFixedValue(new XaTransactionFactory()).build())
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("actionAsString", fromSimpleParameter("action").build())
        .withSetterParameterDefinition("interactWithExternal", fromSimpleParameter("interactWithExternal").build())
        .build());

    buildingDefinitions.add(baseDefinition.copy().withIdentifier("custom-transaction")
        .withTypeDefinition(fromType(MuleTransactionConfig.class))
        .withSetterParameterDefinition("factory", fromSimpleReferenceParameter("factory-ref").build())
        .withSetterParameterDefinition("factory", fromSimpleParameter("factory-class", o -> {
          try {
            return ClassUtils.instanciateClass((String) o);
          } catch (Exception e) {
            return null;
          }
        }).build())
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("actionAsString", fromSimpleParameter("action").build())
        .withSetterParameterDefinition("interactWithExternal", fromSimpleParameter("interactWithExternal").build())
        .build());

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("environment")
        .withTypeDefinition(fromType(Map.class))
        .withObjectFactoryType(EnvironmentPropertyObjectFactory.class)
        .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build())
        .build());

    buildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("custom-transaction-manager")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .withSetterParameterDefinition("environment", fromChildConfiguration(Map.class).build())
        .build());

    return buildingDefinitions;
  }

  public List<ComponentBuildingDefinition> getQueueStoreDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    buildingDefinitions.add(baseDefinition.copy().withIdentifier("default-persistent-queue-store")
        .withTypeDefinition(fromType(org.mule.runtime.core.api.store.QueueStore.class))
        .withObjectFactoryType(QueueStoreObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedReference(MuleProperties.QUEUE_STORE_DEFAULT_PERSISTENT_NAME).build())
        .build());

    buildingDefinitions.add(baseDefinition.copy().withIdentifier("default-in-memory-queue-store")
        .withTypeDefinition(fromType(org.mule.runtime.core.api.store.QueueStore.class))
        .withObjectFactoryType(QueueStoreObjectFactory.class)
        .withConstructorParameterDefinition(fromFixedReference(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME).build())
        .build());

    return buildingDefinitions;
  }

  public static ComponentBuildingDefinition.Builder getTransformerBaseBuilder(Class<? extends AbstractTransformer> transformerClass,
                                                                              KeyAttributeDefinitionPair... configurationAttributes) {
    return getTransformerBaseBuilder(parameters -> createNewInstance(transformerClass), transformerClass,
                                     configurationAttributes);
  }

  public static ComponentBuildingDefinition.Builder getMuleMessageTransformerBaseBuilder() {
    return baseDefinition.copy()
        .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
        .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
        .asPrototype()
        .copy();
  }

  private ComponentBuildingDefinition.Builder createTransactionManagerDefinitionBuilder(String transactionManagerName,
                                                                                        Class<?> transactionManagerClass) {
    return transactionManagerBaseDefinition.copy().withIdentifier(transactionManagerName)
        .withTypeDefinition(fromType(transactionManagerClass));
  }

  /**
   * Parser for the expanded operations, generated dynamically by the {@link ApplicationModel} by reading the extensions
   *
   * @param componentBuildingDefinitions
   */
  private void addModuleOperationChainParser(LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions) {
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("module-operation-chain")
        .withTypeDefinition(fromType(AnnotatedProcessor.class))
        .withObjectFactoryType(ModuleOperationMessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition("properties", fromChildMapConfiguration(String.class, String.class)
            .withWrapperIdentifier("module-operation-properties").build())
        .withSetterParameterDefinition("parameters", fromChildMapConfiguration(String.class, String.class)
            .withWrapperIdentifier("module-operation-parameters").build())
        .withSetterParameterDefinition("returnsVoid", fromSimpleParameter("returnsVoid").build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asPrototype().build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("module-operation-properties")
        .withTypeDefinition(fromType(TreeMap.class)).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("module-operation-property-entry")
        .withTypeDefinition(fromMapEntryType(String.class, String.class))
        .build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("module-operation-parameters")
        .withTypeDefinition(fromType(TreeMap.class)).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("module-operation-parameter-entry")
        .withTypeDefinition(fromMapEntryType(String.class, String.class))
        .build());
  }

}
