/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl;

import org.mule.umo.UMOManagementContext;

/**
 * Objects who want to be aware of the UMOManagementContext should implement this interface. Once the context has
 * been initialised it will be passed to all objects implementing this interface.
 */
public interface ManagementContextAware
{
    void setManagementContext(UMOManagementContext context);
}
