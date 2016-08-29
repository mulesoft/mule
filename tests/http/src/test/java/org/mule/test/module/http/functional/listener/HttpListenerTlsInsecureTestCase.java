/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

/**
 * Sets up two HTTPS servers with regular trust-stores, except one is insecure. Verifies that a request using a certificate not
 * present in the trust-store only works for the insecure server.
 */
public class HttpListenerTlsInsecureTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");
  @Rule
  public DynamicPort port2 = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "http-listener-insecure-config.xml";
  }

  @Test
  public void acceptsInvalidCertificateIfInsecure() throws Exception {
    final MuleEvent res = flowRunner("testRequestToInsecure").withPayload(TEST_PAYLOAD).run();
    assertThat(res.getMessageAsString(muleContext), is(TEST_PAYLOAD));
  }

  @Test
  public void rejectsInvalidCertificateIfSecure() throws Exception {
    MessagingException expecteException = flowRunner("testRequestToSecure").withPayload("data").runExpectingException();
    assertThat(expecteException.getCause(), instanceOf(IOException.class));
    assertThat(expecteException.getCause(), hasMessage(containsString("Remotely close")));
  }
}
