/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.echo;

/**
 * Tests the echo example using CXF.
 */
public class CxfEchoFlowTestCase extends CxfEchoTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "mule-config.xml";
    }

}
