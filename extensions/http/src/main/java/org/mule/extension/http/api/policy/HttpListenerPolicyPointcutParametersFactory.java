/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.StringUtils.isNotEmpty;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.core.policy.PolicyPointcutParameters;
import org.mule.runtime.core.policy.SourcePolicyPointcutParametersFactory;

/**
 * HTTP request operation policy pointcut parameters factory.
 * 
 * @since 4.0
 */
public class HttpListenerPolicyPointcutParametersFactory implements SourcePolicyPointcutParametersFactory {

  private final static ComponentIdentifier listenerIdentifier =
      builder().withPrefix("http").withName("listener").build();

  @Override
  public boolean supportsSourceIdentifier(ComponentIdentifier sourceIdentifier) {
    return listenerIdentifier.equals(sourceIdentifier);
  }

  @Override
  public PolicyPointcutParameters createPolicyPointcutParameters(String flowName, ComponentIdentifier sourceIdentifier,
                                                                 Attributes attributes) {
    checkArgument(isNotEmpty(flowName), "Cannot create a policy pointcut parameter instance with an empty flow name");
    checkArgument(attributes instanceof HttpRequestAttributes, String
        .format("Cannot create a policy pointcut parameter instance from a message which attributes is not an instance of %s, the current attribute instance type is: %s",
                HttpRequestAttributes.class.getName(), attributes != null ? attributes.getClass().getName() : "null"));
    HttpRequestAttributes httpRequestAttributes = (HttpRequestAttributes) attributes;
    return new HttpListenerPolicyPointcutParameters(flowName, sourceIdentifier, httpRequestAttributes.getRequestPath(),
                                                    httpRequestAttributes.getMethod());
  }

}
