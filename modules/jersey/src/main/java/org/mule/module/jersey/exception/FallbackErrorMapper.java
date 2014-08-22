/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.exception;

import org.mule.transformer.types.MimeTypes;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.spi.ResponseErrorMapper;

/**
 * A default {@link org.glassfish.jersey.server.spi.ResponseErrorMapper}
 * to be used when no other error mapper has been configured
 * by the user
 *
 * @since 3.6.0
 */
public class FallbackErrorMapper implements ResponseErrorMapper
{

    @Override
    public Response toResponse(Throwable throwable)
    {
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(throwable.getMessage())
                .type(MimeTypes.TEXT)
                .build();
    }
}
