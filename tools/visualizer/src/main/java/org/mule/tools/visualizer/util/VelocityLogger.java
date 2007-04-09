/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.util;

import org.mule.tools.visualizer.config.GraphEnvironment;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;

public class VelocityLogger implements LogSystem
{

    private GraphEnvironment environment = null;

    public VelocityLogger(GraphEnvironment environment)
    {
        this.environment = environment;
    }

    public void init(RuntimeServices arg0) throws Exception
    {
        // nothing to do
    }

    public void logVelocityMessage(int arg0, String arg1)
    {
        if (environment != null)
        {
            environment.log(arg1);
        }
    }

    public GraphEnvironment getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(GraphEnvironment environment)
    {
        this.environment = environment;
    }

}
