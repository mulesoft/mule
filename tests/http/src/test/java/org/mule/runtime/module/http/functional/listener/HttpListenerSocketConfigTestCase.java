/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.listener;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.module.socket.api.SocketsExtension;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.StringEntity;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerSocketConfigTestCase extends ExtensionFunctionalTestCase
{

    @Rule
    public DynamicPort listenPort1 = new DynamicPort("port1");


    @Override
    protected String getConfigFile()
    {
        return "http-listener-socket-config.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {SocketsExtension.class};
    }

    @Test
    public void globalServerSocketProperties() throws Exception
    {
        // For now, just test that the context is parsed correctly.
        assertResponse(listenPort1.getNumber(), "global");
    }

    private void assertResponse(int port, String path) throws Exception
    {
        final String url = String.format("http://localhost:%s/%s", port, path);
        final Response response = Request.Post(url).body(new StringEntity(TEST_MESSAGE)).connectTimeout(1000).execute();
        assertThat(response.returnContent().asString(), equalTo(TEST_MESSAGE));

    }

}
