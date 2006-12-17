/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.security.CryptoFailureException;

/**
 * <code>UMOEncryptionStrategy</code> can be used to provide different types of
 * Encryption strategy objects. These can be configured with different information
 * relivant with the encryption method being used. for example for Password Based
 * Encryption (PBE) a password, salt, iteration count and algorithm may be set on the
 * strategy.
 */
public interface UMOEncryptionStrategy extends Initialisable
{
    byte[] encrypt(byte[] data, Object info) throws CryptoFailureException;

    byte[] decrypt(byte[] data, Object info) throws CryptoFailureException;
}
