/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.resolvers;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class NoArgsEntryPointResolverTestCase extends AbstractEntryPointResolverTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/resolvers/no-args-entry-point-resolver-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/resolvers/no-args-entry-point-resolver-test-flow.xml"}
        });
    }      
    
    public NoArgsEntryPointResolverTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testIgnored() throws Exception
    {
        doTest("not-ignored", new Object(), "notIgnored");
    }

    @Test
    public void testSelected() throws Exception
    {
        doTest("selected", new Object(), "selected");
    }
}
