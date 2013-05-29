/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey;

import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

@Path("/helloworld")
public class HelloWorldCookieResource
{
    private Map<String, Cookie> cookies;
    private String testCookie;

    @POST
    @Produces("text/plain")
    public Response sayHelloWorld(@Context HttpHeaders hh,
            @CookieParam(value = "testCookie") String testCookie)
    {

        cookies = hh.getCookies();

        setTestCookie(testCookie);

        return Response.ok("Hello World").cookie(new NewCookie(cookies.get("testCookie"))).build();
    }

    public Map<String, Cookie> getCookies()
    {
        return cookies;
    }

    public void setCookies(Map<String, Cookie> cookies)
    {
        this.cookies = cookies;
    }

    public String getTestCookie()
    {
        return testCookie;
    }

    public void setTestCookie(String testCookie)
    {
        this.testCookie = testCookie;
    }

}
