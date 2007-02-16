/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.umo.UMOManagementContext;

/**
 * Objects interested in getting a reference to the Management context for the current Mule instance
 * should implement this interface. Once all properties have been set on an object, this will be called
 */
public interface ManagementContextAware
{
    public void setManagementContext(UMOManagementContext context);
}
