/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.internal.policy.CommonSourcePolicy.POLICY_SOURCE_PARAMETERS_PROCESSOR;
import static org.mule.runtime.core.internal.policy.CompositeOperationPolicy.POLICY_OPERATION_NEXT_OPERATION_RESPONSE;
import static org.mule.runtime.core.internal.policy.CompositeSourcePolicy.POLICY_SOURCE_ORIGINAL_FAILURE_RESPONSE_PARAMETERS;
import static org.mule.runtime.core.internal.policy.CompositeSourcePolicy.POLICY_SOURCE_ORIGINAL_RESPONSE_PARAMETERS;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_EVENT_CTX_IDS;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.collection.FastMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.internal.event.EventInternalContextResolver;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;

/**
 * Knows how to modify events when jumping between policies and between a policy and the flow/operation
 */
public class PolicyEventMapper {

  private static final String POLICY_VARS_PREFIX = "policy.vars.";
  private static final String POLICY_SOURCE_ORIGINAL_EVENT = "policy.source.originalEvent";
  private static final String POLICY_OPERATION_ORIGINAL_EVENT = "policy.operation.originalEvent";
  private static final Logger LOGGER = getLogger(PolicyEventMapper.class);

  private final String policyVarsInternalParameterName;
  private EventInternalContextResolver<Set<String>> nextEventIdCtxResolver =
      new EventInternalContextResolver<>(POLICY_NEXT_EVENT_CTX_IDS, () -> emptySet());

  public PolicyEventMapper() {
    this(null);
  }

  public PolicyEventMapper(String policyId) {
    policyVarsInternalParameterName = POLICY_VARS_PREFIX + policyId;
  }

  /**
   * Clears any variable that was added by a previous policy and stores the current event as an internal parameter to later
   * restore the message if propagation is disabled and to restore variables when policy is completed.
   *
   * @param event the event at the start of the source policy
   */
  public CoreEvent onSourcePolicyBegin(CoreEvent event) {
    return InternalEvent
        .builder(event)
        .addInternalParameter(POLICY_SOURCE_ORIGINAL_EVENT, event)
        .clearVariables()
        .build();
  }

  /**
   * Restores, if any, the variables of the source part of the policy, as variables are shared between source and operation within
   * the same policy. At this point it may contain variables added in the flow.
   *
   * @param event the event just before the operation is executed
   */
  public CoreEvent onOperationPolicyBegin(CoreEvent event) {
    Map<String, TypedValue<?>> variables = loadVars(event);

    return InternalEvent
        .builder(event)
        .addInternalParameter(POLICY_OPERATION_ORIGINAL_EVENT, event)
        .variablesTyped(variables != null ? variables : emptyMap())
        .build();
  }

  /**
   * This point is reached when an operation policy is processed successfully or an error thrown by the operation or policy was
   * correctly handled. <br />
   * <br />
   * If operation was executed successfully: <br />
   *
   * - restores variables from the operation result. This is to not lose variables added by operation if, for example, the
   * operation has defined a target. In the case that after this policy, there is another policy applied, variables of that second
   * policy will be restored when {@link #fromPolicyNext} is executed. <br />
   * - message modifications done by the policy chain after the operation was executed are propagated if propagation flag was
   * enabled. Otherwise, the operation result message is restored. <br />
   * <br />
   *
   * If operation was not executed: <br />
   *
   * - restore variables from the operation policy initial event <br />
   * - message is propagated as it is <br />
   *
   * @param result the event after the operation policy was completed
   * @param propagate whether changes done after the operation was executed should be propagated or not
   */
  public CoreEvent onOperationPolicyFinish(CoreEvent result, boolean propagate) {
    final InternalEvent operationResult =
        ((InternalEvent) result).getInternalParameter(POLICY_OPERATION_NEXT_OPERATION_RESPONSE);

    if (operationResult == null) {
      return InternalEvent.builder(result)
          .addInternalParameter(policyVarsInternalParameterName(), result.getVariables())
          .variablesTyped(getOperationOriginalEvent(result).getVariables())
          .build();

    } else {
      Message message = propagate ? result.getMessage() : operationResult.getMessage();

      CoreEvent next = CoreEvent.builder(result)
          .message(message)
          .variables(operationResult.getVariables())
          .build();

      // Additional copy is needed to update next_operation_response to be the just created event
      return quickCopy(next, FastMap.of(policyVarsInternalParameterName(), result.getVariables(),
                                        POLICY_OPERATION_NEXT_OPERATION_RESPONSE, next));
    }
  }

  /**
   * This point is reached when an error is thrown in the operation or the policy and is not handled. <br />
   * <br />
   * If error thrown after operation was executed successfully: <br />
   *
   * - restores variables from the operation result. This is to not lose variables added by operation if, for example, the
   * operation has defined a target. In the case that after this policy, there is another policy applied, variables of that second
   * policy will be restored when {@link #fromPolicyNext} is executed. <br />
   * - message is propagated as it is <br />
   * <br />
   *
   * If operation was not executed or failed: <br />
   *
   * - restore variables from the operation policy initial event <br />
   * - message is propagated as it is <br />
   *
   * @param result the event after the operation policy error handler was executed
   */
  public CoreEvent onOperationPolicyError(CoreEvent result) {
    final InternalEvent nextOperationResponse =
        ((InternalEvent) result).getInternalParameter(POLICY_OPERATION_NEXT_OPERATION_RESPONSE);

    if (nextOperationResponse == null) {
      // Operation was not executed or failed, so variables from the initial event are used
      return InternalEvent.builder(result)
          .addInternalParameter(policyVarsInternalParameterName(), result.getVariables())
          .variablesTyped(getOperationOriginalEvent(result).getVariables())
          .build();
    } else {
      // Operation was successfully executed, so variables from the operation response are used
      return InternalEvent.builder(result)
          .addInternalParameter(policyVarsInternalParameterName(), result.getVariables())
          .addInternalParameter(POLICY_OPERATION_NEXT_OPERATION_RESPONSE, nextOperationResponse)
          .variablesTyped(nextOperationResponse.getVariables())
          .build();
    }
  }

  /**
   * As current policy is exiting, current variables are stored as an internal parameter and variables are restored from the
   * previously stored event. Also, message is restored or current one is preserved depending on propagation config.
   *
   * @param event the event after the policy processors defined before the execute-next were executed
   * @param propagate whether modifications to the message done by the executed policy processors should be propagated or not
   */
  public CoreEvent onSourcePolicyNext(CoreEvent event, boolean propagate) {
    return onPolicyNext(event, propagate);
  }

  /**
   * As current policy is exiting, current variables are stored as an internal parameter and variables are restored from the
   * previously stored event. Message is always propagated and no restoration is required.
   *
   * @param event the event after the policy processors defined before the execute-next were executed
   */
  public CoreEvent onOperationPolicyNext(CoreEvent event) {
    return onPolicyNext(event, true);
  }

  /**
   * After a second policy, flow or operation was completed, it returns to the calling policy. Variables of the calling policy are
   * restored, since current event is helding variables defined by the exiting chain.
   *
   * @param event the event after the second policy, flow or operation was completed
   */
  public CoreEvent fromPolicyNext(CoreEvent event) {
    Map<String, TypedValue<?>> variables = loadVars(event);

    return InternalEvent
        .builder(event)
        .variablesTyped(variables != null ? variables : emptyMap())
        .build();
  }

  /**
   * After flow execution is completed, the result is transformed using the <code>parametersTransformer</code> from the response
   * parameters into a {@link Message}. Those response parameters are preserved as they are needed later. Variables are left as
   * they are since they will be restored when source policy's {@link #fromPolicyNext} is executed.
   *
   * @param flowResult the event after flow was completed
   * @param parametersTransformer does the transformation from flow's response parameters into a {@link Message}.
   */
  public CoreEvent onFlowFinish(CoreEvent flowResult,
                                Optional<SourcePolicyParametersTransformer> parametersTransformer)
      throws MessagingException {
    try {
      Map<String, Object> originalResponseParameters =
          getResponseParamsProcessor(flowResult)
              .getSuccessfulExecutionResponseParametersFunction()
              .apply(flowResult);

      Message message =
          parametersTransformer
              .map(t -> t.fromSuccessResponseParametersToMessage(originalResponseParameters))
              .orElseGet(flowResult::getMessage);

      return InternalEvent.builder(flowResult)
          .message(message)
          .addInternalParameter(POLICY_SOURCE_ORIGINAL_RESPONSE_PARAMETERS, originalResponseParameters)
          .build();
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Error after Flow finished execution", e);
      }
      throw new MessagingException(flowResult, e);
    }
  }

  /**
   * After flow execution is completed with an error, the result is transformed using the <code>parametersTransformer</code> from
   * the error response parameters into a {@link Message}. Those error response parameters are preserved as they are needed later.
   * Variables are also restored since when this method is called, the source policy's {@link #fromPolicyNext} won't be.
   *
   * @param event the event after the flow's error-handler is executed
   * @param policyId the nearest to the flow (with the greater order) source policy.
   * @param parametersTransformer does the transformation from flow's response parameters into a {@link Message}.
   */
  public CoreEvent onFlowError(CoreEvent event, String policyId,
                               Optional<SourcePolicyParametersTransformer> parametersTransformer) {
    try {
      Map<String, Object> originalFailureResponseParameters =
          getResponseParamsProcessor(event)
              .getFailedExecutionResponseParametersFunction()
              .apply(event);

      Message message =
          parametersTransformer
              .map(t -> t.fromFailureResponseParametersToMessage(originalFailureResponseParameters))
              .orElse(event.getMessage());

      Map<String, TypedValue<?>> variables = loadVars(event, policyId);

      return InternalEvent.builder(event)
          .message(message)
          .addInternalParameter(POLICY_SOURCE_ORIGINAL_FAILURE_RESPONSE_PARAMETERS, originalFailureResponseParameters)
          .variablesTyped(variables != null ? variables : emptyMap())
          .build();
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Error after Flow finished execution", e);
      }
      Map<String, TypedValue<?>> variables = loadVars(event, policyId);

      return InternalEvent.builder(event)
          .variablesTyped(variables != null ? variables : emptyMap())
          .build();
    }
  }

  private CoreEvent onPolicyNext(CoreEvent event, boolean propagate) {
    PrivilegedEvent originalEvent = getOriginalEvent(event);

    Set<String> currentNextEventIdCtx = nextEventIdCtxResolver.getCurrentContextFromEvent(event);

    return InternalEvent
        .builder(event)
        .message(propagate ? event.getMessage() : originalEvent.getMessage())
        .variables(originalEvent.getVariables())
        .addInternalParameter(policyVarsInternalParameterName(), event.getVariables())
        .addInternalParameter(POLICY_NEXT_EVENT_CTX_IDS,
                              ImmutableSet.<String>builder().addAll(currentNextEventIdCtx).add(event.getContext().getId())
                                  .build())
        .build();
  }

  private String policyVarsInternalParameterName() {
    return policyVarsInternalParameterName;
  }

  private String policyVarsInternalParameterName(String policyId) {
    return POLICY_VARS_PREFIX + policyId;
  }

  private PrivilegedEvent getOriginalEvent(CoreEvent event) {
    final PrivilegedEvent operationOriginalEvent = getOperationOriginalEvent(event);
    return operationOriginalEvent != null ? operationOriginalEvent : getSourceOriginalEvent(event);
  }

  private PrivilegedEvent getOperationOriginalEvent(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(POLICY_OPERATION_ORIGINAL_EVENT);
  }

  private PrivilegedEvent getSourceOriginalEvent(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(POLICY_SOURCE_ORIGINAL_EVENT);
  }

  private Map<String, TypedValue<?>> loadVars(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(policyVarsInternalParameterName());
  }

  private Map<String, TypedValue<?>> loadVars(CoreEvent event, String policyId) {
    return ((InternalEvent) event).getInternalParameter(policyVarsInternalParameterName(policyId));
  }

  private MessageSourceResponseParametersProcessor getResponseParamsProcessor(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(POLICY_SOURCE_PARAMETERS_PROCESSOR);
  }
}
