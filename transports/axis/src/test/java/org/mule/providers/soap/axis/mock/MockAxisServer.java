/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.mock;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.server.AxisServer;

public class MockAxisServer extends AxisServer
{
    public MockAxisServer(EngineConfiguration engineConfig)
    {
        super(engineConfig);
    }
}
