/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.policy;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.http.api.policy.HttpRequestPolicyPointcutParameters;
import org.mule.extension.http.api.policy.HttpRequestPolicyPointcutParametersFactory;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.junit.Test;

public class HttpRequestPolicyPointcutParametersFactoryTestCase extends AbstractMuleTestCase {

  private static final ComponentIdentifier HTTP_REQUEST_COMPONENT_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace("http").withName("request").build();
  private static final String TEST_REQUEST_PATH = "test-request-path";
  private static final String TEST_METHOD = "PUT";

  private final HttpRequestPolicyPointcutParametersFactory factory = new HttpRequestPolicyPointcutParametersFactory();

  @Test
  public void supportsHttpListener() {
    assertThat(factory
        .supportsOperationIdentifier(HTTP_REQUEST_COMPONENT_IDENTIFIER),
               is(true));;
  }

  @Test
  public void doesNotSupportHttpRequester() {
    assertThat(factory
        .supportsOperationIdentifier(new ComponentIdentifier.Builder().withNamespace("http").withName("listener").build()),
               is(false));;
  }

  @Test
  public void policyPointcutParameters() {
    Map<String, Object> parametersMap =
        ImmutableMap.<String, Object>builder().put("method", TEST_METHOD).put("path", TEST_REQUEST_PATH).build();
    HttpRequestPolicyPointcutParameters policyPointcutParameters = (HttpRequestPolicyPointcutParameters) factory
        .createPolicyPointcutParameters(HTTP_REQUEST_COMPONENT_IDENTIFIER, parametersMap);
    assertThat(policyPointcutParameters.getComponentIdentifier(), is(HTTP_REQUEST_COMPONENT_IDENTIFIER));
    assertThat(policyPointcutParameters.getPath(), is(TEST_REQUEST_PATH));
    assertThat(policyPointcutParameters.getMethod(), is(TEST_METHOD));
  }

}
