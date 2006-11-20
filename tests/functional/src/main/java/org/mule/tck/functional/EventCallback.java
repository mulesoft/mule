/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.umo.UMOEventContext;

/**
 * <code>EventCallback</code> TODO (document class)
 */

public interface EventCallback
{
    public void eventReceived(UMOEventContext context, Object Component) throws Exception;
}
