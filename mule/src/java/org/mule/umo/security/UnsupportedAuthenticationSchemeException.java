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
 * <code>UnsupportedAuthenticationSchemeException</code> is thrown when a authentication scheme
 * is being used on the message that the Security filter does not understand
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class UnsupportedAuthenticationSchemeException extends UMOSecurityException
{
    /**
     * @param message the exception message
     */
    public UnsupportedAuthenticationSchemeException(String message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public UnsupportedAuthenticationSchemeException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
