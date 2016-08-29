/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;

import java.security.GeneralSecurityException;

import org.junit.Test;

public class HttpRequestValidateCertificateTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-validate-certificate-config.xml";
  }

  @Override
  protected boolean enableHttps() {
    return true;
  }

  @Test
  public void rejectsMissingCertificate() throws Exception {
    MessagingException e = flowRunner("missingCertFlow").withPayload(TEST_MESSAGE).runExpectingException();
    assertThat(e, is(instanceOf(MessagingException.class)));
    assertThat(e.getCauseException(), is(instanceOf(GeneralSecurityException.class)));
  }

  @Test
  public void acceptsValidCertificate() throws Exception {
    MuleEvent result = flowRunner("validCertFlow").withPayload(TEST_MESSAGE).run();
    assertThat(getPayloadAsString(result.getMessage()), equalTo(DEFAULT_RESPONSE));
  }
}
