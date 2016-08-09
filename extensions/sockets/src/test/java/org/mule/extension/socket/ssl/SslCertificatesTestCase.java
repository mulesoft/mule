/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.socket.ssl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import org.mule.extension.socket.SocketExtensionTestCase;
import org.mule.extension.socket.api.SocketAttributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.security.cert.Certificate;

import org.junit.Test;

/**
 * Test {@link Certificate} are present in {@link SocketAttributes} while using SSL.
 *
 * @since 4.0
 */
public class SslCertificatesTestCase extends SocketExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "ssl-certificate-config.xml";
  }

  @Test
  public void testOnce() throws Exception {
    doTests(1);
  }

  @Test
  public void testMany() throws Exception {
    doTests(REPETITIONS);
  }

  protected void doTests(int numberOfMessages) throws Exception {

    for (int i = 0; i < numberOfMessages; ++i) {
      org.mule.runtime.core.api.MuleMessage muleMessage =
          flowRunner("ssl-send-and-receive").withPayload(TEST_STRING).run().getMessage();

      String payload = IOUtils.toString((InputStream) muleMessage.getPayload());
      assertThat(payload, is(notNullValue()));
      SocketAttributes attributes = (SocketAttributes) muleMessage.getAttributes();
      assertThat(attributes.getLocalCertificates(), is(notNullValue()));
    }

  }

  protected void assertCertificate(MuleMessage message) throws Exception {
    String payload = IOUtils.toString((InputStream) message.getPayload());
    assertThat(payload, is(notNullValue()));
    SocketAttributes attributes = (SocketAttributes) message.getAttributes();
    assertThat(attributes.getLocalCertificates(), is(notNullValue()));
  }
}
