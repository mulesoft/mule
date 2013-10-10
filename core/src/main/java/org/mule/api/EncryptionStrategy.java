/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
