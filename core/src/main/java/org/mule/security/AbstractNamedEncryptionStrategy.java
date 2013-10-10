/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import org.mule.api.EncryptionStrategy;
import org.mule.api.security.CryptoFailureException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public abstract class AbstractNamedEncryptionStrategy implements EncryptionStrategy
{

    private String name;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public byte[] encrypt(byte[] data, Object info) throws CryptoFailureException {
        InputStream io = this.encrypt(new ByteArrayInputStream(data), info);
        try
        {
            return IOUtils.toByteArray(io);
        }
        catch (IOException e)
        {
            throw new CryptoFailureException(this, e);
        }
    }

    public byte[] decrypt(byte[] data, Object info) throws CryptoFailureException {
        InputStream io = this.decrypt(new ByteArrayInputStream(data), info);
        try
        {
            return IOUtils.toByteArray(io);
        }
        catch (IOException e)
        {
            throw new CryptoFailureException(this, e);
        }
    }
}
