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

import org.mule.umo.UMOEvent;

/**
 * <code>UMOCredentialsAccessor</code> is a template for obtaining user credentials from
 * the current message and writing the user credentials to an outbound message
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOCredentialsAccessor
{
    public Object getCredentials(UMOEvent event);

    public void setCredentials(UMOEvent event, Object credentials);
}
