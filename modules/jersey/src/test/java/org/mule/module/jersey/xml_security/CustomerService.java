/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.xml_security;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/service")
public interface CustomerService
{
    @POST
    @Path("/customer")
    @Produces({"application/xml; charset=UTF-8"})
    Customer updateCustomer(Customer customer);

    @GET
    @Path("/customer/{index}")
    @Produces({"application/xml; charset=UTF-8"})
    Customer getCustomer(String index);
}
