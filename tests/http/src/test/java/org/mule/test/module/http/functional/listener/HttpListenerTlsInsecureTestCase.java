/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import org.mule.functional.junit4.rules.ExpectedError;
import org.mule.runtime.core.api.Event;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

/**
 * Sets up two HTTPS servers with regular trust-stores, except one is insecure. Verifies that a request using a certificate not
 * present in the trust-store only works for the insecure server.
 */
@Features(HTTP_EXTENSION)
public class HttpListenerTlsInsecureTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Rule
  public DynamicPort port2 = new DynamicPort("port2");

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "http-listener-insecure-config.xml";
  }

  @Test
  public void acceptsInvalidCertificateIfInsecure() throws Exception {
    final Event res = flowRunner("testRequestToInsecure")
        .withPayload(TEST_PAYLOAD)
        .withVariable("port", port1.getNumber())
        .run();
    assertThat(res.getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
  }

  @Test
  public void rejectsInvalidCertificateIfSecure() throws Exception {
    expectedError.expectCause(instanceOf(IOException.class));
    expectedError.expectCause(hasMessage(containsString("Remotely close")));
    flowRunner("testRequestToSecure")
        .withPayload("data")
        .withVariable("port", port2.getNumber())
        .run();
  }
}
