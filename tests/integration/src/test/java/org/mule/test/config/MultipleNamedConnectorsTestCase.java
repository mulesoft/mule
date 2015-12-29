/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class MultipleNamedConnectorsTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "multiple-named-connectors-test-flow.xml";
    }

    @Test
    public void testMultipleNamedConnectors() throws Exception
    {
        // no-op, the initialization must not fail.   
    }
}
