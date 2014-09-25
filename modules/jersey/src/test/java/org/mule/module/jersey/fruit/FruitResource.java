/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.fruit;

import org.mule.tck.testmodels.fruit.Apple;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class FruitResource
{

    @POST
    @Produces("text/plain")
    @Path("/tasteApple")
    public Apple tasteApple(Apple apple)
    {
        return apple;
    }
}
