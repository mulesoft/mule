/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.tls;

import static org.junit.Assert.assertTrue;

import org.mule.runtime.module.tls.internal.TlsConfiguration;
import org.mule.runtime.module.tls.internal.socket.TlsPropertiesSocketFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class TlsPropertiesSocketTestCase extends AbstractMuleTestCase {

  @Test
  public void testSimpleSocket() throws Exception {
    TlsConfiguration configuration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
    configuration.setKeyPassword("mulepassword");
    configuration.setKeyStorePassword("mulepassword");
    configuration.setKeyStore("clientKeystore");
    configuration.initialise(false, TlsConfiguration.JSSE_NAMESPACE);

    TlsPropertiesSocketFactory socketFactory = new TlsPropertiesSocketFactory(true, TlsConfiguration.JSSE_NAMESPACE);
    assertTrue("socket is useless", socketFactory.getSupportedCipherSuites().length > 0);
  }

}


