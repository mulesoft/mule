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
 * The scope of the Event Callback is to be able to get a component we are using
 * and to control it. This is especially useful when we have beans configured in
 * Spring that we need to exercise some form of control on, example setting
 * properties at runtime instead of in the configuration.
 */

public interface EventCallback
{
    public void eventReceived(UMOEventContext context, Object component) throws Exception;
}
