/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.tls;

import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextKeyStoreConfiguration;

import javax.inject.Inject;
import javax.inject.Named;

import static org.junit.Assert.fail;

public class TlsRevocationCheckTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  @Named("tlsContext")
  private TlsContextFactory tlsContextFactory;

  @Override
  protected String getConfigFile() {
    return "tls-namespace-revocation-check-config.xml";
  }

  @Test
  public void testTlsContextWithStandardRevocation() {
    try {
      TlsContextKeyStoreConfiguration keyStoreConfig = tlsContextFactory.getKeyStoreConfiguration();
    } catch (Exception ex) {
      fail("Expected to run successfully, but got " + ex);
    }
  }

}
