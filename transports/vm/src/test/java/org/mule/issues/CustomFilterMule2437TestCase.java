/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class CustomFilterMule2437TestCase extends AbstractServiceAndFlowTestCase
{

    private static final long TIMEOUT = 3000L;

    public CustomFilterMule2437TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "issues/custom-filter-mule-2437-test-service.xml"},
            {ConfigVariant.FLOW, "issues/custom-filter-mule-2437-test-flow.xml"}
        });
    }      
    
    @Test
    public void testVowels() throws Exception
    {
        doTest("aei", "vm://vowels");
    }

    @Test
    public void testConsonants() throws Exception
    {
        doTest("zyx", "vm://consonants");
    }

    protected void doTest(String message, String destination) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", new DefaultMuleMessage(message, muleContext));
        MuleMessage response = client.request(destination, TIMEOUT);
        assertNotNull(response);
        assertEquals(message, response.getPayloadAsString());
    }

}
