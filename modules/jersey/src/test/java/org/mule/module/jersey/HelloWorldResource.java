/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jersey;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.mule.module.jersey.exception.HelloWorldException;

@Path("/helloworld")
public class HelloWorldResource
{
    @POST
    @Produces("text/plain")
    public String sayHelloWorld()
    {
        return "Hello World";
    }

    @GET
    @Produces("application/json")
    @Path("/sayHelloWithJson/{name}")
    public HelloBean sayHelloWithJson(@PathParam("name") String name)
    {
        HelloBean hello = new HelloBean();
        hello.setMessage("Hello " + name);
        return hello;
    }

    @DELETE
    @Produces("text/plain")
    public String deleteHelloWorld()
    {
        return "Hello World Delete";
    }

    @GET
    @Produces("text/plain")
    @Path("/sayHelloWithUri/{name}")
    public String sayHelloWithUri(@PathParam("name") String name)
    {
        return "Hello " + name;
    }

    @GET
    @Produces("text/plain")
    @Path("/sayHelloWithHeader")
    public Response sayHelloWithHeader(@HeaderParam("X-Name") String name)
    {
        return Response.status(201).header("X-ResponseName", name).entity("Hello " + name).build();
    }

    @GET
    @Produces("text/plain")
    @Path("/sayHelloWithQuery")
    public String sayHelloWithQuery(@QueryParam("name") String name)
    {
        return "Hello " + name;
    }
    
    @GET
    @Produces("text/plain")
    @Path("/throwException")
    public String throwException() throws HelloWorldException
    {
        throw new HelloWorldException("This is an exception");
    }
}
