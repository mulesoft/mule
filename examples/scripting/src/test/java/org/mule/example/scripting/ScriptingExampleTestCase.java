/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.scripting;

import org.mule.tck.junit4.FunctionalTestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScriptingExampleTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "mule-config.xml";
    }

    @Test
    public void testDollars() throws Exception
    {
        runTest(2.33, "USD", "[9 quarters, 0 dimes, 1 nickels, 3 pennies]");
    }
    
    @Test
    public void testPounds() throws Exception
    {
        runTest(1.28, "GBP", "[1 pounds, 1 twenty_pence, 1 five_pence, 1 two_pence, 1 pennies]");
    }
    
    private void runTest(double amount, String currency, String expectedResult) throws Exception
    {
        String url = String.format("http://localhost:47493/change-machine?amount=%1f&currency=%2s",
            amount, currency);
        GetMethod httpGet = new GetMethod(url);
        new HttpClient().executeMethod(httpGet);
        String result =  httpGet.getResponseBodyAsString();
        assertEquals(expectedResult, result);
    }        
}
