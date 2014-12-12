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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;

@Path("/")
public class ContextResolverResource
{
    @Context
    private javax.ws.rs.ext.Providers providers;

    @GET
    @Path("/resolve")
    @Produces("application/json")
    public HelloBean resolve()
    {
        ContextResolver<HelloBean> resolver = providers.getContextResolver(HelloBean.class, MediaType.WILDCARD_TYPE);
        return resolver.getContext(HelloBean.class);
    }

}
