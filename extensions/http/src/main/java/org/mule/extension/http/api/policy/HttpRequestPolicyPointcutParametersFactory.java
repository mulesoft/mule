/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;


import static org.mule.runtime.core.util.StringUtils.isNotEmpty;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.core.policy.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.core.policy.PolicyPointcutParameters;

import java.util.Map;

/**
 * HTTP request operation policy pointcut parameters factory.
 *
 * @since 4.0
 */
public class HttpRequestPolicyPointcutParametersFactory implements OperationPolicyPointcutParametersFactory {

  public static final String PATH_PARAMETER_NAME = "path";
  public static final String METHOD_PARAMETER_NAME = "method";
  private final static ComponentIdentifier requestIdentifier =
      builder().withPrefix("http").withName("request").build();

  @Override
  public boolean supportsOperationIdentifier(ComponentIdentifier operationIdentifier) {
    return requestIdentifier.equals(operationIdentifier);
  }

  @Override
  public PolicyPointcutParameters createPolicyPointcutParameters(String flowName, ComponentIdentifier operationIdentifier,
                                                                 Map<String, Object> operationParameters) {
    checkArgument(isNotEmpty(flowName), "Cannot create a policy pointcut parameter instance with an empty flow name");
    String pathParameter = (String) operationParameters.get(PATH_PARAMETER_NAME);
    String methodParameter = (String) operationParameters.get(METHOD_PARAMETER_NAME);
    return new HttpRequestPolicyPointcutParameters(flowName, operationIdentifier, pathParameter, methodParameter);
  }
}
