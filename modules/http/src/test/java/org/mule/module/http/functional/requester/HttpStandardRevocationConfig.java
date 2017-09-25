/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

public class HttpStandardRevocationConfig extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "http-requester-standard-revocation-config.xml";
    }

    @Before
    public void setup()
    {
        Boolean reachable = false;
        try {
            URL url = new URL("http://www.google.com/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            if (conn.getResponseCode() == 200)
            {
                reachable = true;
                conn.disconnect();
            }
        }
        catch (Exception e)
        {
            // ignore, already false by default
        }

        assumeTrue("Check for internet connection, and access to www.google.com", reachable);
    }

    @Test
    public void revocationWorksWithoutTrustStore() throws Exception
    {
        MuleEvent test = runFlow("test");
        String response = test.getMessage().getPayloadAsString();
        assertThat(response, containsString("webcache.googleusercontent.com"));
    }
}
