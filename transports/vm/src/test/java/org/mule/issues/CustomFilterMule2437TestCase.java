/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class CustomFilterMule2437TestCase extends FunctionalTestCase
{

    private static final long TIMEOUT = 3000L;

    @Override
    protected String getConfigFile()
    {
        return "issues/custom-filter-mule-2437-test-flow.xml";
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
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", getTestMuleMessage(message));
        MuleMessage response = client.request(destination, TIMEOUT);
        assertNotNull(response);
        assertEquals(message, getPayloadAsString(response));
    }
}
