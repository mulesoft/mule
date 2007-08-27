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

import org.mule.umo.UMOEvent;

/**
 * <code>UMOCredentialsAccessor</code> is a template for obtaining user credentials
 * from the current message and writing the user credentials to an outbound message
 */
public interface UMOCredentialsAccessor
{
    Object getCredentials(UMOEvent event);

    void setCredentials(UMOEvent event, Object credentials);
}
