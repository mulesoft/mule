/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class HelloWorldComponent
{

    private HelloWorldInterface helloWorldBinding;

    @GET
    @Path("/sayHello")
    @Produces("text/plain")
    public String sayHelloFromBinding()
    {
        return helloWorldBinding.sayHello("s");
    }

    public void setHelloWorldBinding(HelloWorldInterface helloWorldBinding)
    {
        this.helloWorldBinding = helloWorldBinding;
    }

    public HelloWorldInterface getHelloWorldBinding()
    {
        return this.helloWorldBinding;
    }
}
