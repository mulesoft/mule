/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class HttpStandardRevocationConfig extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "http-requester-standard-revocation-config.xml";
    }

    @Test
    public void revocationWorksWithoutTrustStore() throws Exception
    {
        MuleEvent test = runFlow("test");
        String response = test.getMessage().getPayloadAsString();
        assertThat(response, containsString("webcache.googleusercontent.com"));
    }
}
