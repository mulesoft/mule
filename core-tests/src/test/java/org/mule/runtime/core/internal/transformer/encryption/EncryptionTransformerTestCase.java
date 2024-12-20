/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.encryption;

import static org.junit.Assert.fail;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.security.PasswordBasedEncryptionStrategy;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.junit.Before;

public class EncryptionTransformerTestCase extends AbstractTransformerTestCase {

  private static final String TEST_DATA =
      "the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog";

  private PasswordBasedEncryptionStrategy strat;

  @Before
  public void setUp() throws Exception {
    strat = new PasswordBasedEncryptionStrategy();
    strat.setPassword("mule");
  }

  @Override
  public Object getResultData() {
    try {
      return new ByteArrayInputStream(strat.encrypt(TEST_DATA.getBytes(), null));
    } catch (CryptoFailureException e) {
      fail(e.getMessage());
      return null;
    }
  }

  @Override
  public Object getTestData() {
    return new ByteArrayInputStream(TEST_DATA.getBytes());
  }

  @Override
  public Transformer getTransformer() throws InitialisationException {
    EncryptionTransformer transformer = new EncryptionTransformer();
    transformer.setStrategy(strat);
    transformer.initialise();
    return configureTransformer(transformer);
  }

  @Override
  public Transformer getRoundTripTransformer() throws InitialisationException {
    DecryptionTransformer transformer = new DecryptionTransformer();
    transformer.setStrategy(strat);
    transformer.initialise();
    return configureTransformer(transformer);
  }

  @Override
  public boolean compareResults(Object src, Object result) {
    if (src == null && result == null) {
      return true;
    }

    if (src == null || result == null) {
      return false;
    }

    if (src instanceof byte[] && result instanceof byte[]) {
      return Arrays.equals((byte[]) src, (byte[]) result);
    } else {
      return super.compareResults(src, result);
    }
  }
}
