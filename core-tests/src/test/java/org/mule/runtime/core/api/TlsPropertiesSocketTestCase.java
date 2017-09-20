/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.privileged.security.tls.TlsConfiguration;
import org.mule.runtime.core.internal.secutiry.tls.TlsPropertiesSocketFactory;
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


