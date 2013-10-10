/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;

public class SignedMessage implements Message
{

    private LazyTransformedInputStream encryptedMessage;

    public SignedMessage(InputStream toBeDecrypted,
                         PGPPublicKey publicKey,
                         PGPSecretKey secretKey,
                         String password) throws IOException
    {
        StreamTransformer transformer = new DecryptStreamTransformer(toBeDecrypted, publicKey, secretKey,
            password);
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
