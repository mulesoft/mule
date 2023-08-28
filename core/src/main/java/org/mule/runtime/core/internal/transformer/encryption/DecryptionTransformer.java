/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.encryption;

import org.mule.runtime.core.api.security.CryptoFailureException;

import java.io.InputStream;

/**
 * <code>EncryptionTransformer</code> will transform an encrypted array of bytes or string into an decrypted array of bytes
 */
public class DecryptionTransformer extends AbstractEncryptionTransformer {

  @Override
  protected InputStream primTransform(InputStream input) throws CryptoFailureException {
    return getStrategy().decrypt(input, null);
  }
}
