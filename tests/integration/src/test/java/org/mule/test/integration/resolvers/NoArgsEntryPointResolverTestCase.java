/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.resolvers;

public class NoArgsEntryPointResolverTestCase extends AbstractEntryPointResolverTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/resolvers/no-args-entry-point-resolver-test.xml";
    }

    public void testIgnored() throws Exception
    {
        doTest("not-ignored", new Object(), "notIgnored");
    }

    public void testSelected() throws Exception
    {
        doTest("selected", new Object(), "selected");
    }

}
