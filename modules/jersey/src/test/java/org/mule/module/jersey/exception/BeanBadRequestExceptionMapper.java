/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey.exception;

import org.mule.transformer.types.MimeTypes;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class BeanBadRequestExceptionMapper implements ExceptionMapper<BeanBadRequestException>
{

    public Response toResponse(BeanBadRequestException exception)
    {
        int status = Response.Status.BAD_REQUEST.getStatusCode();
        return Response.status(status).entity(exception.getMessage()).type(MimeTypes.TEXT).build();
    }
}
