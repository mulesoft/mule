/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.echo;

/**
 * Tests the echo example using Xfire
 */
public class XFireEchoTestCase extends AxisEchoTestCase
{

    protected String getConfigResources()
    {
        return "echo-xfire-config.xml";
    }

    protected String getExpectedGetResponseResource()
    {
        return "echo-xfire-response.xml";
    }

    protected String getExpectedPostResponseResource()
    {
        return "echo-xfire-response.xml";
    }

    protected String getProtocol()
    {
        return "xfire";
    }

}
