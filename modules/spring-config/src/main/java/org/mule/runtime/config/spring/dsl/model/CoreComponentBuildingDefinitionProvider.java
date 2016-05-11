/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.model;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildListConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromType;
import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.config.spring.dsl.processor.MessageEnricherObjectFactory;
import org.mule.runtime.config.spring.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.ChoiceRouterFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorFilterPairFactoryBean;
import org.mule.runtime.config.spring.factories.PollingMessageSourceFactoryBean;
import org.mule.runtime.config.spring.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.ScatterGatherRouterFactoryBean;
import org.mule.runtime.config.spring.factories.SubflowMessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.TransactionalMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.WatermarkFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.MessageInfoMapping;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.schedule.SchedulerFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.core.exception.CatchMessagingExceptionStrategy;
import org.mule.runtime.core.exception.ChoiceMessagingExceptionStrategy;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.exception.RedeliveryExceeded;
import org.mule.runtime.core.exception.RollbackMessagingExceptionStrategy;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.TransactionalMessageProcessor;
import org.mule.runtime.core.routing.AggregationStrategy;
import org.mule.runtime.core.routing.ChoiceRouter;
import org.mule.runtime.core.routing.FirstSuccessful;
import org.mule.runtime.core.routing.Foreach;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.routing.MessageProcessorFilterPair;
import org.mule.runtime.core.routing.RoundRobin;
import org.mule.runtime.core.routing.ScatterGatherRouter;
import org.mule.runtime.core.routing.UntilSuccessful;
import org.mule.runtime.core.routing.WireTap;
import org.mule.runtime.core.routing.filters.NotWildcardFilter;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;
import org.mule.runtime.core.source.polling.MessageProcessorPollingOverride;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencySchedulerFactory;
import org.mule.runtime.core.source.polling.watermark.Watermark;
import org.mule.runtime.core.transaction.lookup.GenericTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.JBossTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.JRunTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.Resin3TransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.WeblogicTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.WebsphereTransactionManagerLookupFactory;
import org.mule.runtime.core.transformer.simple.SetPayloadMessageProcessor;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition} definitions for the components
 * provided by the core runtime.
 *
 * @since 4.0
 */
public class CoreComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider
{

    public static final String MESSAGE_PROCESSORS = "messageProcessors";
    public static final String NAME = "name";
    public static final String EXCEPTION_STRATEGY = "exception-strategy";
    public static final String CATCH_EXCEPTION_STRATEGY = "catch-exception-strategy";
    public static final String WHEN = "when";
    public static final String ROLLBACK_EXCEPTION_STRATEGY = "rollback-exception-strategy";
    public static final String DEFAULT_EXCEPTION_STRATEGY = "default-exception-strategy";
    public static final String NAME_EXCEPTION_STRATEGY_ATTRIBUTE = "globalName";
    public static final String CUSTOM_EXCEPTION_STRATEGY = "custom-exception-strategy";
    public static final String CHOICE_EXCEPTION_STRATEGY = "choice-exception-strategy";
    public static final String SET_PAYLOAD = "set-payload";
    public static final String PROCESSOR_CHAIN = "processor-chain";
    public static final String PROCESSOR = "processor";
    public static final String TRANSFORMER = "transformer";
    public static final String FILTER = "filter";
    public static final String CUSTOM_PROCESSOR = "custom-processor";
    public static final String CLASS_ATTRIBUTE = "class";
    public static final String SUB_FLOW = "sub-flow";
    public static final String RESPONSE = "response";
    public static final String MESSAGE_FILTER = "message-filter";
    public static final String FLOW = "flow";
    public static final String EXCEPTION_LISTENER_ATTRIBUTE = "exceptionListener";
    public static final String SCATTER_GATHER = "scatter-gather";
    public static final String WIRE_TAP = "wire-tap";
    public static final String ENRICHER = "enricher";
    public static final String ASYNC = "async";
    public static final String TRANSACTIONAL = "transactional";
    public static final String UNTIL_SUCCESSFUL = "until-successful";
    public static final String FOREACH = "foreach";
    public static final String FIRST_SUCCESSFUL = "first-successful";
    public static final String ROUND_ROBIN = "round-robin";
    public static final String CHOICE = "choice";
    public static final String OTHERWISE = "otherwise";
    public static final String ALL = "all";
    public static final String POLL = "poll";

    private ComponentBuildingDefinition.Builder baseDefinition;
    private ComponentBuildingDefinition.Builder transactionManagerBaseDefinition;

    @Override
    public void init(MuleContext muleContext)
    {
        baseDefinition = new ComponentBuildingDefinition.Builder().withNamespace(CORE_NAMESPACE_NAME);
        transactionManagerBaseDefinition = baseDefinition.copy();
    }

    @Override
    public List<ComponentBuildingDefinition> getComponentBuildingDefinitions()
    {

        LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();


        AttributeDefinition messageProcessorListAttributeDefinition = fromChildListConfiguration(MessageProcessor.class).build();
        ComponentBuildingDefinition.Builder exceptionStrategyBaseBuilder = baseDefinition.copy()
                .withSetterParameterDefinition(MESSAGE_PROCESSORS, messageProcessorListAttributeDefinition)
                .withSetterParameterDefinition("globalName", fromSimpleParameter(NAME).build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(EXCEPTION_STRATEGY)
                                                 .withTypeDefinition(fromType(Object.class))
                                                 .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build())
                                                 .build());
        componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy()
                                                 .withIdentifier(CATCH_EXCEPTION_STRATEGY)
                                                 .withTypeDefinition(fromType(CatchMessagingExceptionStrategy.class))
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition(WHEN, fromSimpleParameter(WHEN).build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy()
                                                 .withIdentifier(ROLLBACK_EXCEPTION_STRATEGY)
                                                 .withTypeDefinition(fromType(RollbackMessagingExceptionStrategy.class))
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition(WHEN, fromSimpleParameter(WHEN).build())
                                                 .withSetterParameterDefinition("maxRedeliveryAttempts", fromSimpleParameter("maxRedeliveryAttempts").build())
                                                 .withSetterParameterDefinition("redeliveryExceeded", fromChildConfiguration(RedeliveryExceeded.class).build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("on-redelivery-attempts-exceeded")
                                                 .withTypeDefinition(fromType(RedeliveryExceeded.class))
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .asScope()
                                                 .build());
        componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy()
                                                 .withIdentifier(DEFAULT_EXCEPTION_STRATEGY)
                                                 .withTypeDefinition(fromType(DefaultMessagingExceptionStrategy.class))
                                                 .withSetterParameterDefinition(NAME_EXCEPTION_STRATEGY_ATTRIBUTE, fromSimpleParameter(NAME).build())
                                                 .withSetterParameterDefinition("stopMessageProcessing", fromSimpleParameter("stopMessageProcessing").build())
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("commitTxFilter", fromChildConfiguration(WildcardFilter.class).build())
                                                 .withSetterParameterDefinition("rollbackTxFilter", fromChildConfiguration(WildcardFilter.class).build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("commit-transaction")
                                                 .withTypeDefinition(fromType(WildcardFilter.class))
                                                 .withSetterParameterDefinition("pattern", fromSimpleParameter("exception-pattern").build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("rollback-transaction")
                                                 .withTypeDefinition(fromType(NotWildcardFilter.class))
                                                 .withSetterParameterDefinition("pattern", fromSimpleParameter("exception-pattern").build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(CUSTOM_EXCEPTION_STRATEGY)
                                                 .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(CHOICE_EXCEPTION_STRATEGY)
                                                 .withTypeDefinition(fromType(ChoiceMessagingExceptionStrategy.class))
                                                 .withSetterParameterDefinition("globalName", fromSimpleParameter(NAME).build())
                                                 .withSetterParameterDefinition("exceptionListeners", fromChildListConfiguration(MessagingExceptionHandler.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(SET_PAYLOAD)
                                                 .withTypeDefinition(fromType(SetPayloadMessageProcessor.class))
                                                 .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
                                                 .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
                                                 .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
                                                 .build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("jndi-transaction-manager", GenericTransactionManagerLookupFactory.class) //TODO add support for environment
                                                 .withSetterParameterDefinition("jndiName", fromSimpleParameter("jndiName").build())
                                                 .build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("weblogic-transaction-manager", WeblogicTransactionManagerLookupFactory.class).build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("jboss-transaction-manager", JBossTransactionManagerLookupFactory.class).build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("jrun-transaction-manager", JRunTransactionManagerLookupFactory.class).build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("resin-transaction-manager", Resin3TransactionManagerLookupFactory.class).build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("websphere-transaction-manager", WebsphereTransactionManagerLookupFactory.class).build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(PROCESSOR)
                                                 .withTypeDefinition(fromType(Object.class))
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(TRANSFORMER)
                                                 .withTypeDefinition(fromType(Object.class))
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(FILTER)
                                                 .withTypeDefinition(fromType(Object.class))
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(CUSTOM_PROCESSOR)
                                                 .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(PROCESSOR_CHAIN)
                                                 .withTypeDefinition(fromType(MessageProcessor.class))
                                                 .withObjectFactoryType(MessageProcessorChainFactoryBean.class)
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(SUB_FLOW)
                                                 .withTypeDefinition(fromType(MessageProcessor.class))
                                                 .withObjectFactoryType(SubflowMessageProcessorChainFactoryBean.class)
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(RESPONSE)
                                                 .withTypeDefinition(fromType(ResponseMessageProcessorAdapter.class))
                                                 .withObjectFactoryType(ResponseMessageProcessorsFactoryBean.class)
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(MESSAGE_FILTER)
                                                 .withTypeDefinition(fromType(MessageFilter.class))
                                                 .withConstructorParameterDefinition(fromChildConfiguration(Filter.class).build())
                                                 .withConstructorParameterDefinition(fromSimpleParameter("throwOnUnaccepted").withDefaultValue(false).build())
                                                 .withConstructorParameterDefinition(fromSimpleReferenceParameter("onUnaccepted").build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(FLOW)
                                                 .withTypeDefinition(fromType(Flow.class))
                                                 .withConstructorParameterDefinition(fromSimpleParameter(NAME).build())
                                                 .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
                                                 .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
                                                 .withSetterParameterDefinition("messageSource", fromChildConfiguration(MessageSource.class).build())
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition(EXCEPTION_LISTENER_ATTRIBUTE, fromChildConfiguration(MessagingExceptionHandler.class).build())
                                                 .withSetterParameterDefinition("processingStrategy", fromSimpleReferenceParameter("processingStrategy").build())
                                                 .withSetterParameterDefinition("messageInfoMapping", fromChildConfiguration(MessageInfoMapping.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(SCATTER_GATHER)
                                                 .withTypeDefinition(fromType(ScatterGatherRouter.class))
                                                 .withObjectFactoryType(ScatterGatherRouterFactoryBean.class)
                                                 .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
                                                 .withSetterParameterDefinition("aggregationStrategy", fromChildConfiguration(AggregationStrategy.class).build())
                                                 .withSetterParameterDefinition("threadingProfile", fromChildConfiguration(ThreadingProfile.class).build())
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .asScope()
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(WIRE_TAP)
                                                 .withTypeDefinition(fromType(WireTap.class))
                                                 .withSetterParameterDefinition("tap", fromChildConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("filter", fromChildConfiguration(Filter.class).build())
                                                 .asScope()
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(ENRICHER)
                                                 .withObjectFactoryType(MessageEnricherObjectFactory.class)
                                                 .withTypeDefinition(fromType(MessageEnricher.class))
                                                 .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("enrichExpressionPairs", fromChildListConfiguration(MessageEnricher.EnrichExpressionPair.class).build())
                                                 .withSetterParameterDefinition("source", fromSimpleParameter("source").build())
                                                 .withSetterParameterDefinition("target", fromSimpleParameter("target").build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("enrich")
                                                 .withTypeDefinition(fromType(MessageEnricher.EnrichExpressionPair.class))
                                                 .withConstructorParameterDefinition(fromSimpleParameter("source").build())
                                                 .withConstructorParameterDefinition(fromSimpleParameter("target").build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(ASYNC)
                                                 .withTypeDefinition(fromType(AsyncDelegateMessageProcessor.class))
                                                 .withObjectFactoryType(AsyncMessageProcessorsFactoryBean.class)
                                                 .withSetterParameterDefinition("processingStrategy", fromSimpleReferenceParameter("processingStrategy").build())
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(TRANSACTIONAL)
                                                 .withTypeDefinition(fromType(TransactionalMessageProcessor.class))
                                                 .withObjectFactoryType(TransactionalMessageProcessorsFactoryBean.class)
                                                 .withSetterParameterDefinition("exceptionListener", fromChildConfiguration(MessagingExceptionHandler.class).build())
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("action", fromSimpleParameter("action").build())
                                                 .build());

        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(UNTIL_SUCCESSFUL)
                                                 .withTypeDefinition(fromType(UntilSuccessful.class))
                                                 .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("objectStore-ref").build())
                                                 .withSetterParameterDefinition("deadLetterQueue", fromSimpleReferenceParameter("deadLetterQueue-ref").build())
                                                 .withSetterParameterDefinition("maxRetries", fromSimpleParameter("maxRetries").build())
                                                 .withSetterParameterDefinition("millisBetweenRetries", fromSimpleParameter("millisBetweenRetries").build())
                                                 .withSetterParameterDefinition("secondsBetweenRetries", fromSimpleParameter("secondsBetweenRetries").build())
                                                 .withSetterParameterDefinition("failureExpression", fromSimpleParameter("failureExpression").build())
                                                 .withSetterParameterDefinition("ackExpression", fromSimpleParameter("ackExpression").build())
                                                 .withSetterParameterDefinition("synchronous", fromSimpleParameter("synchronous").build())
                                                 .withSetterParameterDefinition("threadingProfile", fromChildConfiguration(ThreadingProfile.class).build())
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(FOREACH)
                                                 .withTypeDefinition(fromType(Foreach.class))
                                                 .withSetterParameterDefinition("collectionExpression", fromSimpleParameter("collection").build())
                                                 .withSetterParameterDefinition("batchSize", fromSimpleParameter("batchSize").build())
                                                 .withSetterParameterDefinition("rootMessageVariableName", fromSimpleParameter("rootMessageVariableName").build())
                                                 .withSetterParameterDefinition("counterVariableName", fromSimpleParameter("counterVariableName").build())
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(FIRST_SUCCESSFUL)
                                                 .withTypeDefinition(fromType(FirstSuccessful.class))
                                                 .withSetterParameterDefinition("failureExpression", fromSimpleParameter("failureExpression").build())
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(ROUND_ROBIN)
                                                 .withTypeDefinition(fromType(RoundRobin.class))
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(CHOICE)
                                                 .withTypeDefinition(fromType(ChoiceRouter.class))
                                                 .withObjectFactoryType(ChoiceRouterFactoryBean.class)
                                                 .withSetterParameterDefinition("routes", fromChildListConfiguration(MessageProcessorFilterPair.class).build())
                                                 .withSetterParameterDefinition("defaultRoute", fromChildConfiguration(MessageProcessorFilterPair.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(WHEN)
                                                 .withTypeDefinition(fromType(MessageProcessorFilterPair.class))
                                                 .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(OTHERWISE)
                                                 .withTypeDefinition(fromType(MessageProcessorFilterPair.class))
                                                 .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("expression", fromFixedValue("true").build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(ALL)
                                                 .withTypeDefinition(fromType(MulticastingRouter.class))
                                                 .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier(POLL)
                                                 .withTypeDefinition(fromType(PollingMessageSource.class))
                                                 .withObjectFactoryType(PollingMessageSourceFactoryBean.class)
                                                 .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
                                                 .withSetterParameterDefinition("override", fromChildConfiguration(MessageProcessorPollingOverride.class).build())
                                                 .withSetterParameterDefinition("schedulerFactory", fromChildConfiguration(SchedulerFactory.class).build())
                                                 .build());

        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("fixed-frequency-scheduler")
                                                 .withTypeDefinition(fromType(FixedFrequencySchedulerFactory.class))
                                                 .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
                                                 .withSetterParameterDefinition("startDelay", fromSimpleParameter("startDelay").build())
                                                 .withSetterParameterDefinition("timeUnit", fromSimpleParameter("timeUnit").build())
                                                 .build());
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("watermark")
                                                 .withSetterParameterDefinition("variable", fromSimpleParameter("variable").build())
                                                 .withSetterParameterDefinition("defaultExpression", fromSimpleParameter("default-expression").build())
                                                 .withSetterParameterDefinition("updateExpression", fromSimpleParameter("update-expression").build())
                                                 .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("object-store-ref").build())
                                                 .withSetterParameterDefinition("selector", fromSimpleParameter("selector").build())
                                                 .withSetterParameterDefinition("selectorExpression", fromSimpleParameter("selector-expression").build())
                                                 .withTypeDefinition(fromType(Watermark.class))
                                                 .withObjectFactoryType(WatermarkFactoryBean.class)
                                                 .build());
        return componentBuildingDefinitions;
    }

    private ComponentBuildingDefinition.Builder createTransactionManagerDefinitionBuilder(String transactionManagerName, Class<?> transactionManagerClass)
    {
        return transactionManagerBaseDefinition.copy().withIdentifier(transactionManagerName).withTypeDefinition(fromType(transactionManagerClass));
    }

}
