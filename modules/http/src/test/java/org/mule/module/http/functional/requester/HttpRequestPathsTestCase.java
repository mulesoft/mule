/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;

import org.junit.Test;

public class HttpRequestPathsTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-paths-config.xml";
    }

    @Test
    public void emptyPathWithoutBasePath() throws Exception
    {
        assertRequestUri("requestWithoutBasePath", "", "", "/");
    }

    @Test
    public void rootPathWithoutBasePath() throws Exception
    {
        assertRequestUri("requestWithoutBasePath", "", "/", "/");
    }

    @Test
    public void customPathNoSlashWithoutBasePath() throws Exception
    {
        assertRequestUri("requestWithoutBasePath", "", "testPath", "/testPath");
    }

    @Test
    public void customPathWithSlashWithoutBasePath() throws Exception
    {
        assertRequestUri("requestWithoutBasePath", "", "/testPath", "/testPath");
    }

    @Test
    public void emptyPathWithBasePathNoSlash() throws Exception
    {
        assertRequestUri("requestWithBasePath", "basePath", "", "/basePath");
    }

    @Test
    public void rootPathWithBasePathNoSlash() throws Exception
    {
        assertRequestUri("requestWithBasePath", "basePath", "/", "/basePath/");
    }

    @Test
    public void customPathNoSlashWitBasePathNoSlash() throws Exception
    {
        assertRequestUri("requestWithBasePath", "basePath", "testPath", "/basePath/testPath");
    }

    @Test
    public void customPathWithSlashWithBasePathNoSlash() throws Exception
    {
        assertRequestUri("requestWithBasePath", "basePath", "/testPath", "/basePath/testPath");
    }

    @Test
    public void emptyPathWithBasePathWithSlash() throws Exception
    {
        assertRequestUri("requestWithBasePath", "basePath/", "", "/basePath/");
    }

    @Test
    public void rootPathWithBasePathWithSlash() throws Exception
    {
        assertRequestUri("requestWithBasePath", "basePath/", "/", "/basePath/");
    }

    @Test
    public void customPathNoSlashWitBasePathWithSlash() throws Exception
    {
        assertRequestUri("requestWithBasePath", "basePath/", "testPath", "/basePath/testPath");
    }

    @Test
    public void customPathWithSlashWithBasePathWithSlash() throws Exception
    {
        assertRequestUri("requestWithBasePath", "basePath/", "/testPath", "/basePath/testPath");
    }

    @Test
    public void customPathWithSpaceAndEncodedCharacter() throws Exception
    {
        // Spaces should be replaced by "%20", but any other encoded character must not be modified.
        assertRequestUri("requestWithBasePath", "base Path%25", "test Path%25?k1=v%25&k2=v2", "/base%20Path%25/test%20Path%25?k1=v%25&k2=v2");
    }





    private void assertRequestUri(String flowName, String basePath, String requestPath, String expectedUri) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event.getMessage().setInvocationProperty("basePath", basePath);
        event.getMessage().setInvocationProperty("requestPath", requestPath);
        flow.process(event);
        assertThat(uri, equalTo(expectedUri));

    }

}
