/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CustomFilterMule2437TestCase extends FunctionalTestCase
{

    private static final long TIMEOUT = 3000L;

    @Override
    protected String getConfigResources()
    {
        return "issues/custom-filter-mule-2437-test.xml";
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
