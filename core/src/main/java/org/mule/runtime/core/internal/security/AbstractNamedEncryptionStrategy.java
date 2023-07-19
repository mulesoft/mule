/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
