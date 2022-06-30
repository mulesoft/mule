/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal.dsl.model;

import static org.mule.runtime.api.tx.TransactionType.LOCAL;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.PARALLEL_FOREACH_ELEMENT;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.SCATTER_GATHER_ELEMENT;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionProviderUtils.createNewInstance;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionProviderUtils.getMuleMessageTransformerBaseBuilder;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionProviderUtils.getTransformerBaseBuilder;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.context.notification.AnySelector.ANY_SELECTOR;
import static org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair.ANY_SELECTOR_STRING;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.api.transaction.MuleTransactionConfig.ACTION_INDIFFERENT_STRING;
import static org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler.REUSE_GLOBAL_ERROR_HANDLER;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromTextContent;
import static org.mule.runtime.dsl.api.component.CommonTypeConverters.stringToClassConverter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
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
import static org.mule.runtime.internal.dsl.DslConstants.CRON_STRATEGY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.ERROR_MAPPING_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.FIXED_FREQUENCY_STRATEGY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECTION_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER;

import static org.apache.commons.lang3.ArrayUtils.addAll;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.api.util.DataUnit;
import org.mule.runtime.config.api.dsl.ConfigurableInstanceFactory;
import org.mule.runtime.config.api.dsl.ConfigurableObjectFactory;
import org.mule.runtime.config.internal.bean.CustomEncryptionStrategyDelegate;
import org.mule.runtime.config.internal.bean.CustomSecurityProviderDelegate;
import org.mule.runtime.config.internal.bean.NotificationConfig;
import org.mule.runtime.config.internal.bean.ServerNotificationManagerConfigurator;
import org.mule.runtime.config.internal.dsl.processor.EnvironmentPropertyObjectFactory;
import org.mule.runtime.config.internal.dsl.processor.ReconnectionConfigObjectFactory;
import org.mule.runtime.config.internal.dsl.processor.RetryPolicyTemplateObjectFactory;
import org.mule.runtime.config.internal.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.runtime.config.internal.factories.ChoiceRouterObjectFactory;
import org.mule.runtime.config.internal.factories.DefaultFlowFactoryBean;
import org.mule.runtime.config.internal.factories.DynamicConfigExpirationObjectFactory;
import org.mule.runtime.config.internal.factories.EnrichedErrorMappingsFactoryBean;
import org.mule.runtime.config.internal.factories.ErrorHandlerFactoryBean;
import org.mule.runtime.config.internal.factories.ExpirationPolicyObjectFactory;
import org.mule.runtime.config.internal.factories.FlowRefFactoryBean;
import org.mule.runtime.config.internal.factories.MuleConfigurationConfigurator;
import org.mule.runtime.config.internal.factories.OnErrorFactoryBean;
import org.mule.runtime.config.internal.factories.ProcessorExpressionRouteFactoryBean;
import org.mule.runtime.config.internal.factories.ProcessorRouteFactoryBean;
import org.mule.runtime.config.internal.factories.SchedulingMessageSourceFactoryBean;
import org.mule.runtime.config.internal.factories.SubflowMessageProcessorChainFactoryBean;
import org.mule.runtime.config.internal.factories.TryProcessorFactoryBean;
import org.mule.runtime.config.internal.factories.streaming.InMemoryCursorIteratorProviderObjectFactory;
import org.mule.runtime.config.internal.factories.streaming.InMemoryCursorStreamProviderObjectFactory;
import org.mule.runtime.config.internal.factories.streaming.NullCursorIteratorProviderObjectFactory;
import org.mule.runtime.config.internal.factories.streaming.NullCursorStreamProviderObjectFactory;
import org.mule.runtime.config.privileged.dsl.processor.AddVariablePropertyConfigurator;
import org.mule.runtime.config.privileged.dsl.processor.MessageProcessorChainFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationExtension;
import org.mule.runtime.core.api.config.DynamicConfigExpiration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.api.context.notification.ResourceIdentifierSelector;
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
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.el.mvel.configuration.AliasEntry;
import org.mule.runtime.core.internal.el.mvel.configuration.ImportEntry;
import org.mule.runtime.core.internal.el.mvel.configuration.MVELExpressionLanguageObjectFactory;
import org.mule.runtime.core.internal.el.mvel.configuration.MVELGlobalFunctionsConfig;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.OnErrorContinueHandler;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.internal.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.internal.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.internal.processor.LoggerMessageProcessor;
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
import org.mule.runtime.core.internal.routing.ParallelForEach;
import org.mule.runtime.core.internal.routing.ProcessorExpressionRoute;
import org.mule.runtime.core.internal.routing.ProcessorRoute;
import org.mule.runtime.core.internal.routing.RoundRobin;
import org.mule.runtime.core.internal.routing.ScatterGatherRouter;
import org.mule.runtime.core.internal.routing.UntilSuccessful;
import org.mule.runtime.core.internal.routing.forkjoin.CollectListForkJoinStrategyFactory;
import org.mule.runtime.core.internal.security.PasswordBasedEncryptionStrategy;
import org.mule.runtime.core.internal.security.SecretKeyEncryptionStrategy;
import org.mule.runtime.core.internal.security.filter.MuleEncryptionEndpointSecurityFilter;
import org.mule.runtime.core.internal.source.scheduler.DefaultSchedulerMessageSource;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.privileged.processor.AnnotatedProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.simple.AbstractAddVariablePropertyProcessor;
import org.mule.runtime.core.privileged.transaction.xa.XaTransactionFactory;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * {@link ComponentBuildingDefinition} definitions for the components provided by the core runtime.
 *
 * @since 4.0
 */
public class CoreComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  private static final String MESSAGE_PROCESSORS = "messageProcessors";
  private static final String NAME = "name";
  private static final String WHEN = "when";
  private static final String ERROR_HANDLER = "error-handler";
  private static final String ON_ERROR = "on-error";
  private static final String ON_ERROR_CONTINUE = "on-error-continue";
  private static final String ON_ERROR_PROPAGATE = "on-error-propagate";
  private static final String SET_PAYLOAD = "set-payload";
  private static final String LOGGER = "logger";
  private static final String ROUTE = "route";
  private static final String ROUTES = "routes";
  private static final String SUB_FLOW = "sub-flow";
  private static final String FLOW = "flow";
  private static final String FLOW_REF = "flow-ref";
  private static final String EXCEPTION_LISTENER_ATTRIBUTE = "exceptionListener";
  private static final String FORK_JOIN_STRATEGY = "forkJoinStrategyFactory";
  private static final String COLLECT_LIST = "collect-list";
  private static final String ASYNC = "async";
  private static final String TRY = "try";
  private static final String UNTIL_SUCCESSFUL = "until-successful";
  private static final String FOREACH = "foreach";
  private static final String FIRST_SUCCESSFUL = "first-successful";
  private static final String ROUND_ROBIN = "round-robin";
  private static final String CHOICE = "choice";
  private static final String OTHERWISE = "otherwise";
  private static final String SCHEDULER = "scheduler";
  private static final String ERROR_TYPE = "errorType";
  private static final String TYPE = "type";
  private static final String TX_ACTION = "transactionalAction";
  private static final String TX_TYPE = "transactionType";
  private static final String LOG_EXCEPTION = "logException";
  private static final String RAISE_ERROR = "raise-error";
  private static final String INHERIT_ITERABLE_REPEATABILITY = "inheritIterableRepeatability";

  @SuppressWarnings("rawtypes")
  private static ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(CORE_PREFIX);

  @Override
  public void init() {
    // Nothing to do
  }

  @SuppressWarnings("unchecked")
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
        .add(baseDefinition.withIdentifier(ON_ERROR).withTypeDefinition(fromType(TemplateOnErrorHandler.class))
            .withObjectFactoryType(OnErrorFactoryBean.class)
            .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build()).build());
    componentBuildingDefinitions.add(onErrorBaseBuilder.withIdentifier(ON_ERROR_CONTINUE)
        .withTypeDefinition(fromType(OnErrorContinueHandler.class))
        .asPrototype().build());
    componentBuildingDefinitions.add(onErrorBaseBuilder.withIdentifier(ON_ERROR_PROPAGATE)
        .withTypeDefinition(fromType(OnErrorPropagateHandler.class))
        .asPrototype().build());

    Builder errorHandlerBuilder = getErrorHandlerBuilder();
    componentBuildingDefinitions.add(errorHandlerBuilder.build());
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

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(ROUTE)
        .withTypeDefinition(fromType(MessageProcessorChain.class)).withObjectFactoryType(MessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(SUB_FLOW)
        .withTypeDefinition(fromType(MessageProcessorChain.class))
        .withObjectFactoryType(SubflowMessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build()).asPrototype().build());
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
        .withSetterParameterDefinition("target", fromSimpleParameter("target").build())
        .withSetterParameterDefinition("targetValue", fromSimpleParameter("targetValue")
            .withDefaultValue("#[payload]")
            .build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(COLLECT_LIST)
        .withTypeDefinition(fromType(CollectListForkJoinStrategyFactory.class))
        .build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(SCATTER_GATHER_ELEMENT)
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
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(PARALLEL_FOREACH_ELEMENT)
        .withTypeDefinition(fromType(ParallelForEach.class))
        .withSetterParameterDefinition("collectionExpression", fromSimpleParameter("collection").build())
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("maxConcurrency", fromSimpleParameter("maxConcurrency").build())
        .withSetterParameterDefinition("target", fromSimpleParameter("target").build())
        .withSetterParameterDefinition("targetValue", fromSimpleParameter("targetValue")
            .withDefaultValue("#[payload]")
            .build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asScope().build());
    componentBuildingDefinitions.add(baseDefinition
        .withIdentifier("collection").withTypeDefinition(fromType(String.class)).build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(ASYNC).withTypeDefinition(fromType(AsyncDelegateMessageProcessor.class))
            .withObjectFactoryType(AsyncMessageProcessorsFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build())
            .withSetterParameterDefinition("maxConcurrency", fromSimpleParameter("maxConcurrency").build())
            .build());
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
            .withSetterParameterDefinition("maxRetries", fromSimpleParameter("maxRetries").withDefaultValue("5").build())
            .withSetterParameterDefinition("millisBetweenRetries",
                                           fromSimpleParameter("millisBetweenRetries").withDefaultValue("60000").build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(FOREACH).withTypeDefinition(fromType(Foreach.class))
        .withSetterParameterDefinition("collectionExpression", fromSimpleParameter("collection").build())
        .withSetterParameterDefinition("batchSize", fromSimpleParameter("batchSize").build())
        .withSetterParameterDefinition("rootMessageVariableName", fromSimpleParameter("rootMessageVariableName").build())
        .withSetterParameterDefinition("counterVariableName", fromSimpleParameter("counterVariableName").build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(FIRST_SUCCESSFUL)
        .withTypeDefinition(fromType(FirstSuccessful.class))
        .withSetterParameterDefinition("routes", fromChildCollectionConfiguration(MessageProcessorChain.class).build())
        .build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(ROUND_ROBIN)
        .withTypeDefinition(fromType(RoundRobin.class))
        .withSetterParameterDefinition("routes", fromChildCollectionConfiguration(MessageProcessorChain.class).build())
        .build());
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(CHOICE).withTypeDefinition(fromType(ChoiceRouter.class))
        .withObjectFactoryType(ChoiceRouterObjectFactory.class)
        .withSetterParameterDefinition("routes", fromChildCollectionConfiguration(ProcessorExpressionRoute.class).build())
        .withSetterParameterDefinition("defaultRoute", fromChildConfiguration(ProcessorRoute.class).build())
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(WHEN).withTypeDefinition(fromType(ProcessorExpressionRoute.class))
            .withObjectFactoryType(ProcessorExpressionRouteFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .withSetterParameterDefinition("expression", fromSimpleParameter("expression").withDefaultValue("true").build())
            .build());
    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(OTHERWISE).withTypeDefinition(fromType(ProcessorRoute.class))
            .withObjectFactoryType(ProcessorRouteFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
            .build());

    componentBuildingDefinitions
        .add(baseDefinition.withIdentifier(ERROR_MAPPING_ELEMENT_IDENTIFIER)
            .withTypeDefinition(fromType(EnrichedErrorMapping.class))
            .withObjectFactoryType(EnrichedErrorMappingsFactoryBean.class)
            .withSetterParameterDefinition("source", fromSimpleParameter("sourceType").build())
            .withSetterParameterDefinition("target", fromSimpleParameter("targetType").build())
            .build());

    componentBuildingDefinitions.add(baseDefinition
        .withIdentifier(SCHEDULER)
        .withTypeDefinition(fromType(DefaultSchedulerMessageSource.class))
        .withObjectFactoryType(SchedulingMessageSourceFactoryBean.class)
        .withSetterParameterDefinition("disallowConcurrentExecution",
                                       fromSimpleParameter("disallowConcurrentExecution").withDefaultValue(false).build())
        .withSetterParameterDefinition("scheduler", fromChildConfiguration(PeriodicScheduler.class)
            .withWrapperIdentifier(SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(FIXED_FREQUENCY_STRATEGY_ELEMENT_IDENTIFIER)
        .withTypeDefinition(fromType(FixedFrequencyScheduler.class))
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
        .withSetterParameterDefinition("startDelay", fromSimpleParameter("startDelay").build())
        .withSetterParameterDefinition("timeUnit", fromSimpleParameter("timeUnit").build()).build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(CRON_STRATEGY_ELEMENT_IDENTIFIER)
        .withTypeDefinition(fromType(CronScheduler.class))
        .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
        .withSetterParameterDefinition("timeZone", fromSimpleParameter("timeZone").build()).build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier("configuration")
        .withTypeDefinition(fromType(MuleConfiguration.class))
        .withObjectFactoryType(MuleConfigurationConfigurator.class)
        .withSetterParameterDefinition("defaultErrorHandlerName",
                                       fromSimpleParameter("defaultErrorHandler-ref").build())
        .withSetterParameterDefinition("defaultResponseTimeout", fromSimpleParameter("defaultResponseTimeout").build())
        .withSetterParameterDefinition("maxQueueTransactionFilesSize",
                                       fromSimpleParameter("maxQueueTransactionFilesSize").build())
        .withSetterParameterDefinition("defaultTransactionTimeout", fromSimpleParameter("defaultTransactionTimeout").build())
        .withSetterParameterDefinition("shutdownTimeout", fromSimpleParameter("shutdownTimeout").build())
        .withSetterParameterDefinition("defaultObjectSerializer",
                                       fromSimpleReferenceParameter("defaultObjectSerializer-ref").build())
        .withSetterParameterDefinition("extensions", fromChildCollectionConfiguration(ConfigurationExtension.class).build())
        .withSetterParameterDefinition("dynamicConfigExpiration",
                                       fromChildConfiguration(DynamicConfigExpiration.class).build())
        .withSetterParameterDefinition("extensions", fromChildCollectionConfiguration(Object.class).build())
        .withSetterParameterDefinition(INHERIT_ITERABLE_REPEATABILITY,
                                       fromSimpleParameter(INHERIT_ITERABLE_REPEATABILITY).build())
        .withSetterParameterDefinition("correlationIdGeneratorExpression",
                                       fromSimpleParameter("correlationIdGeneratorExpression").build())
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
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .withSetterParameterDefinition("maxRedeliveryCount",
                                       fromSimpleParameter("maxRedeliveryCount").withDefaultValue(5).build())
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

    componentBuildingDefinitions.addAll(getMvelBuildingDefinitions());

    componentBuildingDefinitions.addAll(getTransformersBuildingDefinitions());

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

  protected Builder getErrorHandlerBuilder() {
    Builder errorHandlerBuilder = baseDefinition.withIdentifier(ERROR_HANDLER)
        .withTypeDefinition(fromType(ErrorHandler.class))
        .withObjectFactoryType(ErrorHandlerFactoryBean.class)
        .withSetterParameterDefinition("delegate", fromSimpleReferenceParameter("ref").build())
        .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build())
        .withSetterParameterDefinition("exceptionListeners",
                                       fromChildCollectionConfiguration(FlowExceptionHandler.class).build());
    if (!REUSE_GLOBAL_ERROR_HANDLER) {
      errorHandlerBuilder = errorHandlerBuilder.asPrototype();
    }
    return errorHandlerBuilder;
  }

  private TypeConverter<String, TransactionType> getTransactionTypeConverter() {
    return TransactionType::valueOf;
  }

  private TypeConverter<String, Predicate<? extends Notification>> getNotificationSubscriptionConverter() {
    return subscription -> {
      if (ANY_SELECTOR_STRING.equals(subscription)) {
        return ANY_SELECTOR;
      }
      return new ResourceIdentifierSelector(subscription);
    };
  }

  @SuppressWarnings("unchecked")
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

  private List<ComponentBuildingDefinition> getMvelBuildingDefinitions() {
    List<ComponentBuildingDefinition> mvelComponentBuildingDefinitions = new ArrayList<>();

    mvelComponentBuildingDefinitions.add(baseDefinition.withIdentifier("expression-language")
        .withTypeDefinition(fromType(MVELExpressionLanguage.class))
        .withObjectFactoryType(MVELExpressionLanguageObjectFactory.class)
        .withSetterParameterDefinition("autoResolveVariables", fromSimpleParameter("autoResolveVariables").build())
        .withSetterParameterDefinition("globalFunctions", fromChildConfiguration(MVELGlobalFunctionsConfig.class).build())
        .withSetterParameterDefinition("imports", fromChildCollectionConfiguration(ImportEntry.class).build())
        .withSetterParameterDefinition("aliases", fromChildCollectionConfiguration(AliasEntry.class).build())
        .build());

    mvelComponentBuildingDefinitions.add(baseDefinition.withIdentifier("import")
        .withTypeDefinition(fromType(ImportEntry.class))
        .withSetterParameterDefinition("key", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("class", stringToClassConverter()).build())
        .build());

    mvelComponentBuildingDefinitions.add(baseDefinition.withIdentifier("alias")
        .withTypeDefinition(fromType(AliasEntry.class))
        .withSetterParameterDefinition("key", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("expression").build())
        .build());

    mvelComponentBuildingDefinitions.add(baseDefinition.withIdentifier("global-functions")
        .withTypeDefinition(fromType(MVELGlobalFunctionsConfig.class))
        .withSetterParameterDefinition("file", fromSimpleParameter("file").build())
        .withSetterParameterDefinition("inlineScript", fromTextContent().build())
        .build());

    return mvelComponentBuildingDefinitions;
  }


  @SuppressWarnings("unchecked")
  private List<ComponentBuildingDefinition> getTransformersBuildingDefinitions() {
    List<ComponentBuildingDefinition> transformerComponentBuildingDefinitions = new ArrayList<>();

    transformerComponentBuildingDefinitions.add(baseDefinition
        .withIdentifier("parse-template")
        .withTypeDefinition(fromType(ParseTemplateProcessor.class))
        .withSetterParameterDefinition("content", fromChildConfiguration(String.class).withIdentifier("content").build())
        .withSetterParameterDefinition("outputMimeType", fromSimpleParameter("outputMimeType").build())
        .withSetterParameterDefinition("outputEncoding", fromSimpleParameter("outputEncoding").build())
        .withSetterParameterDefinition("target", fromSimpleParameter("target").build())
        .withSetterParameterDefinition("location", fromSimpleParameter("location").build())
        .withSetterParameterDefinition("targetValue", fromSimpleParameter("targetValue").build())
        .build());
    transformerComponentBuildingDefinitions.add(getCoreMuleMessageTransformerBaseBuilder()
        .withIdentifier("content").withTypeDefinition(fromType(String.class))
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

  @SuppressWarnings("unchecked")
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

  @SuppressWarnings("unchecked")
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
                                                .withDefaultValue(DEFAULT_BYTE_STREAMING_BUFFER_DATA_UNIT.name()).build())
        .build());

    buildingDefinitions.add(baseDefinition
        .withIdentifier(NON_REPEATABLE_BYTE_STREAM_ALIAS)
        .withTypeDefinition(fromType(CursorStreamProviderFactory.class))
        .withObjectFactoryType(NullCursorStreamProviderObjectFactory.class)
        .build());

    return buildingDefinitions;
  }

  @SuppressWarnings("unchecked")
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

  @SuppressWarnings("unchecked")
  private List<ComponentBuildingDefinition> getReconnectionDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();

    ComponentBuildingDefinition.Builder baseReconnectDefinition = baseDefinition
        .withTypeDefinition(fromType(RetryPolicyTemplate.class)).withObjectFactoryType(RetryPolicyTemplateObjectFactory.class)
        // 'blocking' configuration remains available only for compatibility use cases
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

  @SuppressWarnings("unchecked")
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

}
