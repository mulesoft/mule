/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import org.mule.runtime.core.api.security.SecretKeyFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class SecretKeyEncryptionStrategyTestCase extends AbstractMuleTestCase {

  private static final String TRIPLE_DES_KEY = RandomStringUtils.randomAlphabetic(24);

  @Test
  public void testRoundTripEncryptionBlowfish() throws Exception {
    SecretKeyEncryptionStrategy ske = new SecretKeyEncryptionStrategy();
    ske.setAlgorithm("Blowfish");
    ske.setKey("shhhhh");
    ske.initialise();

    byte[] b = ske.encrypt("hello".getBytes(), null);

    assertNotSame(new String(b), "hello");
    String s = new String(ske.decrypt(b, null), "UTF-8");
    assertEquals("hello", s);
  }

  @Test
  public void testRoundTripEncryptionBlowfishWithKeyFactory() throws Exception {
    SecretKeyEncryptionStrategy ske = new SecretKeyEncryptionStrategy();
    ske.setAlgorithm("Blowfish");
    ske.setKeyFactory(new SecretKeyFactory() {

      @Override
      public byte[] getKey() {
        return "shhhh".getBytes();
      }
    });
    ske.initialise();

    byte[] b = ske.encrypt("hello".getBytes(), null);

    assertNotSame(new String(b), "hello");
    String s = new String(ske.decrypt(b, null), "UTF-8");
    assertEquals("hello", s);
  }

  @Test
  public void testRoundTripEncryptionTripleDES() throws Exception {
    SecretKeyEncryptionStrategy ske = new SecretKeyEncryptionStrategy();
    ske.setAlgorithm("TripleDES");
    ske.setKey(TRIPLE_DES_KEY);

    ske.initialise();

    byte[] b = ske.encrypt("hello".getBytes(), null);

    assertNotSame(new String(b), "hello");
    String s = new String(ske.decrypt(b, null), "UTF-8");
    assertEquals("hello", s);
  }

}
