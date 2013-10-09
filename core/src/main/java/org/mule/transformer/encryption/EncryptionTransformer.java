/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.encryption;

import org.mule.api.security.CryptoFailureException;

import java.io.InputStream;

/**
 * <code>EncryptionTransformer</code> will transform an array of bytes or string
 * into an encrypted array of bytes
 */
public class EncryptionTransformer extends AbstractEncryptionTransformer
{
    @Override
    protected InputStream primTransform(InputStream input) throws CryptoFailureException
    {
        return getStrategy().encrypt(input, null);
    }
}
