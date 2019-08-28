/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

/**
 * Object that communicates information about the decryption procedure.
 */
public class PGPDecryptInfo
{

    private final boolean verifySignatureIfFound;


    public PGPDecryptInfo(boolean verifySignatureIfFound)
    {
        this.verifySignatureIfFound = verifySignatureIfFound;
    }

    public boolean isVerifySignatureIfFound()
    {
        return verifySignatureIfFound;
    }
}
