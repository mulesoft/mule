/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Rule;
import org.junit.Test;

public class HttpPatchTestCase extends FunctionalTestCase
{
    private static final String REQUEST = "{ \"name\" : \"alan\" }";

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "mule-http-patch.xml";
    }

    @Test
    public void patchBodyShouldBeEchoed() throws Exception
    {
        String url = String.format("http://localhost:%d/httpPatch", port.getNumber());
        PatchMethod patch = new PatchMethod(url);

        RequestEntity requestEntity = new StringRequestEntity(REQUEST, "text/plain", "UTF-8");
        patch.setRequestEntity(requestEntity);

        new HttpClient().executeMethod(patch);

        String response = patch.getResponseBodyAsString();
        assertEquals(REQUEST, response);
    }
}
