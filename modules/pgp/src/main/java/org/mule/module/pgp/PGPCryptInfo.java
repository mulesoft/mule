/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
