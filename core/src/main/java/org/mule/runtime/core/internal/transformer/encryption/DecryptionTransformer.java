/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
