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

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.umo.security.UnknownAuthenticationTypeException;

/**
 * Empty mock for test
 */
public class MockSecurityProvider extends Named implements UMOSecurityProvider
{

    public UMOAuthentication authenticate(UMOAuthentication authentication) throws org.mule.umo.security.SecurityException
    {
        return null;
    }

    public boolean supports(Class aClass)
    {
        return false;
    }

    public UMOSecurityContext createSecurityContext(UMOAuthentication auth) throws UnknownAuthenticationTypeException
    {
        return null;
    }

    public void initialise() throws InitialisationException
    {
        // mock
    }

}
