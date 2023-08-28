/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api;

import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.privileged.security.tls.TlsConfiguration;
import org.mule.runtime.core.internal.security.tls.TlsPropertiesSocketFactory;
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


