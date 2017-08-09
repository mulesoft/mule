/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.bouncycastle.bcpg.HashAlgorithmTags;

public enum PGPSignatureAlgorithm
{
    MD5(HashAlgorithmTags.MD5),
    SHA1(HashAlgorithmTags.SHA1),
    RIPEMD160(HashAlgorithmTags.RIPEMD160),
    DOUBLE_SHA(HashAlgorithmTags.DOUBLE_SHA),
    TIGER_192(HashAlgorithmTags.TIGER_192),
    HAVAL_5_160(HashAlgorithmTags.HAVAL_5_160),
    SHA256(HashAlgorithmTags.SHA256),
    SHA384(HashAlgorithmTags.SHA384),
    SHA512(HashAlgorithmTags.SHA512),
    SHA224(HashAlgorithmTags.SHA224);
    
    private int numericId;

    PGPSignatureAlgorithm(int numericId)
    {
        this.numericId = numericId;
    }

    public int getNumericId()
    {
        return numericId;
    }
}
