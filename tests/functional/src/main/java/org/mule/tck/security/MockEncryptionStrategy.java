/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.security;

import org.mule.api.EncryptionStrategy;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CryptoFailureException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Empty mock for tests
 */
public class MockEncryptionStrategy extends Named implements EncryptionStrategy
{

    public byte[] encrypt(byte[] data, Object info) throws CryptoFailureException
    {
        return new byte[0];
    }

    public byte[] decrypt(byte[] data, Object info) throws CryptoFailureException
    {
        return new byte[0];
    }

    public void initialise() throws InitialisationException
    {
        // nothing to do
    }

    public InputStream decrypt(InputStream data, Object info) throws CryptoFailureException
    {
        return new ByteArrayInputStream(new byte[0]);
    }

    public InputStream encrypt(InputStream data, Object info) throws CryptoFailureException
    {
        return new ByteArrayInputStream(new byte[0]);
    }

}
