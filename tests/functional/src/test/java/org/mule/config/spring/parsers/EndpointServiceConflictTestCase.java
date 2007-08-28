/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

public class EndpointServiceConflictTestCase extends AbstractBadConfigTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint-service-conflict-test.xml";
    }

    public void testBeanError() throws Exception
    {
        assertErrorContains("A component named LenderService already exists");
    }

}
