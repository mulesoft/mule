/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.security;

import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.CryptoFailureException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Empty mock for tests
 */
public class MockEncryptionStrategy extends Named implements EncryptionStrategy {

  public byte[] encrypt(byte[] data, Object info) throws CryptoFailureException {
    return new byte[0];
  }

  public byte[] decrypt(byte[] data, Object info) throws CryptoFailureException {
    return new byte[0];
  }

  public void initialise() throws InitialisationException {
    // nothing to do
  }

  public InputStream decrypt(InputStream data, Object info) throws CryptoFailureException {
    return new ByteArrayInputStream(new byte[0]);
  }

  public InputStream encrypt(InputStream data, Object info) throws CryptoFailureException {
    return new ByteArrayInputStream(new byte[0]);
  }

}
