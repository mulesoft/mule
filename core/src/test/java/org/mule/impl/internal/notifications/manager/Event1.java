/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications.manager;

import org.mule.umo.manager.UMOServerNotification;

public class Event1 extends UMOServerNotification
{

    public Event1()
    {
        super("", 0);
    }

    public Event1(String id)
    {
        super("", 0, id);
    }

}
