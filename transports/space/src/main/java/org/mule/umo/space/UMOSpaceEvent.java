/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.space;

import java.util.EventObject;

/**
 * A space event occurs when an item is added to the sapce and there is a one or more
 * listeners waiting for it.
 */
public class UMOSpaceEvent extends EventObject
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4575516735778744922L;

    private final UMOSpace space;

    public UMOSpaceEvent(Object item, UMOSpace space)
    {
        super(item);
        this.space = space;
    }

    public UMOSpace getSpace()
    {
        return space;
    }
}
