/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.security;

import org.mule.umo.UMOEncryptionStrategy;

/**
 * <code>CryptoFailureException</code> is a generic exception thrown by
 * an CryptoStrategy if encryption or decryption fails. The constuctors
 * of this exception accept a UMOEncryptionStrategy that will be included in
 * the exception message.  Implementors of UMOEncryptionStrategy should provide
 * a toString method that exposes *only* information that maybe useful for
 * debugging not passwords, secret keys, etc.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class CryptoFailureException extends UMOSecurityException
{
    /**
     * @param message the exception message
     */
    public CryptoFailureException(String message, UMOEncryptionStrategy strategy)
    {
        super(message + (strategy==null ? "" : " : Crypto strategy is: " + strategy.toString()));
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public CryptoFailureException(String message, Throwable cause, UMOEncryptionStrategy strategy)
    {
        super(message  + (strategy==null ? "" : " : Crypto strategy is: " + strategy.toString()), cause);
    }
}
