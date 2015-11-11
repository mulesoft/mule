/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("/")
public class MultipartMessageResource
{
    @POST
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    public Response createJob(@FormDataParam("uploadedFile") FormDataBodyPart dataBodyParts) throws IOException, URISyntaxException
    {
        return Response.status(200).entity("Got " + dataBodyParts.getName()).build();
    }

}
