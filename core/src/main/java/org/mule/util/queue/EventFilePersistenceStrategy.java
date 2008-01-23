/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.api.MuleEvent;

public class EventFilePersistenceStrategy extends FilePersistenceStrategy
{

    public EventFilePersistenceStrategy()
    {
        super();
    }

    protected String getId(Object obj)
    {
        MuleEvent event = (MuleEvent) obj;
        return event.getId();
    }

}
