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
 * <code>SecurityProviderNotFoundException</code> is thrown by the UMOSecurityManager
 * when an authentication request is made but no suitable security provider can be
 * found to process the authentication
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SecurityProviderNotFoundException extends UMOSecurityException
{
    /**
     * @param message the exception message
     */
    public SecurityProviderNotFoundException(String message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public SecurityProviderNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
