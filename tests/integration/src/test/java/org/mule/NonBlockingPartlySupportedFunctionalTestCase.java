/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class NonBlockingPartlySupportedFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "non-blocking-partly-supported-test-config.xml";
    }

    @Test
    public void foreach() throws Exception
    {
        testFlowNonBlocking("foreach");
    }

    @Test
    public void wiretap() throws Exception
    {
        testFlowNonBlocking("wiretap");
    }

}

