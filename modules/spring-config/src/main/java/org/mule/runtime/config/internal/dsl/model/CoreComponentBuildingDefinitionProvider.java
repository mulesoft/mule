/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal.dsl.model;

import static java.lang.Boolean.parseBoolean;
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
import static org.mule.runtime.api.tx.TransactionType.LOCAL;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionProviderUtils.createNewInstance;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionProviderUtils.getMuleMessageTransformerBaseBuilder;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionProviderUtils.getTransformerBaseBuilder;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair.ANY_SELECTOR_STRING;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.api.transaction.MuleTransactionConfig.ACTION_INDIFFERENT_STRING;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildMapConfiguration;
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
import static org.mule.runtime.extension.api.ExtensionConstants.DYNAMIC_CONFIG_EXPIRATION_FREQUENCY;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_BYTE_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.POOLING_PROFILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECTION_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.api.util.DataUnit;
import org.mule.runtime.config.api.dsl.ConfigurableInstanceFactory;
import org.mule.runtime.config.api.dsl.ConfigurableObjectFactory;
import org.mule.runtime.config.internal.CustomEncryptionStrategyDelegate;
import org.mule.runtime.config.internal.CustomSecurityProviderDelegate;
import org.mule.runtime.config.internal.MuleConfigurationConfigurator;
import org.mule.runtime.config.internal.NotificationConfig;
import org.mule.runtime.config.internal.ServerNotificationManagerConfigurator;
import org.mule.runtime.config.internal.dsl.processor.CustomSecurityFilterObjectFactory;
import org.mule.runtime.config.internal.dsl.processor.EnvironmentPropertyObjectFactory;
import org.mule.runtime.config.internal.dsl.processor.ReconnectionConfigObjectFactory;
import org.mule.runtime.config.internal.dsl.processor.RetryPolicyTemplateObjectFactory;
import org.mule.runtime.config.internal.dsl.processor.factory.MessageEnricherObjectFactory;
import org.mule.runtime.config.internal.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.runtime.config.internal.factories.ChoiceRouterObjectFactory;
import org.mule.runtime.config.internal.factories.DefaultFlowFactoryBean;
import org.mule.runtime.config.internal.factories.DynamicConfigExpirationObjectFactory;
import org.mule.runtime.config.internal.factories.ExpirationPolicyObjectFactory;
import org.mule.runtime.config.internal.factories.FlowRefFactoryBean;
import org.mule.runtime.config.internal.factories.MessageProcessorFilterPairFactoryBean;
import org.mule.runtime.config.internal.factories.ModuleOperationMessageProcessorChainFactoryBean;
import org.mule.runtime.config.internal.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.runtime.config.internal.factories.SchedulingMessageSourceFactoryBean;
import org.mule.runtime.config.internal.factories.SubflowMessageProcessorChainFactoryBean;
import org.mule.runtime.config.internal.factories.TryProcessorFactoryBean;
import org.mule.runtime.config.internal.factories.streaming.InMemoryCursorIteratorProviderObjectFactory;
import org.mule.runtime.config.internal.factories.streaming.InMemoryCursorStreamProviderObjectFactory;
import org.mule.runtime.config.internal.factories.streaming.NullCursorIteratorProviderObjectFactory;
import org.mule.runtime.config.internal.factories.streaming.NullCursorStreamProviderObjectFactory;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.privileged.dsl.processor.AddVariablePropertyConfigurator;
import org.mule.runtime.config.privileged.dsl.processor.MessageProcessorChainFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationExtension;
import org.mule.runtime.core.api.config.DynamicConfigExpiration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.RaiseErrorProcessor;
import org.mule.runtime.core.api.retry.RetryNotifier;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.core.api.security.MuleSecurityManagerConfigurator;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.source.scheduler.CronScheduler;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.core.api.source.scheduler.PeriodicScheduler;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.object.CursorIteratorProviderFactory;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.el.ExpressionLanguageComponent;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.el.mvel.configuration.AliasEntry;
import org.mule.runtime.core.internal.el.mvel.configuration.ImportEntry;
import org.mule.runtime.core.internal.el.mvel.configuration.MVELExpressionLanguageObjectFactory;
import org.mule.runtime.core.internal.el.mvel.configuration.MVELGlobalFunctionsConfig;
import org.mule.runtime.core.internal.enricher.MessageEnricher;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.OnErrorContinueHandler;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.internal.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.internal.processor.InvokerMessageProcessor;
import org.mule.runtime.core.internal.processor.LoggerMessageProcessor;
import org.mule.runtime.core.internal.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.internal.processor.TryScope;
import org.mule.runtime.core.internal.processor.simple.AddFlowVariableProcessor;
import org.mule.runtime.core.internal.processor.simple.ParseTemplateProcessor;
import org.mule.runtime.core.internal.processor.simple.RemoveFlowVariableProcessor;
import org.mule.runtime.core.internal.processor.simple.SetPayloadMessageProcessor;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.core.internal.routing.ChoiceRouter;
import org.mule.runtime.core.internal.routing.FirstSuccessful;
import org.mule.runtime.core.internal.routing.Foreach;
import org.mule.runtime.core.internal.routing.ForkJoinStrategyFactory;
import org.mule.runtime.core.internal.routing.IdempotentMessageValidator;
import org.mule.runtime.core.internal.routing.MessageChunkAggregator;
import org.mule.runtime.core.internal.routing.MessageChunkSplitter;
import org.mule.runtime.core.internal.routing.MessageProcessorExpressionPair;
import org.mule.runtime.core.internal.routing.Resequencer;
import org.mule.runtime.core.internal.routing.RoundRobin;
import org.mule.runtime.core.internal.routing.ScatterGatherRouter;
import org.mule.runtime.core.internal.routing.SimpleCollectionAggregator;
import org.mule.runtime.core.internal.routing.Splitter;
import org.mule.runtime.core.internal.routing.UntilSuccessful;
import org.mule.runtime.core.internal.routing.forkjoin.CollectListForkJoinStrategyFactory;
import org.mule.runtime.core.internal.routing.requestreply.SimpleAsyncRequestReplyRequester;
import org.mule.runtime.core.internal.security.PasswordBasedEncryptionStrategy;
import org.mule.runtime.core.internal.security.SecretKeyEncryptionStrategy;
import org.mule.runtime.core.internal.security.UsernamePasswordAuthenticationFilter;
import org.mule.runtime.core.internal.security.filter.MuleEncryptionEndpointSecurityFilter;
import org.mule.runtime.core.internal.source.scheduler.DefaultSchedulerMessageSource;
import org.mule.runtime.core.internal.transformer.codec.XmlEntityDecoder;
import org.mule.runtime.core.internal.transformer.codec.XmlEntityEncoder;
import org.mule.runtime.core.internal.transformer.compression.GZipCompressTransformer;
import org.mule.runtime.core.internal.transformer.compression.GZipUncompressTransformer;
import org.mule.runtime.core.internal.transformer.encryption.AbstractEncryptionTransformer;
import org.mule.runtime.core.internal.transformer.encryption.DecryptionTransformer;
import org.mule.runtime.core.internal.transformer.encryption.EncryptionTransformer;
import org.mule.runtime.core.internal.transformer.expression.AbstractExpressionTransformer;
import org.mule.runtime.core.internal.transformer.expression.ExpressionArgument;
import org.mule.runtime.core.internal.transformer.expression.ExpressionTransformer;
import org.mule.runtime.core.internal.transformer.simple.AutoTransformer;
import org.mule.runtime.core.internal.transformer.simple.ByteArrayToHexString;
import org.mule.runtime.core.internal.transformer.simple.HexStringToByteArray;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.internal.transformer.simple.ObjectToString;
import org.mule.runtime.core.internal.transformer.simple.StringAppendTransformer;
import org.mule.runtime.core.privileged.processor.AnnotatedProcessor;
import org.mule.runtime.core.privileged.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.objectfactory.MessageProcessorChainObjectFactory;
import org.mule.runtime.core.privileged.processor.simple.AbstractAddVariablePropertyProcessor;
import org.mule.runtime.core.privileged.transaction.xa.XaTransactionFactory;
import org.mule.runtime.core.privileged.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.privileged.transformer.simple.ByteArrayToSerializable;
import org.mule.runtime.core.privileged.transformer.simple.SerializableToByteArray;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.dsl.api.component.TypeConverter;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

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
  private static final String ERROR_HANDLER = "error-handler";
  private static final String SET_PAYLOAD = "set-payload";
  private static final String LOGGER = "logger";
  private static final String PROCESSOR_CHAIN = "processor-chain";
  private static final String ROUTE = "route";
  private static final String ROUTES = "routes";
  private static final String PROCESSOR = "processor";
  private static final String TRANSFORMER = "transformer";
  private static final String CUSTOM_PROCESSOR = "custom-processor";
  private static final String CLASS_ATTRIBUTE = "class";
  private static final String SUB_FLOW = "sub-flow";
  private static final String RESPONSE = "response";
  private static final String FLOW = "flow";
  private static final String FLOW_REF = "flow-ref";
  private static final String EXCEPTION_LISTENER_ATTRIBUTE = "exceptionListener";
  private static final String SCATTER_GATHER = "scatter-gather";
  private static final String FORK_JOIN_STRATEGY = "forkJoinStrategyFactory";
  private static final String COLLECT_LIST = "collect-list";
  private static final String ENRICHER = "enricher";
  private static final String ASYNC = "async";
  private static final String TRY = "try";
  private static final String UNTIL_SUCCESSFUL = "until-successful";
  private static final String FOREACH = "foreach";
  private static final String FIRST_SUCCESSFUL = "first-successful";
  private static final String ROUND_ROBIN = "round-robin";
  private static final String CHOICE = "choice";
  private static final String OTHERWISE = "otherwise";
  private static final String SCHEDULER = "scheduler";
  private static final String REQUEST_REPLY = "request-reply";
  private static final String ERROR_TYPE = "errorType";
  private static final String TYPE = "type";
  private static final String TX_ACTION = "transactionalAction";
  private static final String TX_TYPE = "transactionType";
  private static final String LOG_EXCEPTION = "logException";
  private static final String RAISE_ERROR = "raise-error";

  private static final Class<?> MESSAGE_PROCESSOR_CLASS = Processor.class;

  private static ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(CORE_PREFIX);
  private ComponentBuildingDefinition.Builder transactionManagerBaseDefinition;

  @Override
  public void init() {
    transactionManagerBaseDefinition = baseDefinition;
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {

    LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();

    AttributeDefinition messageProcessorListAttributeDefinition =
        fromChildCollectionConfiguration(Processor.class).build();
    ComponentBuildingDefinition.Builder onErrorBaseBuilder = baseDefinition
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, messageProcessorListAttributeDefinition)
        .withSetterParameterDefinition(WHEN, fromSimpleParameter(WHEN).build())
        .withSetterParameterDefinition(ERROR_TYPE, fromSimpleParameter(TYPE).build())
        .withSetterParameterDefinition(LOG_EXCEPTION, fromSimpleParameter(LOG_EXCEPTION).withDefaultValue("true").build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(EXCEPTION_STRATEGY).withTypeDefinition(fromType(Object.class))
            .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build()).build());
    componentBuildingDefinitions.add(onErrorBaseBuilder.withIdentifier(ON_ERROR_CONTINUE)
        .withTypeDefinition(fromType(OnErrorContinueHandler.class))
        .asPrototype().build());
    componentBuildingDefinitions.add(onErrorBaseBuilder.withIdentifier(ON_ERROR_PROPAGATE)
        .withTypeDefinition(fromType(OnErrorPropagateHandler.class))
        .asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(ERROR_HANDLER)
        .withTypeDefinition(fromType(ErrorHandler.class))
        .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build())
        .withSetterParameterDefinition("exceptionListeners",
                                       fromChildCollectionConfiguration(FlowExceptionHandler.class).build())
        .asPrototype()
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(SET_PAYLOAD).withTypeDefinition(fromType(SetPayloadMessageProcessor.class))
            .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
            .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
            .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build()).build());

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(LOGGER).withTypeDefinition(fromType(LoggerMessageProcessor.class))
            .withSetterParameterDefinition("message", fromSimpleParameter("message").build())
            .withSetterParameterDefinition("category", fromSimpleParameter("category").build())
            .withSetterParameterDefinition("level", fromSimpleParameter("level").build()).build());

    componentBuildingDefinitions
        .add(getSetVariablePropertyBaseBuilder(getAddVariableTransformerInstanceFactory(AddFlowVariableProcessor.class),
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

    componentBuildingDefinitions.add(getCoreMuleMessageTransformerBaseBuilder()
        .withIdentifier("remove-variable")
        .withTypeDefinition(fromType(RemoveFlowVariableProcessor.class))
        .withSetterParameterDefinition("identifier", fromSimpleParameter("variableName").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition
        .withIdentifier("global-property")
        .withTypeDefinition(fromType(String.class))
        .withConstructorParameterDefinition(fromSimpleParameter("value").build())
        .build());

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(TRANSFORMER).withTypeDefinition(fromType(Transformer.class)).build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(CUSTOM_PROCESSOR)
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE)
            .checkingThatIsClassOrInheritsFrom(MESSAGE_PROCESSOR_CLASS))
        .asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(PROCESSOR_CHAIN)
        .withTypeDefinition(fromType(AnnotatedProcessor.class)).withObjectFactoryType(MessageProcessorChainObjectFactory.class)
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(ROUTE)
        .withTypeDefinition(fromType(MessageProcessorChain.class)).withObjectFactoryType(MessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asPrototype().build());
    addModuleOperationChainParser(componentBuildingDefinitions);
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(SUB_FLOW)
        .withTypeDefinition(fromType(MessageProcessorChain.class))
        .withObjectFactoryType(SubflowMessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build()).asPrototype().build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(RESPONSE).withTypeDefinition(fromType(ResponseMessageProcessorAdapter.class))
            .withObjectFactoryType(ResponseMessageProcessorsFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(FLOW).withTypeDefinition(fromType(Flow.class))
            .withObjectFactoryType(DefaultFlowFactoryBean.class)
            .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build())
            .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
            .withSetterParameterDefinition("initialState",
                                           fromSimpleParameter("initialState").withDefaultValue(INITIAL_STATE_STARTED).build())
            .withSetterParameterDefinition("messageSource", fromChildConfiguration(MessageSource.class).build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition(EXCEPTION_LISTENER_ATTRIBUTE,
                                           fromChildConfiguration(FlowExceptionHandler.class).build())
            .withSetterParameterDefinition("maxConcurrency", fromSimpleParameter("maxConcurrency").build())
            .build());

    Builder processorRefBuilder = baseDefinition
        .withTypeDefinition(fromType(AnnotatedProcessor.class))
        .withObjectFactoryType(FlowRefFactoryBean.class);

    componentBuildingDefinitions.add(processorRefBuilder
        .withIdentifier(FLOW_REF)
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .build());
    componentBuildingDefinitions.add(processorRefBuilder
        .withIdentifier(PROCESSOR)
        .withSetterParameterDefinition("name", fromSimpleParameter("ref").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(COLLECT_LIST)
        .withTypeDefinition(fromType(CollectListForkJoinStrategyFactory.class))
        .build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(SCATTER_GATHER)
        .withTypeDefinition(fromType(ScatterGatherRouter.class))
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("maxConcurrency", fromSimpleParameter("maxConcurrency").build())
        .withSetterParameterDefinition("target", fromSimpleParameter("target").build())
        .withSetterParameterDefinition("targetValue", fromSimpleParameter("targetValue")
            .withDefaultValue("#[payload]")
            .build())
        .withSetterParameterDefinition(ROUTES, fromChildCollectionConfiguration(MessageProcessorChain.class).build())
        .withSetterParameterDefinition(FORK_JOIN_STRATEGY, fromChildConfiguration(ForkJoinStrategyFactory.class).build())
        .asScope().build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(ENRICHER)
        .withObjectFactoryType(MessageEnricherObjectFactory.class).withTypeDefinition(fromType(MessageEnricher.class))
        .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(Processor.class).build())
        .withSetterParameterDefinition("enrichExpressionPairs",
                                       fromChildCollectionConfiguration(MessageEnricher.EnrichExpressionPair.class).build())
        .withSetterParameterDefinition("source", fromSimpleParameter("source").build())
        .withSetterParameterDefinition("target", fromSimpleParameter("target").build()).build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier("enrich")
        .withTypeDefinition(fromType(MessageEnricher.EnrichExpressionPair.class))
        .withConstructorParameterDefinition(fromSimpleParameter("source").build())
        .withConstructorParameterDefinition(fromSimpleParameter("target").build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(ASYNC).withTypeDefinition(fromType(AsyncDelegateMessageProcessor.class))
            .withObjectFactoryType(AsyncMessageProcessorsFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build()).build());
    // TODO MULE-12726 Remove TryProcessorFactoryBean
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(TRY).withTypeDefinition(fromType(TryScope.class))
            .withObjectFactoryType(TryProcessorFactoryBean.class)
            .withSetterParameterDefinition("exceptionListener", fromChildConfiguration(FlowExceptionHandler.class).build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition(TX_ACTION, fromSimpleParameter(TX_ACTION).withDefaultValue(ACTION_INDIFFERENT_STRING)
                .build())
            .withSetterParameterDefinition(TX_TYPE, fromSimpleParameter(TX_TYPE, getTransactionTypeConverter())
                .withDefaultValue(LOCAL.name()).build())
            .build());

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(UNTIL_SUCCESSFUL).withTypeDefinition(fromType(UntilSuccessful.class))
            .withSetterParameterDefinition("maxRetries", fromSimpleParameter("maxRetries").withDefaultValue(5).build())
            .withSetterParameterDefinition("millisBetweenRetries",
                                           fromSimpleParameter("millisBetweenRetries").withDefaultValue(60000).build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(FOREACH).withTypeDefinition(fromType(Foreach.class))
        .withSetterParameterDefinition("collectionExpression", fromSimpleParameter("collection").build())
        .withSetterParameterDefinition("batchSize", fromSimpleParameter("batchSize").build())
        .withSetterParameterDefinition("rootMessageVariableName", fromSimpleParameter("rootMessageVariableName").build())
        .withSetterParameterDefinition("counterVariableName", fromSimpleParameter("counterVariableName").build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(FIRST_SUCCESSFUL).withTypeDefinition(fromType(FirstSuccessful.class))
            .withSetterParameterDefinition(MESSAGE_PROCESSORS,
                                           fromChildCollectionConfiguration(MessageProcessorChain.class).build())
            .build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(ROUND_ROBIN).withTypeDefinition(fromType(RoundRobin.class))
            .withSetterParameterDefinition(MESSAGE_PROCESSORS,
                                           fromChildCollectionConfiguration(MessageProcessorChain.class).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(CHOICE).withTypeDefinition(fromType(ChoiceRouter.class))
        .withObjectFactoryType(ChoiceRouterObjectFactory.class)
        .withSetterParameterDefinition("routes", fromChildCollectionConfiguration(MessageProcessorExpressionPair.class).build())
        .withSetterParameterDefinition("defaultRoute", fromChildConfiguration(MessageProcessorExpressionPair.class).build())
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(WHEN).withTypeDefinition(fromType(MessageProcessorExpressionPair.class))
            .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition("expression", fromSimpleParameter("expression").withDefaultValue("true").build())
            .build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(OTHERWISE).withTypeDefinition(fromType(MessageProcessorExpressionPair.class))
            .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition("expression", fromFixedValue("true").build()).build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier("message-chunk-splitter")
        .withTypeDefinition(fromType(MessageChunkSplitter.class))
        .withSetterParameterDefinition("messageSize", fromSimpleParameter("messageSize").build()).build());

    ComponentBuildingDefinition.Builder baseAggregatorDefinition = baseDefinition
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("failOnTimeout", fromSimpleParameter("failOnTimeout").build())
        .withSetterParameterDefinition("processedGroupsObjectStore",
                                       fromSimpleReferenceParameter("processed-groups-object-store").build())
        .withSetterParameterDefinition("eventGroupsObjectStore",
                                       fromSimpleReferenceParameter("event-groups-object-store").build())
        .withSetterParameterDefinition("persistentStores", fromSimpleParameter("persistentStores").build())
        .withSetterParameterDefinition("storePrefix", fromSimpleParameter("storePrefix").build());

    componentBuildingDefinitions.add(baseAggregatorDefinition.withIdentifier("message-chunk-aggregator")
        .withTypeDefinition(fromType(MessageChunkAggregator.class)).build());

    componentBuildingDefinitions.add(baseAggregatorDefinition.withIdentifier("collection-aggregator")
        .withTypeDefinition(fromType(SimpleCollectionAggregator.class))
        .build());

    componentBuildingDefinitions.add(baseAggregatorDefinition.withIdentifier("resequencer")
        .withTypeDefinition(fromType(Resequencer.class))
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("splitter")
        .withTypeDefinition(fromType(Splitter.class))
        .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
        .withSetterParameterDefinition("filterOnErrorType", fromSimpleParameter("filterOnErrorType").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(REQUEST_REPLY)
        .withTypeDefinition(fromType(SimpleAsyncRequestReplyRequester.class))
        .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(Processor.class).build())
        .withSetterParameterDefinition("messageSource", fromChildConfiguration(MessageSource.class).build())
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("storePrefix", fromSimpleParameter("storePrefix").build()).build());

    componentBuildingDefinitions.add(baseDefinition
        .withIdentifier(SCHEDULER)
        .withTypeDefinition(fromType(DefaultSchedulerMessageSource.class))
        .withObjectFactoryType(SchedulingMessageSourceFactoryBean.class)
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
        .withSetterParameterDefinition("scheduler", fromChildConfiguration(PeriodicScheduler.class)
            .withWrapperIdentifier("scheduling-strategy").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("fixed-frequency")
        .withTypeDefinition(fromType(FixedFrequencyScheduler.class))
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
        .withSetterParameterDefinition("startDelay", fromSimpleParameter("startDelay").build())
        .withSetterParameterDefinition("timeUnit", fromSimpleParameter("timeUnit").build()).build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("cron")
        .withTypeDefinition(fromType(CronScheduler.class))
        .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
        .withSetterParameterDefinition("timeZone", fromSimpleParameter("timeZone").build()).build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("configuration")
        .withTypeDefinition(fromType(MuleConfiguration.class)).withObjectFactoryType(MuleConfigurationConfigurator.class)
        .withSetterParameterDefinition("defaultErrorHandlerName",
                                       fromSimpleParameter("defaultErrorHandler-ref").build())
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
        .withSetterParameterDefinition("defaultObjectSerializer",
                                       fromSimpleReferenceParameter("defaultObjectSerializer-ref").build())
        .withSetterParameterDefinition("extensions", fromChildCollectionConfiguration(ConfigurationExtension.class).build())
        .withSetterParameterDefinition("dynamicConfigExpiration",
                                       fromChildConfiguration(DynamicConfigExpiration.class).build())
        .withSetterParameterDefinition("extensions", fromChildCollectionConfiguration(Object.class).build())
        .alwaysEnabled(true)
        .withRegistrationName(OBJECT_MULE_CONFIGURATION)
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("dynamic-config-expiration")
        .withTypeDefinition(fromType(DynamicConfigExpiration.class))
        .withObjectFactoryType(DynamicConfigExpirationObjectFactory.class)
        .withConstructorParameterDefinition(fromSimpleParameter("frequency")
            .withDefaultValue(DYNAMIC_CONFIG_EXPIRATION_FREQUENCY.getTime())
            .build())
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("timeUnit", value -> TimeUnit.valueOf((String) value))
                                                .withDefaultValue(DYNAMIC_CONFIG_EXPIRATION_FREQUENCY.getUnit())
                                                .build())
        .withSetterParameterDefinition("expirationPolicy", fromChildConfiguration(ExpirationPolicy.class).build())
        .build());

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier("expiration-policy").withTypeDefinition(fromType(ExpirationPolicy.class))
            .withObjectFactoryType(ExpirationPolicyObjectFactory.class)
            .withSetterParameterDefinition("maxIdleTime",
                                           fromSimpleParameter("maxIdleTime")
                                               .withDefaultValue(DYNAMIC_CONFIG_EXPIRATION_FREQUENCY.getTime())
                                               .build())
            .withSetterParameterDefinition("timeUnit",
                                           fromSimpleParameter("timeUnit", value -> TimeUnit.valueOf((String) value))
                                               .withDefaultValue(DYNAMIC_CONFIG_EXPIRATION_FREQUENCY.getUnit())
                                               .build())
            .build());


    componentBuildingDefinitions.add(baseDefinition.withIdentifier("notifications")
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
        baseDefinition.withSetterParameterDefinition("interfaceName", fromSimpleParameter("interface").build())
            .withSetterParameterDefinition("eventName", fromSimpleParameter("event").build())
            .withSetterParameterDefinition("interfaceClass", fromSimpleParameter("interface-class").build())
            .withSetterParameterDefinition("eventClass", fromSimpleParameter("event-class").build());

    componentBuildingDefinitions.add(baseNotificationDefinition
        .withTypeDefinition(fromType(NotificationConfig.EnabledNotificationConfig.class))
        .withIdentifier("notification").build());

    componentBuildingDefinitions.add(baseNotificationDefinition
        .withTypeDefinition(fromType(NotificationConfig.DisabledNotificationConfig.class))
        .withIdentifier("disable-notification").build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("notification-listener")
        .withTypeDefinition(fromType(ListenerSubscriptionPair.class))
        .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build())
        .withConstructorParameterDefinition(fromSimpleParameter("subscription", getNotificationSubscriptionConverter())
            .withDefaultValue(ANY_SELECTOR_STRING)
            .build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("username-password-filter")
        .withTypeDefinition(fromType(UsernamePasswordAuthenticationFilter.class))
        .withSetterParameterDefinition("username", fromSimpleParameter("username").build())
        .withSetterParameterDefinition("password", fromSimpleParameter("password").build())
        .withIgnoredConfigurationParameter(NAME)
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("custom-security-filter")
        .withTypeDefinition(fromType(Processor.class))
        .withObjectFactoryType(CustomSecurityFilterObjectFactory.class)
        .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build())
        .withIgnoredConfigurationParameter(NAME)
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("encryption-security-filter")
        .withTypeDefinition(fromType(MuleEncryptionEndpointSecurityFilter.class))
        .withConstructorParameterDefinition(fromSimpleReferenceParameter("strategy-ref").build())
        .withIgnoredConfigurationParameter(NAME)
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("security-manager")
        .withTypeDefinition(fromType(SecurityManager.class)).withObjectFactoryType(MuleSecurityManagerConfigurator.class)
        .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("providers", fromChildCollectionConfiguration(SecurityProvider.class).build())
        .withSetterParameterDefinition("encryptionStrategies", fromChildCollectionConfiguration(EncryptionStrategy.class).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("custom-security-provider")
        .withTypeDefinition(fromType(CustomSecurityProviderDelegate.class))
        .withConstructorParameterDefinition(fromSimpleReferenceParameter("provider-ref").build())
        .withConstructorParameterDefinition(fromSimpleParameter("name").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("custom-encryption-strategy")
        .withTypeDefinition(fromType(CustomEncryptionStrategyDelegate.class))
        .withConstructorParameterDefinition(fromSimpleReferenceParameter("strategy-ref").build())
        .withConstructorParameterDefinition(fromSimpleParameter("name").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("secret-key-encryption-strategy")
        .withTypeDefinition(fromType(SecretKeyEncryptionStrategy.class))
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("key", fromSimpleParameter("key").build())
        .withSetterParameterDefinition("keyFactory", fromSimpleReferenceParameter("keyFactory-ref").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("password-encryption-strategy")
        .withTypeDefinition(fromType(PasswordBasedEncryptionStrategy.class))
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("iterationCount", fromSimpleParameter("iterationCount").build())
        .withSetterParameterDefinition("password", fromSimpleParameter("password").build())
        .withSetterParameterDefinition("salt", fromSimpleParameter("salt").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(REDELIVERY_POLICY_ELEMENT_IDENTIFIER)
        .withTypeDefinition(fromType(IdempotentRedeliveryPolicy.class))
        .withSetterParameterDefinition("maxRedeliveryCount", fromSimpleParameter("maxRedeliveryCount").build())
        .withSetterParameterDefinition("useSecureHash", fromSimpleParameter("useSecureHash").build())
        .withSetterParameterDefinition("messageDigestAlgorithm", fromSimpleParameter("messageDigestAlgorithm").build())
        .withSetterParameterDefinition("idExpression", fromSimpleParameter("idExpression").build())
        .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("objectStore").build())
        .withSetterParameterDefinition("privateObjectStore", fromChildConfiguration(ValueResolver.class).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("pooling-profile")
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

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("expression-component")
        .withTypeDefinition(fromType(ExpressionLanguageComponent.class))
        .withSetterParameterDefinition("expression", fromTextContent().build())
        .withSetterParameterDefinition("expressionFile", fromSimpleParameter("file").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("invoke")
        .withTypeDefinition(fromType(InvokerMessageProcessor.class))
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("methodName", fromSimpleParameter("method").build())
        .withSetterParameterDefinition("argumentExpressionsString",
                                       fromSimpleParameter("methodArguments").build())
        .withSetterParameterDefinition("object",
                                       fromSimpleReferenceParameter("object-ref").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("expression-language")
        .withTypeDefinition(fromType(MVELExpressionLanguage.class))
        .withObjectFactoryType(MVELExpressionLanguageObjectFactory.class)
        .withSetterParameterDefinition("autoResolveVariables", fromSimpleParameter("autoResolveVariables").build())
        .withSetterParameterDefinition("globalFunctions", fromChildConfiguration(MVELGlobalFunctionsConfig.class).build())
        .withSetterParameterDefinition("imports", fromChildCollectionConfiguration(ImportEntry.class).build())
        .withSetterParameterDefinition("aliases", fromChildCollectionConfiguration(AliasEntry.class).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("import")
        .withTypeDefinition(fromType(ImportEntry.class))
        .withSetterParameterDefinition("key", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("class", stringToClassConverter()).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("alias")
        .withTypeDefinition(fromType(AliasEntry.class))
        .withSetterParameterDefinition("key", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("expression").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("global-functions")
        .withTypeDefinition(fromType(MVELGlobalFunctionsConfig.class))
        .withSetterParameterDefinition("file", fromSimpleParameter("file").build())
        .withSetterParameterDefinition("inlineScript", fromTextContent().build())
        .build());

    componentBuildingDefinitions.addAll(getTransformersBuildingDefinitions());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(POOLING_PROFILE_ELEMENT_IDENTIFIER)
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

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(RAISE_ERROR).withTypeDefinition(fromType(RaiseErrorProcessor.class))
            .withSetterParameterDefinition("type", fromSimpleParameter("type").build())
            .withSetterParameterDefinition("description", fromSimpleParameter("description").build()).build());

    componentBuildingDefinitions.addAll(getStreamingDefinitions());
    componentBuildingDefinitions.addAll(getIdempotentValidatorsDefinitions());
    componentBuildingDefinitions.addAll(getReconnectionDefinitions());
    componentBuildingDefinitions.addAll(getTransactionDefinitions());
    return componentBuildingDefinitions;
  }

  private TypeConverter<String, TransactionType> getTransactionTypeConverter() {
    return TransactionType::valueOf;
  }

  private TypeConverter<String, Predicate<? extends Notification>> getNotificationSubscriptionConverter() {
    return subscription -> {
      if (ANY_SELECTOR_STRING.equals(subscription)) {
        return (Predicate<? extends Notification>) (n -> true);
      }
      return (notification -> subscription != null ? subscription
          .equals(((AbstractServerNotification) notification).getResourceIdentifier()) : true);
    };
  }

  private List<ComponentBuildingDefinition> getIdempotentValidatorsDefinitions() {
    List<ComponentBuildingDefinition> definitions = new LinkedList<>();

    ComponentBuildingDefinition.Builder baseIdempotentMessageFilterDefinition = baseDefinition
        .withSetterParameterDefinition("idExpression", fromSimpleParameter("idExpression").build())
        .withSetterParameterDefinition("valueExpression", fromSimpleParameter("valueExpression").build())
        .withSetterParameterDefinition("storePrefix", fromSimpleParameter("storePrefix").build())
        .withSetterParameterDefinition("throwOnUnaccepted", fromSimpleParameter("throwOnUnaccepted").build())
        .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("objectStore").build())
        .withSetterParameterDefinition("unacceptedMessageProcessor", fromSimpleReferenceParameter("onUnaccepted").build())
        .withSetterParameterDefinition("privateObjectStore", fromChildConfiguration(ValueResolver.class).build());

    definitions.add(baseIdempotentMessageFilterDefinition
        .withIdentifier("idempotent-message-validator")
        .withTypeDefinition(fromType(IdempotentMessageValidator.class))
        .build());

    return definitions;
  }

  private List<ComponentBuildingDefinition> getTransformersBuildingDefinitions() {
    List<ComponentBuildingDefinition> transformerComponentBuildingDefinitions = new ArrayList<>();
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(XmlEntityEncoder.class)
        .withIdentifier("xml-entity-encoder-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(XmlEntityDecoder.class)
        .withIdentifier("xml-entity-decoder-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(GZipCompressTransformer.class)
        .withIdentifier("gzip-compress-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(GZipUncompressTransformer.class)
        .withIdentifier("gzip-uncompress-transformer")
        .build());
    KeyAttributeDefinitionPair strategyParameterDefinition = newBuilder()
        .withKey("strategy")
        .withAttributeDefinition(fromSimpleReferenceParameter("strategy-ref").build())
        .build();
    transformerComponentBuildingDefinitions
        .add(getTransformerBaseBuilder(getEncryptionTransformerConfigurationFactory(EncryptionTransformer.class),
                                       EncryptionTransformer.class, strategyParameterDefinition)
                                           .withIdentifier("encrypt-transformer").withNamespace(CORE_PREFIX)
                                           .build());
    transformerComponentBuildingDefinitions
        .add(getTransformerBaseBuilder(getEncryptionTransformerConfigurationFactory(DecryptionTransformer.class),
                                       DecryptionTransformer.class, strategyParameterDefinition)
                                           .withIdentifier("decrypt-transformer").withNamespace(CORE_PREFIX)
                                           .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(ByteArrayToHexString.class)
        .withIdentifier("byte-array-to-hex-string-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(HexStringToByteArray.class)
        .withIdentifier("hex-string-to-byte-array-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(ByteArrayToObject.class)
        .withIdentifier("byte-array-to-object-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(ObjectToByteArray.class)
        .withIdentifier("object-to-byte-array-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(ObjectToString.class)
        .withIdentifier("object-to-string-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(ByteArrayToSerializable.class)
        .withIdentifier("byte-array-to-serializable-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(SerializableToByteArray.class)
        .withIdentifier("serializable-to-byte-array-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(ObjectToString.class)
        .withIdentifier("byte-array-to-string-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(ObjectToByteArray.class)
        .withIdentifier("string-to-byte-array-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(baseDefinition
        .withIdentifier("parse-template")
        .withTypeDefinition(fromType(ParseTemplateProcessor.class))
        .withSetterParameterDefinition("content", fromChildConfiguration(String.class).withIdentifier("content").build())
        .withSetterParameterDefinition("target", fromSimpleParameter("target").build())
        .withSetterParameterDefinition("location", fromSimpleParameter("location").build())
        .withSetterParameterDefinition("targetValue", fromSimpleParameter("targetValue").build())
        .build());
    transformerComponentBuildingDefinitions.add(getCoreMuleMessageTransformerBaseBuilder()
        .withIdentifier("content").withTypeDefinition(fromType(String.class)).build());
    transformerComponentBuildingDefinitions.add(getCoreTransformerBaseBuilder(AutoTransformer.class)
        .withIdentifier("auto-transformer")
        .build());
    transformerComponentBuildingDefinitions.add(getCoreMuleMessageTransformerBaseBuilder()
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
                                                                                  .withNamespace(CORE_PREFIX)
                                                                                  .build());

    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder(getExpressionTransformerConfigurationFactory(),
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
                                                                                  .withNamespace(CORE_PREFIX)
                                                                                  .withTypeDefinition(fromType(ExpressionTransformer.class))
                                                                                  .build());
    transformerComponentBuildingDefinitions.add(baseDefinition
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
    transformerComponentBuildingDefinitions.add(baseDefinition
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

  private ConfigurableInstanceFactory getAddVariableTransformerInstanceFactory(Class<? extends AbstractAddVariablePropertyProcessor> transformerType) {
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

  private ConfigurableInstanceFactory getExpressionArgumentConfigurationFactory() {
    return parameters -> {
      String name = (String) parameters.get("propertyName");
      ExpressionArgument expressionArgument =
          new ExpressionArgument(name, (String) parameters.get("expression"),
                                 parseBoolean((String) ofNullable(parameters.get("optional")).orElse("false")));
      expressionArgument.setMuleContext((MuleContext) parameters.get("muleContext"));
      return expressionArgument;
    };
  }

  private ConfigurableInstanceFactory getExpressionTransformerConfigurationFactory() {
    return getAbstractTransformerConfigurationFactory(parameters -> {
      ExpressionTransformer expressionTransformer = new ExpressionTransformer();
      Boolean returnSourceIfNull = (Boolean) parameters.get("returnSourceIfNull");
      if (returnSourceIfNull != null) {
        expressionTransformer.setReturnSourceIfNull(returnSourceIfNull);
      }
      return expressionTransformer;
    });
  }

  private ConfigurableInstanceFactory getAbstractTransformerConfigurationFactory(Function<Map<String, Object>, AbstractExpressionTransformer> abstractExpressionTransformerFactory) {
    return parameters -> {
      List<ExpressionArgument> arguments = (List<ExpressionArgument>) parameters.get("arguments");
      String expression = (String) parameters.get("expression");
      AbstractExpressionTransformer abstractExpressionTransformer = abstractExpressionTransformerFactory.apply(parameters);
      if (expression != null && arguments != null) {
        throw new MuleRuntimeException(createStaticMessage("Expression transformer do not support expression attribute or return-data child element at the same time."));
      }
      if (expression != null) {
        arguments = asList(new ExpressionArgument("single", expression, false));
      }
      abstractExpressionTransformer.setArguments(arguments);
      return abstractExpressionTransformer;
    };
  }

  private static ComponentBuildingDefinition.Builder getSetVariablePropertyBaseBuilder(ConfigurableInstanceFactory configurableInstanceFactory,
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
    return baseDefinition
        .withTypeDefinition(fromType(setterClass))
        .withObjectFactoryType(new ConfigurableObjectFactory<>().getClass())
        .withSetterParameterDefinition("factory", fromFixedValue(configurableInstanceFactory).build())
        .withSetterParameterDefinition("commonConfiguratorType", fromFixedValue(AddVariablePropertyConfigurator.class).build())
        .withSetterParameterDefinition("parameters",
                                       fromMultipleDefinitions(addAll(commonTransformerParameters, configurationAttributes))
                                           .build())
        .asPrototype();
  }

  private List<ComponentBuildingDefinition> getStreamingDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    buildingDefinitions.addAll(getBytesStreamingDefinitions());
    buildingDefinitions.addAll(getObjectsStreamingDefinitions());

    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getBytesStreamingDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    buildingDefinitions.add(baseDefinition
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
                                            fromSimpleParameter("maxBufferSize")
                                                .withDefaultValue(DEFAULT_BYTES_STREAMING_MAX_BUFFER_SIZE)
                                                .build())
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("bufferUnit", value -> DataUnit.valueOf((String) value))
                                                .withDefaultValue(DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT).build())
        .build());

    buildingDefinitions.add(baseDefinition
        .withIdentifier(NON_REPEATABLE_BYTE_STREAM_ALIAS)
        .withTypeDefinition(fromType(CursorStreamProviderFactory.class))
        .withObjectFactoryType(NullCursorStreamProviderObjectFactory.class)
        .build());

    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getObjectsStreamingDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    buildingDefinitions.add(baseDefinition
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
                                            fromSimpleParameter("maxBufferSize")
                                                .withDefaultValue(DEFAULT_OBJECT_STREAMING_MAX_BUFFER_SIZE)
                                                .build())
        .build());

    buildingDefinitions.add(baseDefinition
        .withIdentifier(NON_REPEATABLE_OBJECTS_STREAM_ALIAS)
        .withTypeDefinition(fromType(CursorIteratorProviderFactory.class))
        .withObjectFactoryType(NullCursorIteratorProviderObjectFactory.class)
        .build());

    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getReconnectionDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    ComponentBuildingDefinition.Builder baseReconnectDefinition = baseDefinition
        .withTypeDefinition(fromType(RetryPolicyTemplate.class)).withObjectFactoryType(RetryPolicyTemplateObjectFactory.class)
        .withSetterParameterDefinition("blocking", fromSimpleParameter("blocking").build())
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build());

    buildingDefinitions.add(baseReconnectDefinition.withIdentifier(RECONNECT_FOREVER_ELEMENT_IDENTIFIER)
        .withSetterParameterDefinition("count", fromFixedValue(RETRY_COUNT_FOREVER).build()).build());
    buildingDefinitions.add(baseReconnectDefinition.withIdentifier(RECONNECT_ELEMENT_IDENTIFIER)
        .withSetterParameterDefinition("retryNotifier", fromChildConfiguration(RetryNotifier.class).build())
        .withSetterParameterDefinition("count", fromSimpleParameter("count").build()).build());

    buildingDefinitions.add(baseDefinition
        .withIdentifier(RECONNECTION_ELEMENT_IDENTIFIER)
        .withTypeDefinition(fromType(ReconnectionConfig.class))
        .withObjectFactoryType(ReconnectionConfigObjectFactory.class)
        .withSetterParameterDefinition("failsDeployment", fromSimpleParameter("failsDeployment").build())
        .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicyTemplate.class).build())
        .build());

    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getTransactionDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    buildingDefinitions.add(baseDefinition.withIdentifier("xa-transaction")
        .withTypeDefinition(fromType(MuleTransactionConfig.class))
        .withSetterParameterDefinition("factory", fromFixedValue(new XaTransactionFactory()).build())
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("actionAsString", fromSimpleParameter("action").build())
        .withSetterParameterDefinition("interactWithExternal", fromSimpleParameter("interactWithExternal").build())
        .build());

    buildingDefinitions.add(baseDefinition
        .withIdentifier("environment")
        .withTypeDefinition(fromType(Map.class))
        .withObjectFactoryType(EnvironmentPropertyObjectFactory.class)
        .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build())
        .build());

    return buildingDefinitions;
  }

  private Builder getCoreTransformerBaseBuilder(final Class<? extends AbstractTransformer> transformerClass) {
    return getTransformerBaseBuilder(transformerClass).withNamespace(CORE_PREFIX);
  }

  private Builder getCoreMuleMessageTransformerBaseBuilder() {
    return getMuleMessageTransformerBaseBuilder().withNamespace(CORE_PREFIX);
  }

  private ComponentBuildingDefinition.Builder createTransactionManagerDefinitionBuilder(String transactionManagerName,
                                                                                        Class<?> transactionManagerClass) {
    return transactionManagerBaseDefinition.withIdentifier(transactionManagerName)
        .withTypeDefinition(fromType(transactionManagerClass));
  }

  /**
   * Parser for the expanded operations, generated dynamically by the {@link ApplicationModel} by reading the extensions
   *
   * @param componentBuildingDefinitions
   */
  private void addModuleOperationChainParser(LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions) {
    componentBuildingDefinitions.add(baseDefinition.withIdentifier("module-operation-chain")
        .withTypeDefinition(fromType(AnnotatedProcessor.class))
        .withObjectFactoryType(ModuleOperationMessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition("properties", fromChildMapConfiguration(String.class, String.class)
            .withWrapperIdentifier("module-operation-properties").build())
        .withSetterParameterDefinition("parameters", fromChildMapConfiguration(String.class, String.class)
            .withWrapperIdentifier("module-operation-parameters").build())
        .withSetterParameterDefinition("moduleName", fromSimpleParameter("moduleName").build())
        .withSetterParameterDefinition("moduleOperation", fromSimpleParameter("moduleOperation").build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asPrototype().build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("module-operation-properties")
        .withTypeDefinition(fromType(TreeMap.class)).build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier("module-operation-property-entry")
        .withTypeDefinition(fromMapEntryType(String.class, String.class))
        .build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier("module-operation-parameters")
        .withTypeDefinition(fromType(TreeMap.class)).build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier("module-operation-parameter-entry")
        .withTypeDefinition(fromMapEntryType(String.class, String.class))
        .build());
  }

}
