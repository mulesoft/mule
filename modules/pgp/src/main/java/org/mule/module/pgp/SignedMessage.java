/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;

public class SignedMessage implements Message
{

    private InputStream encryptedMessage;

    public SignedMessage(InputStream toBeDecrypted,
                         PGPSecretKey secretKey,
                         String password, PGPSecretKeyRingCollection secretKeys) throws Exception
    {
        this.encryptedMessage = new DecryptStreamTransformer(secretKey, secretKeys,
                                                             null, password).process(toBeDecrypted);
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
