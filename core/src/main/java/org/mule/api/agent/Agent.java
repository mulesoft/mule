/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
