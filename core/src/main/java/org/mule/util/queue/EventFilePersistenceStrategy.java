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

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class EventFilePersistenceStrategy extends FilePersistenceStrategy
{

    public EventFilePersistenceStrategy()
    {
        super();
    }

    protected String getId(Object obj)
    {
        UMOEvent event = (UMOEvent)obj;
        return event.getId();
    }

}
