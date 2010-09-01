/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.tck;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/weather-report")
public class WeatherReportResource
{
    @POST
    @Consumes("application/xml")
    @Produces("text/plain")
    public Response createNewReport(String report) throws URISyntaxException
    {
        return Response.created(new URI("foo://fake_report_uri")).build();
    }
}
