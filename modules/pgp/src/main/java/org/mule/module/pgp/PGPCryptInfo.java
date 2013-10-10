/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.bouncycastle.openpgp.PGPPublicKey;

public class PGPCryptInfo
{
    private PGPPublicKey publicKey;
    private boolean signRequested;

    public PGPCryptInfo(PGPPublicKey publicKey, boolean signRequested)
    {
        super();

        this.setPublicKey(publicKey);
        this.setSignRequested(signRequested);
    }

    public PGPPublicKey getPublicKey()
    {
        return publicKey;
    }

    public void setPublicKey(PGPPublicKey publicKey)
    {
        this.publicKey = publicKey;
    }

    public boolean isSignRequested()
    {
        return signRequested;
    }

    public void setSignRequested(boolean signRequested)
    {
        this.signRequested = signRequested;
    }
}
