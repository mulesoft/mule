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
 * <code>UnknownAuthenticationTypeException</code> is thrown if a security
 * context request is make with an unrecognised UMOAuthentication type.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class UnknownAuthenticationTypeException extends UMOSecurityException
{
    /**
     * @param auth the unrecognised UMOAuthentication object
     */
    public UnknownAuthenticationTypeException(UMOAuthentication auth)
    {
        super("The authentication type " + auth.getClass().getName() + " is not recognied by the security manager");
    }
}
