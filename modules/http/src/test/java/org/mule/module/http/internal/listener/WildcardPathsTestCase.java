/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.listener;


import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class WildcardPathsTestCase extends FunctionalTestCase
{
    private static final String response1= "V1 Flow invoked";
    private static final String response2= "V2 flow invoked";
    private static final String response3= "V2 - Healthcheck";
    private static final String path1= "/*";
    private static final String path2= "V2/*";
    private static final String path3= "V2/taxes/healthcheck";
    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Rule
    public SystemProperty systemPropertyPath1 = new SystemProperty("path1", path1);

    @Rule
    public SystemProperty systemPropertyPath2 = new SystemProperty("path2", path2);

    @Rule
    public SystemProperty systemPropertyPath3= new SystemProperty("path3", path3);

    @Rule
    public SystemProperty systemPropertyResponse1 = new SystemProperty("response1", response1);

    @Rule
    public SystemProperty systemPropertyResponse2 = new SystemProperty("response2", response2);

    @Rule
    public SystemProperty systemPropertyResponse3 = new SystemProperty("response3", response3);

    private static final String HOST ="http://localhost:%s/%s";
    private String path;
    private String response;

    @Test
    public void testPath() throws Exception {
        final String url = String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
        final Response httpResponse = Request.Get(url).execute();
        assertThat(httpResponse.returnContent().asString(), is(response));
    }

    @org.junit.runners.Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{"",response1}, {"taxes",response1},{"taxes/healtcheck",response1},
                {"taxes/1",response1},{"V2",response2},{"V2/taxes",response2}, { "V2/console",response2},
                {"V2/taxes/1",response2}, {"V2/taxes/healthcheck",response3}});
    }

    public WildcardPathsTestCase(String path, String response) {
        this.path = path;
        this.response = response;
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder("HttpTestPathsWildcard.xml");
    }


    private String createURL(String path){
        return String.format(HOST,listenPort.getNumber(),path);
    }
}
