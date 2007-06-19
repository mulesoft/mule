/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.security;

import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.CryptoFailureException;

/**
 * Empty mock for tests
 */
public class MockEncryptionStrategy extends Named implements UMOEncryptionStrategy
{

    public byte[] encrypt(byte[] data, Object info) throws CryptoFailureException
    {
        return new byte[0];
    }

    public byte[] decrypt(byte[] data, Object info) throws CryptoFailureException
    {
        return new byte[0];
    }

    public void initialise() throws InitialisationException
    {
        // mock
    }

}
