/*
 * $Header$
 * $Revision$
 * $Date$
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
 * <code>UMOSecurityContextFactory</code> is responsible for creating a
 * UMOSecurityContext instance. The factory itself is associated with an
 * Authentication class type on the UMOSecurityManager
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOSecurityContextFactory
{
    UMOSecurityContext create(UMOAuthentication authentication);
}
