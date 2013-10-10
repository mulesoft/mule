/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
