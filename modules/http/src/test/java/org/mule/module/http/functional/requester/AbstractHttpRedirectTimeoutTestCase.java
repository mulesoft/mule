/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester;

import static java.lang.String.format;
import static java.lang.String.valueOf;

import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Rule;

public class AbstractHttpRedirectTimeoutTestCase extends AbstractHttpRequestTestCase
{

    private static long DELAY ;
    @Rule
    public SystemProperty timeoutProperty ;

    private String REDIRECT_URL = format("http://localhost:%s/%s", httpPort.getNumber(), "secondPath");

    public AbstractHttpRedirectTimeoutTestCase(long timeout, long delay)
    {
        timeoutProperty = new SystemProperty("timeout", valueOf(timeout));
        DELAY = delay;
    }

    protected String getConfigFile()
    {
        return "http-redirect-timeout-config.xml";
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if(request.getRequestURI().contains("firstPath"))
        {
            response.setStatus(302);
            response.setHeader("Location", REDIRECT_URL);
        }
        else if (request.getRequestURI().contains("secondPath"))
        {
            try
            {
                Thread.sleep(DELAY);
                response.getOutputStream().print("OK");
            }
            catch (InterruptedException e)
            {
                //Ignore interrupted exception.
            }
        }
        response.getOutputStream().flush();
    }
}
