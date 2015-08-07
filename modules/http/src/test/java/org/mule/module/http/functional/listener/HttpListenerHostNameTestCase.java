/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.StringEntity;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

public class HttpListenerHostNameTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-host-name-config.xml";
    }

    @Test
    public void routeToTheRightListener() throws Exception
    {
        final String url = String.format("http://localhost:%s/", listenPort.getNumber());
        final Response response = Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
        assertThat(response.returnContent().asString(), is("ok"));
    }

}
