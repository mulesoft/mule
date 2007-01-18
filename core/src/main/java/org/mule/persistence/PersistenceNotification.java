/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence;

import org.mule.umo.manager.UMOServerNotification;

/**
 * @author 
 * @version $Revision: $
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

    protected String getActionName(int action)
    {
        return (action == PERSISTABLE_READY ? "ready" : "not ready");
    }
}
