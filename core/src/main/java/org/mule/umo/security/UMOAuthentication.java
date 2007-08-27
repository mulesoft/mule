/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security;

import java.util.Map;

/**
 * <code>UMOAuthentication</code> represents an authentication request and contains
 * authentication information if the request was successful
 */
public interface UMOAuthentication
{
    void setAuthenticated(boolean b);

    boolean isAuthenticated();

    Object getCredentials();

    Object getPrincipal();

    Map getProperties();

    void setProperties(Map properties);
}
