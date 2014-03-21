/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;

public class SignedMessage implements Message
{

    private LazyTransformedInputStream encryptedMessage;

    public SignedMessage(InputStream toBeDecrypted,
                         PGPPublicKey publicKey,
                         PGPSecretKey secretKey,
                         String password,
                         Provider provider) throws IOException
    {
        StreamTransformer transformer = new DecryptStreamTransformer(toBeDecrypted, publicKey, secretKey,
            password, provider);
        this.encryptedMessage = new LazyTransformedInputStream(new TransformContinuouslyPolicy(), transformer);
    }

    public boolean verify()
    {
        // TODO Signed messages is not implemented yet
        return false;
    }

    public Message getContents() throws IOException
    {
        String contents = IOUtils.toString(this.encryptedMessage);
        return new LiteralMessage(contents.getBytes());
    }

}
