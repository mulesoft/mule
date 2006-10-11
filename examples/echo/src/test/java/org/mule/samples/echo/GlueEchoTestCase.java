/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.echo;

/**
 * Tests the echo example using GLUE
 */
public class GlueEchoTestCase extends AxisEchoTestCase
{

    protected String getConfigResources()
    {
        return "echo-glue-config.xml";
    }

    protected String getProtocol()
    {
        return "glue";
    }

    public void testPostEcho() throws Exception
    {
        // Glue doesn't use the Mule Http transport so cannot provide automatic
        // transformations
    }

    public void testGetEcho() throws Exception
    {
        // Glue doesn't use the Mule Http transport so cannot provide automatic
        // transformations
    }

}
