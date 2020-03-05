/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.empty;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.policy.api.PolicyPointcutParameters;

import java.util.Map;
import java.util.Optional;

/**
 * Holds all the context information for a source policy to function
 *
 * @since 4.3.0
 */
public class SourcePolicyContext implements EventInternalContext<SourcePolicyContext> {

  /**
   * Extracts an instance stored as an internal parameter in the given {@code result} under the {@link #SOURCE_POLICY_CONTEXT} key
   *
   * @param event
   * @return an {@link SourcePolicyContext} or {@code null} if none was set on the event
   */
  public static SourcePolicyContext from(CoreEvent event) {
    return (SourcePolicyContext) ((InternalEvent) event).<SourcePolicyContext>getSourcePolicyContext();
  }

  private final PolicyPointcutParameters pointcutParameters;

  private CoreEvent originalEvent;
  private MessageSourceResponseParametersProcessor responseParametersProcessor;
  private CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> processCallback;
  private Map<String, Object> originalResponseParameters;
  private Map<String, Object> originalFailureResponseParameters;
  private Optional<SourcePolicyParametersTransformer> parametersTransformer = empty();
  private Map<String, Map<String, TypedValue<?>>> variables = new SmallMap<>();

  public SourcePolicyContext(PolicyPointcutParameters pointcutParameters) {
    this.pointcutParameters = pointcutParameters;
  }

  public void configure(MessageSourceResponseParametersProcessor responseParametersProcessor,
                        CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback) {
    this.responseParametersProcessor = responseParametersProcessor;
    this.processCallback = callback;
  }

  @Override
  public SourcePolicyContext copy() {
    return this;
  }

  public PolicyPointcutParameters getPointcutParameters() {
    return pointcutParameters;
  }

  public CoreEvent getOriginalEvent() {
    return originalEvent;
  }

  public void setOriginalEvent(CoreEvent originalEvent) {
    this.originalEvent = originalEvent;
  }

  public MessageSourceResponseParametersProcessor getResponseParametersProcessor() {
    return responseParametersProcessor;
  }

  public CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> getProcessCallback() {
    return processCallback;
  }

  public Map<String, Object> getOriginalResponseParameters() {
    return originalResponseParameters;
  }

  public void setOriginalResponseParameters(Map<String, Object> originalResponseParameters) {
    this.originalResponseParameters = originalResponseParameters;
  }

  public Map<String, Object> getOriginalFailureResponseParameters() {
    return originalFailureResponseParameters;
  }

  public void setOriginalFailureResponseParameters(Map<String, Object> originalFailureResponseParameters) {
    this.originalFailureResponseParameters = originalFailureResponseParameters;
  }

  public void setParametersTransformer(Optional<SourcePolicyParametersTransformer> parametersTransformer) {
    this.parametersTransformer = parametersTransformer;
  }

  public Optional<SourcePolicyParametersTransformer> getParametersTransformer() {
    return parametersTransformer;
  }

  public Map<String, TypedValue<?>> getVariables(String policyId) {
    return variables.get(policyId);
  }

  public void addVariables(String policyId, Map<String, TypedValue<?>> variables) {
    this.variables.put(policyId, variables);
  }
}
