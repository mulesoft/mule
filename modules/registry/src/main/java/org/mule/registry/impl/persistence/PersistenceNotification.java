/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl.persistence;

import org.mule.persistence.Persistable;
import org.mule.umo.manager.UMOServerNotification;

/**
 * TODO
 */
public class PersistenceNotification extends UMOServerNotification 
{
    public static final int PERSISTABLE_NOTREADY = 0;
    public static final int PERSISTABLE_READY = 1;

    public PersistenceNotification(Persistable object, int action)
    {
        super(object, action);
    }

    protected boolean isReady()
    {
        return (action == PERSISTABLE_READY ? true : false);
    }
}
