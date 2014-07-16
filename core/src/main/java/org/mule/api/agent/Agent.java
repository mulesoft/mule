/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.agent;

import org.mule.api.NameableObject;
import org.mule.api.lifecycle.Lifecycle;

/**
 * <code>Agent</code> is a server plugin that can be initialised, started and
 * destroyed along with the MuleContext itself. Agents can initialise or bind to
 * external services such as a Jmx server.
 */
public interface Agent extends Lifecycle, NameableObject
{
    /**
     * A description of the agent that gets displayed in the splash
     * screen when the Mule Context starts up.
     * @return a 1 line description of the agent.
     */
    String getDescription();
}
