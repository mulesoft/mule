/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

/**
 * Atomic encrypt and sign procedure information. Used for communication between mule-module-security and mule's
 * PGP-module.
 */
public class PGPEncryptAndSignInfo
{

    private final String signerPrincipal;

    public PGPEncryptAndSignInfo(String signerPrincipal)
    {
        this.signerPrincipal = signerPrincipal;
    }

    public String getSignerPrincipal()
    {
        return signerPrincipal;
    }
}
