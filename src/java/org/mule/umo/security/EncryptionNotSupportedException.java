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

/**
 * <code>EncryptionNotSupportedException</code> is thrown if an algorithm is
 * set in the MULE_USER header but it doesn't match the algorithm set on
 * the security filter
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class EncryptionNotSupportedException extends UMOSecurityException
{
    /**
     * @param message the exception message
     */
    public EncryptionNotSupportedException(String message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public EncryptionNotSupportedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
