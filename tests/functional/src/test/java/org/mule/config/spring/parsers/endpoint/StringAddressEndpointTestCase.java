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

public class StringAddressEndpointTestCase extends AbstractEndpointTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/string-address-endpoint-test.xml";
    }

    public void testStringAddress()
    {
        doTest("string");
    }

    public void testOrphanAddress()
    {
        doTest("orphan");
    }

    public void testChildAddress()
    {
        doTest("child");
    }

}
