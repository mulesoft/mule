/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
