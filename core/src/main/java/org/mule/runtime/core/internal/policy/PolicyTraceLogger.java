/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.util.Map;

import org.slf4j.Logger;

/**
 * Logs the in-flight {@link CoreEvent} as it goes through the different policies
 *
 * @since 4.3
 */
class PolicyTraceLogger {

  private static final Logger LOGGER = getLogger(PolicyTraceLogger.class);
  private static final String NL = System.lineSeparator();
  private static final String TAB = "   ";

  /**
   * Logs the event before starting to execute the source policy
   */
  public void logSourcePolicyStart(Policy policy, CoreEvent event) {
    if (LOGGER.isTraceEnabled()) {
      // Setting event in thread local so the correlation Id is printed with the log message
      setCurrentEvent((PrivilegedEvent) event);
      LOGGER.trace(NL + "Executing policy " + getPolicyName(policy) + NL + eventAsString(event));
    }
  }

  /**
   * Logs the event before exiting current policy because of the execution of an execute-next element
   */
  public void logBeforeExecuteNext(String policyId, CoreEvent event) {
    if (LOGGER.isTraceEnabled()) {
      logEvent(NL + "Jumping to next from policy " + policyId, event);
    }
  }

  /**
   * Logs the event after execute-next is completed and resuming execution of a policy
   */
  public void logAfterExecuteNext(String policyId, CoreEvent event) {
    if (LOGGER.isTraceEnabled()) {
      logEvent(NL + "Resuming execution of policy " + policyId, event);
    }
  }

  /**
   * Logs the event before starting to execute the operation policy
   */
  public void logOperationPolicyStart(Policy policy, CoreEvent event) {
    if (LOGGER.isTraceEnabled()) {
      logEvent(NL + "Executing operation policy " + getPolicyName(policy), event);
    }
  }

  /**
   * Logs the event after execution of the source policy finished
   */
  public void logSourcePolicyEnd(Policy policy, CoreEvent event) {
    if (LOGGER.isTraceEnabled()) {
      logEvent(NL + "Policy " + getPolicyName(policy) + " execution finished", event);
    }
  }

  /**
   * Logs the event after execution of the operation policy finished
   */
  public void logOperationPolicyEnd(Policy policy, CoreEvent event) {
    if (LOGGER.isTraceEnabled()) {
      logEvent(NL + "Operation policy " + getPolicyName(policy) + " execution finished", event);
    }
  }

  public void logSourcePolicyFailureResult(SourcePolicyFailureResult result) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Event id: " + result.getMessagingException().getEvent().getContext().getId()
          + "\nFinished processing with failure. \n" +
          "Error message: " + result.getMessagingException().getMessage());
    }
  }

  private void logEvent(String initialLine, CoreEvent event) {
    LOGGER.trace(initialLine + NL + eventAsString(event));
  }

  private String eventAsString(CoreEvent event) {
    StringBuilder builder = new StringBuilder();
    builder.append("Attributes: ").append(attributesAsString(event))
        .append(NL).append("Variables: ").append(variablesAsString(event.getVariables()));

    event.getAuthentication()
        .ifPresent(authentication -> builder.append(NL).append("Authentication: ").append(event.getAuthentication()));

    return builder.toString();
  }

  private String attributesAsString(CoreEvent event) {
    if (event.getMessage() == null || event.getMessage().getAttributes() == null
        || event.getMessage().getAttributes().getValue() == null) {
      return "";
    }
    return event.getMessage().getAttributes().getValue().toString();
  }

  private String variablesAsString(Map<String, TypedValue<?>> variables) {
    StringBuilder builder = new StringBuilder();
    builder.append("{");

    if (!variables.isEmpty()) {
      builder.append(NL);
    }

    variables.entrySet().forEach(variable -> builder.append(TAB).append(variable).append(NL));

    builder.append("}");
    return builder.toString();
  }

  private String getPolicyName(Policy policy) {
    return policy.getPolicyId();
  }

}
