/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.http.policy;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.policy.HttpListenerPolicyPointcutParameters;
import org.mule.extension.http.api.policy.HttpListenerPolicyPointcutParametersFactory;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpListenerPolicyPointcutParametersFactoryTestCase extends AbstractMuleTestCase {

  private static final ComponentIdentifier HTTP_LISTENER_COMPONENT_IDENTIFIER =
      builder().withNamespace("httpn").withName("listener").build();
  private static final String TEST_REQUEST_PATH = "test-request-path";
  private static final String TEST_METHOD = "PUT";

  private final HttpListenerPolicyPointcutParametersFactory factory = new HttpListenerPolicyPointcutParametersFactory();
  private final ComponentLocation componentLocation = mock(ComponentLocation.class);
  private final HttpRequestAttributes httpAttributes = mock(HttpRequestAttributes.class);
  private final TypedValue attributes = new TypedValue(mock(Attributes.class), OBJECT);

  @Test
  public void supportsHttpListener() {
    assertThat(factory
        .supportsSourceIdentifier(HTTP_LISTENER_COMPONENT_IDENTIFIER),
               is(true));
  }

  @Test
  public void doesNotSupportHttpRequester() {
    assertThat(factory
        .supportsSourceIdentifier(ComponentIdentifier.builder().withNamespace("httpn").withName("request").build()),
               is(false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void failIfAttributesIsNotHttpRequestAttributes() {
    factory.createPolicyPointcutParameters(componentLocation, attributes);
  }

  @Test(expected = NullPointerException.class)
  public void failIfComponentLocationIsNull() {
    factory.createPolicyPointcutParameters(null, attributes);
  }

  @Test
  public void policyPointcutParameters() {
    when(httpAttributes.getRequestPath()).thenReturn(TEST_REQUEST_PATH);
    when(httpAttributes.getMethod()).thenReturn(TEST_METHOD);

    HttpListenerPolicyPointcutParameters policyPointcutParameters =
        (HttpListenerPolicyPointcutParameters) factory.createPolicyPointcutParameters(componentLocation,
                                                                                      new TypedValue<>(httpAttributes, OBJECT));

    assertThat(policyPointcutParameters.getComponentLocation(), is(componentLocation));
    assertThat(policyPointcutParameters.getPath(), is(TEST_REQUEST_PATH));
    assertThat(policyPointcutParameters.getMethod(), is(TEST_METHOD));
  }

}
