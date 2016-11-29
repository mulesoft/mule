/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import org.mule.runtime.core.policy.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.core.policy.PolicyPointcutParameters;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Map;

/**
 * HTTP request operation policy pointcut parameters factory.
 *
 * @since 4.0
 */
public class HttpRequestPolicyPointcutParametersFactory implements OperationPolicyPointcutParametersFactory {

  protected static final String PATH_PARAMETER_NAME = "path";
  protected static final String METHOD_PARAMETER_NAME = "method";
  private final static ComponentIdentifier requestIdentifier =
      new ComponentIdentifier.Builder().withNamespace("http").withName("request").build();

  @Override
  public boolean supportsOperationIdentifier(ComponentIdentifier operationIdentifier) {
    return requestIdentifier.equals(operationIdentifier);
  }

  @Override
  public PolicyPointcutParameters createPolicyPointcutParameters(ComponentIdentifier operationIdentifier,
                                                                 Map<String, Object> operationParameters) {
    String pathParameter = (String) operationParameters.get(PATH_PARAMETER_NAME);
    String methodParameter = (String) operationParameters.get(METHOD_PARAMETER_NAME);
    return new HttpRequestPolicyPointcutParameters(operationIdentifier, pathParameter, methodParameter);
  }
}
