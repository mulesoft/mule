/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.http.policy;

import static java.util.Collections.emptyMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.extension.http.api.policy.HttpRequestPolicyPointcutParametersFactory.PATH_PARAMETER_NAME;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;

import org.mule.extension.http.api.policy.HttpRequestPolicyPointcutParameters;
import org.mule.extension.http.api.policy.HttpRequestPolicyPointcutParametersFactory;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpRequestPolicyPointcutParametersFactoryTestCase extends AbstractMuleTestCase {

  private static final ComponentIdentifier HTTP_REQUEST_COMPONENT_IDENTIFIER =
      builder().withNamespace("http").withName("request").build();
  private static final String TEST_REQUEST_PATH = "test-request-path";
  private static final String TEST_METHOD = "PUT";

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
        .supportsOperationIdentifier(builder().withNamespace("http").withName("listener").build()),
               is(false));
  }

  @Test(expected = NullPointerException.class)
  public void failIfComponentLocationIsNull() {
    factory.createPolicyPointcutParameters(null, emptyMap());
  }

  @Test
  public void policyPointcutParameters() {
    ComponentLocation componentLocation = mock(ComponentLocation.class);
    Map<String, Object> parametersMap =
        ImmutableMap.<String, Object>builder().put(HttpRequestPolicyPointcutParametersFactory.METHOD_PARAMETER_NAME, TEST_METHOD)
            .put(PATH_PARAMETER_NAME, TEST_REQUEST_PATH).build();

    HttpRequestPolicyPointcutParameters policyPointcutParameters =
        (HttpRequestPolicyPointcutParameters) factory.createPolicyPointcutParameters(componentLocation, parametersMap);

    assertThat(policyPointcutParameters.getComponentLocation(), is(componentLocation));
    assertThat(policyPointcutParameters.getPath(), is(TEST_REQUEST_PATH));
    assertThat(policyPointcutParameters.getMethod(), is(TEST_METHOD));
  }

}
