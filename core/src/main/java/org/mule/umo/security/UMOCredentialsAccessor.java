/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.security;

import org.mule.umo.UMOEvent;

/**
 * <code>UMOCredentialsAccessor</code> is a template for obtaining user
 * credentials from the current message and writing the user credentials to an
 * outbound message
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOCredentialsAccessor
{
    Object getCredentials(UMOEvent event);

    void setCredentials(UMOEvent event, Object credentials);
}
