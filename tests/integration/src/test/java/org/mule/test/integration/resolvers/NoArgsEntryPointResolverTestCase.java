/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import org.junit.Test;

public class NoArgsEntryPointResolverTestCase extends AbstractEntryPointResolverTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/resolvers/no-args-entry-point-resolver-test-flow.xml";
    }

    @Test
    public void testIgnored() throws Exception
    {
        doTest("NotIgnored", new Object(), "notIgnored");
    }

    @Test
    public void testSelected() throws Exception
    {
        doTest("Selected", new Object(), "selected");
    }
}
