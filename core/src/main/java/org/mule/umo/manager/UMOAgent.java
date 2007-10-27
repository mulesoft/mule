/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.manager;

import org.mule.umo.NamedObject;
import org.mule.umo.lifecycle.Lifecycle;

/**
 * <code>UMOAgent</code> is a server plugin that can be initialised, started and
 * destroyed along with the UMOManager itself. Agents can initialise or bind to
 * external services such as a Jmx server.
 */
public interface UMOAgent extends Lifecycle, NamedObject
{
    /**
     * Should be a 1 line description of the agent
     * 
     * @return
     */
    String getDescription();

    void registered();

    void unregistered();
}
