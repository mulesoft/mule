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

import org.mule.umo.lifecycle.Initialisable;

/**
 * <code>UMOSecurityProvider</code> is a target security provider thsat actually does
 * the work of authenticating credentials and populating the UMOAuthentication object.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOSecurityProvider extends Initialisable
{
    public void setName(String name);

    public String getName();

    public UMOAuthentication authenticate(UMOAuthentication authentication) throws UMOSecurityException;

    public boolean supports(Class aClass);

    public UMOSecurityContext createSecurityContext(UMOAuthentication auth) throws UnknownAuthenticationTypeException;
}
