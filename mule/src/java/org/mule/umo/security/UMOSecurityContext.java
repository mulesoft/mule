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
 * <code>UMOSecurityContext</code> holds security information and is associated with
 * the UMOSession.
 *
 * @see org.mule.umo.UMOSession
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOSecurityContext
{
    public void setAuthentication(UMOAuthentication authentication);

    public UMOAuthentication getAuthentication();
}
