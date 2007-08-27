/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.security;

import org.mule.config.MuleProperties;
import org.mule.umo.UMOEvent;
import org.mule.umo.security.UMOCredentialsAccessor;

/**
 * <code>MuleHeaderCredentialsAccessor</code> obtains and sets the user credentials
 * as Mule property headers.
 */
public class MuleHeaderCredentialsAccessor implements UMOCredentialsAccessor
{
    public Object getCredentials(UMOEvent event)
    {
        return event.getMessage().getProperty(MuleProperties.MULE_USER_PROPERTY);
    }

    public void setCredentials(UMOEvent event, Object credentials)
    {
        event.getMessage().setProperty(MuleProperties.MULE_USER_PROPERTY, credentials);
    }
}
