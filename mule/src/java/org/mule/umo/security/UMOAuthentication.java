/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.security;

/**
 * <code>UMOAuthentication</code> represents an authentication request and
 * contains authenitcation information if the request was successful
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOAuthentication
{
    void setAuthenticated(boolean b);

    boolean isAuthenticated();

    Object getCredentials();

    Object getDetails();

    Object getPrincipal();
}
