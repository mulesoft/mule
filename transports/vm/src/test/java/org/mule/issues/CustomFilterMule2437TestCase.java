/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.issues;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class CustomFilterMule2437TestCase extends FunctionalTestCase
{

    private static final long TIMEOUT = 3000L;

    protected String getConfigResources()
    {
        return "issues/custom-filter-mule-2437-test.xml";
    }

    public void testVowels() throws Exception
    {
        doTest("aei", "vm://vowels");
    }

    public void testConsonants() throws Exception
    {
        doTest("zyx", "vm://consonants");
    }

    protected void doTest(String message, String destination) throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://in?connector=default", new MuleMessage(message));
        UMOMessage response = client.request(destination + "?connector=queue", TIMEOUT);
        assertNotNull(response);
        assertEquals(message, response.getPayloadAsString());
    }

}
