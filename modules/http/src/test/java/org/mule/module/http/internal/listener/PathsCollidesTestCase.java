/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.listener;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.util.Collection;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PathsCollidesTestCase extends FunctionalTestCase
{

    private static final String path1 = "prefix/*/suffix";
    private static final String path2 = "prefix/keyword/differentSuffix/*";
    private static final String response1 = "First Listener invoked";
    private static final String response2 = "Second Listener invoked";

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Rule
    public SystemProperty systemPropertyPath1 = new SystemProperty("path1", path1);

    @Rule
    public SystemProperty systemPropertyPath2 = new SystemProperty("path2", path2);

    @Rule
    public SystemProperty systemPropertyResponse1 = new SystemProperty("response1", response1);

    @Rule
    public SystemProperty systemPropertyResponse2 = new SystemProperty("response2", response2);

    private final String path;
    private final String response;
    private final String URL = "http://localhost:%s/%s";

    public PathsCollidesTestCase(String path, String response)
    {
        this.path = path;
        this.response = response;
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder("paths-collides.xml");
    }

    @Parameters
    public static Collection<Object[]> data()
    {
        return asList(new Object[][] {
                {"prefix/keyword/suffix", response1},
                {"prefix/other-keyword/suffix", response1},
                {"prefix/keyword/differentSuffix", response2},
                {"prefix/keyword/differentSuffix/1", response2}}
        );
    }

    @Test
    public void testPath() throws IOException
    {
        final String url = String.format(URL, listenPort.getNumber(), path);
        final Response httpResponse = Request.Get(url).execute();
        assertThat(httpResponse.returnContent().asString(), is(response));
    }

}
