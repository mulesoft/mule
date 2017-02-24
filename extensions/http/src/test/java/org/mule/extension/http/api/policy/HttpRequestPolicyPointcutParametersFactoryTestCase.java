/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import static java.util.Collections.emptyMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.http.api.policy.HttpRequestPolicyPointcutParametersFactory.PATH_PARAMETER_NAME;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.junit.Test;

public class HttpRequestPolicyPointcutParametersFactoryTestCase extends AbstractMuleTestCase {

  private static final ComponentIdentifier HTTP_REQUEST_COMPONENT_IDENTIFIER =
      builder().withPrefix("http").withName("request").build();
  private static final String TEST_REQUEST_PATH = "test-request-path";
  private static final String TEST_METHOD = "PUT";
  private static final String FLOW_NAME = "flow-name";

  private final HttpRequestPolicyPointcutParametersFactory factory = new HttpRequestPolicyPointcutParametersFactory();

  @Test
  public void supportsHttpRequest() {
    assertThat(factory
        .supportsOperationIdentifier(HTTP_REQUEST_COMPONENT_IDENTIFIER),
               is(true));
  }

  @Test
  public void doesNotSupportHttpListener() {
    assertThat(factory
        .supportsOperationIdentifier(builder().withPrefix("http").withName("listener").build()),
               is(false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void failIfFlowNameIsEmpty() {
    factory.createPolicyPointcutParameters("", HTTP_REQUEST_COMPONENT_IDENTIFIER, emptyMap());
  }

  @Test
  public void policyPointcutParameters() {
    Map<String, Object> parametersMap =
        ImmutableMap.<String, Object>builder().put(HttpRequestPolicyPointcutParametersFactory.METHOD_PARAMETER_NAME, TEST_METHOD)
            .put(PATH_PARAMETER_NAME, TEST_REQUEST_PATH).build();
    HttpRequestPolicyPointcutParameters policyPointcutParameters = (HttpRequestPolicyPointcutParameters) factory
        .createPolicyPointcutParameters(FLOW_NAME, HTTP_REQUEST_COMPONENT_IDENTIFIER, parametersMap);
    assertThat(policyPointcutParameters.getComponentIdentifier(), is(HTTP_REQUEST_COMPONENT_IDENTIFIER));
    assertThat(policyPointcutParameters.getPath(), is(TEST_REQUEST_PATH));
    assertThat(policyPointcutParameters.getMethod(), is(TEST_METHOD));
    assertThat(policyPointcutParameters.getFlowName(), is(FLOW_NAME));
  }

}
