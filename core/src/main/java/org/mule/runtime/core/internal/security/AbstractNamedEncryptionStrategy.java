/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategy;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractNamedEncryptionStrategy extends AbstractComponent implements EncryptionStrategy {

  private String name;

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public byte[] encrypt(byte[] data, Object info) throws CryptoFailureException {
    InputStream io = this.encrypt(new ByteArrayInputStream(data), info);
    try {
      return IOUtils.toByteArray(io);
    } catch (IOException e) {
      throw new CryptoFailureException(this, e);
    }
  }

  @Override
  public byte[] decrypt(byte[] data, Object info) throws CryptoFailureException {
    InputStream io = this.decrypt(new ByteArrayInputStream(data), info);
    try {
      return IOUtils.toByteArray(io);
    } catch (IOException e) {
      throw new CryptoFailureException(this, e);
    }
  }
}
