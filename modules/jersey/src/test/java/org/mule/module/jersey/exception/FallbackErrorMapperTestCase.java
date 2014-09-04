/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.exception;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

@SmallTest
public class FallbackErrorMapperTestCase extends AbstractMuleTestCase
{

    @Test
    public void map()
    {
        Exception e = new RuntimeException("Hello World!");
        Response response = new FallbackErrorMapper().toResponse(e);

        assertThat((String) response.getEntity(), equalTo(e.getMessage()));
        assertThat(response.getStatus(), equalTo(INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.getMediaType(), equalTo(MediaType.TEXT_PLAIN_TYPE));
    }
}
