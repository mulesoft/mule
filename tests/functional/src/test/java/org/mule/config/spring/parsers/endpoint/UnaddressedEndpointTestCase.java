/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.endpoint;

public class UnaddressedEndpointTestCase extends AbstractEndpointTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/unaddressed-endpoint-test.xml";
    }

    public void testUnaddressed()
    {
        doTest("unaddressed");
    }

    public void testUnaddressedOrphan()
    {
        doTest("unaddressed-orphan");
    }

}