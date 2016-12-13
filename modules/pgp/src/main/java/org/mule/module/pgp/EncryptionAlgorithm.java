/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;

public enum EncryptionAlgorithm
{
    IDEA(SymmetricKeyAlgorithmTags.IDEA),
    TRIPLE_DES(SymmetricKeyAlgorithmTags.TRIPLE_DES),
    CAST5(SymmetricKeyAlgorithmTags.CAST5),
    BLOWFISH(SymmetricKeyAlgorithmTags.BLOWFISH),
    SAFER(SymmetricKeyAlgorithmTags.SAFER),
    DES(SymmetricKeyAlgorithmTags.DES),
    AES_128(SymmetricKeyAlgorithmTags.AES_128),
    AES_192(SymmetricKeyAlgorithmTags.AES_192),
    AES_256(SymmetricKeyAlgorithmTags.AES_256),
    TWOFISH(SymmetricKeyAlgorithmTags.TWOFISH),
    CAMELLIA_128(SymmetricKeyAlgorithmTags.CAMELLIA_128),
    CAMELLIA_192(SymmetricKeyAlgorithmTags.CAMELLIA_192),
    CAMELLIA_256(SymmetricKeyAlgorithmTags.CAMELLIA_256);

    private int numericId;

    EncryptionAlgorithm(int numericId)
    {
        this.numericId = numericId;
    }

    public int getNumericId()
    {
        return numericId;
    }
}
