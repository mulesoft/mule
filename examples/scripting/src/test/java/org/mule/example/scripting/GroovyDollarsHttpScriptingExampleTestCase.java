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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class GroovyDollarsHttpScriptingExampleTestCase extends AbstractScriptingExampleTestCase
{
    @Override
    protected String getScriptFile()
    {
        return "greedy.groovy";
    }

    @Override
    protected String getCurrency()
    {
        return "USD";
    }

    public void testHttpRequest() throws Exception
    {
        GetMethod httpGet = new GetMethod("http://localhost:47493/change-machine?amount=2.33");
        new HttpClient().executeMethod(httpGet);
        String result =  httpGet.getResponseBodyAsString();
        assertEquals("[9 quarters, 0 dimes, 1 nickels, 3 pennies]", result);
    }
}
