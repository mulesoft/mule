/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import org.mule.tck.AbstractMuleTestCase;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

public abstract class AbstractEncryptionStrategyTestCase extends AbstractMuleTestCase
{
    
    protected static boolean isCryptographyExtensionInstalled()
    {
        // see MULE-3671
        try
        {
            int maxKeyLength = Cipher.getMaxAllowedKeyLength("DES/CBC/PKCS5Padding");
            // if JCE is not installed, maxKeyLength will be 64
            return maxKeyLength == Integer.MAX_VALUE;
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new AssertionError(e);
        }
    }
    
    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return (isCryptographyExtensionInstalled() == false);
    }

}


