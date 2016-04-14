/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.lifecycle.Initialisable;
import org.mule.api.security.CryptoFailureException;

import java.io.InputStream;

/**
 * <code>EncryptionStrategy</code> can be used to provide different types of
 * Encryption strategy objects. These can be configured with different information
 * relivant with the encryption method being used. for example for Password Based
 * Encryption (PBE) a password, salt, iteration count and algorithm may be set on the
 * strategy.
 */
public interface EncryptionStrategy extends Initialisable, NamedObject
{
    InputStream encrypt(InputStream data, Object info) throws CryptoFailureException;

    InputStream decrypt(InputStream data, Object info) throws CryptoFailureException;
    
    byte[] encrypt(byte[] data, Object info) throws CryptoFailureException;

    byte[] decrypt(byte[] data, Object info) throws CryptoFailureException;
}
