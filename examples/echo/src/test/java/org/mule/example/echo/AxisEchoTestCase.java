/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.echo;


/**
 * Tests the Echo example using Axis.
 */
public class AxisEchoTestCase extends AbstractEchoTestCase
{
    protected String getExpectedGetResponseResource()
    {
        return "echo-axis-get-response.xml";
    }

    protected String getExpectedPostResponseResource()
    {
        return "echo-axis-post-response.xml";
    }

    protected String getConfigResources()
    {
        return "echo-axis-config.xml";
    }

    protected String getProtocol()
    {
        return "axis";
    }
}
