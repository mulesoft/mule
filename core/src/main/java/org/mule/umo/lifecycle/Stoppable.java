/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

import org.mule.umo.UMOException;

/**
 * <code>Stoppable</code> TODO (document class)
 */
public interface Stoppable
{
    public static final String PHASE_NAME = "stop";
    
    void stop() throws UMOException;
}
