/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.encryption;

import org.mule.umo.security.CryptoFailureException;

/**
 * <code>EncryptionTransformer</code> will transform an array of bytes or string
 * into an encrypted array of bytes
 */
public class EncryptionTransformer extends AbstractEncryptionTransformer
{

    protected byte[] getTransformedBytes(byte[] buffer) throws CryptoFailureException
    {
        return getStrategy().encrypt(buffer, null);
    }

}
