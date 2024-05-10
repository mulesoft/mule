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
import org.mule.runtime.api.tls.TlsContextTrustStoreConfiguration;

import javax.inject.Inject;
import javax.inject.Named;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TlsRevocationCheckTestCase extends MuleArtifactFunctionalTestCase {

  private static final String ALGORITHM = "PKIX";

  @Inject
  @Named("tlsContext")
  private TlsContextFactory tlsContextFactory;

  @Override
  protected String getConfigFile() {
    return "tls-namespace-revocation-check-config.xml";
  }

  @Test
  public void testTlsContextKeyStoreProperties() throws Exception {
    TlsContextTrustStoreConfiguration trustStoreConfig = tlsContextFactory.getTrustStoreConfiguration();
    assertThat(trustStoreConfig.getAlgorithm(), equalTo(ALGORITHM));
  }

}
