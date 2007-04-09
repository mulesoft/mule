/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.umo.UMOEvent;

public class EventMemoryPersistenceStrategy extends MemoryPersistenceStrategy
{

    protected Object getId(Object obj)
    {
        return ((UMOEvent) obj).getId();
    }

}
