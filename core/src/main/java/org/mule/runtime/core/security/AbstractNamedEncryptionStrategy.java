/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
