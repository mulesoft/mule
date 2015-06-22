/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class
        HttpUndefinedMethodsBodyTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    private String methodName;
    private HttpClient client;

    public HttpUndefinedMethodsBodyTestCase(String methodName)
    {
        this.methodName = methodName;
        client = new HttpClient();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {{HttpConstants.METHOD_DELETE}, {HttpConstants.METHOD_GET}});
    }

    @Override
    protected String getConfigFile()
    {
        return "http-undefined-methods-body-config.xml";
    }

    @Test
    public void sendBody() throws Exception
    {
        sendRequestAndAssertMethod(TEST_MESSAGE, TEST_MESSAGE);
    }

    @Test
    public void noBody() throws Exception
    {
        sendRequestAndAssertMethod(StringUtils.EMPTY, "/");
    }

    private void sendRequestAndAssertMethod(String payload, String expectedContent) throws Exception
    {
        CustomMethod method = new CustomMethod(getUrl(), methodName);
        method.setRequestEntity(new StringRequestEntity(payload, HttpConstants.DEFAULT_CONTENT_TYPE, "UTF-8"));
        int statusCode = client.executeMethod(method);

        assertEquals(HttpStatus.SC_OK, statusCode);
        assertEquals(expectedContent, method.getResponseBodyAsString());
    }

    private String getUrl()
    {
        return "http://localhost:" + port.getValue();
    }

    class CustomMethod extends EntityEnclosingMethod
    {

        private final String methodName;

        public CustomMethod(String uri, String methodName)
        {
            super(uri);
            this.methodName = methodName;
        }

        @Override
        public String getName()
        {
            return methodName;
        }
    }

}
