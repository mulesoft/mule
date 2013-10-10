/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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
