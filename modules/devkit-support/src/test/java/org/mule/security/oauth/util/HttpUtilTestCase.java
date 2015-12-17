/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mule.module.http.api.HttpConstants.HttpStatus.ACCEPTED;
import static org.mule.module.http.api.HttpConstants.HttpStatus.CREATED;
import static org.mule.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.module.http.api.HttpConstants.HttpStatus.NON_AUTHORITATIVE_INFORMATION;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Ignore("See MULE-9199")
@RunWith(Parameterized.class)
public class HttpUtilTestCase extends FunctionalTestCase
{

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {OK.getStatusCode()},
                {CREATED.getStatusCode()},
                {ACCEPTED.getStatusCode()},
                {NON_AUTHORITATIVE_INFORMATION.getStatusCode()},
                {INTERNAL_SERVER_ERROR.getStatusCode()}
        });
    }

    private static final String SUCCESS_PAYLOAD = "ok";

    private int statusCode;
    private final HttpUtil util = new HttpUtilImpl();

    @Rule
    public DynamicPort port = new DynamicPort("port1");

    public HttpUtilTestCase(int statusCode)
    {
        this.statusCode = statusCode;
    }

    @Override
    protected String getConfigFile()
    {
        return "http-util-config.xml";
    }

    @Test
    public void testResponse() throws Exception
    {
        try
        {
            assertEquals(SUCCESS_PAYLOAD, util.post(String.format("http://localhost:%d/%d", port.getNumber(), statusCode), ""));
            assertFalse(statusCode == INTERNAL_SERVER_ERROR.getStatusCode());
        }
        catch (RuntimeException e)
        {
            assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), statusCode);
            assertTrue(e.getCause() instanceof IOException);
        }
    }


}
