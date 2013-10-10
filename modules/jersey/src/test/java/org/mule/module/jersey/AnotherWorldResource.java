/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/anotherworld")
public class AnotherWorldResource 
{
    @GET
    @Produces("text/plain")
    @Path("/sayHelloWithUri/{name}")
    public String sayHelloWithUri(@PathParam("name") String name) 
    {
        return "Bonjour " + name;
    }
}
