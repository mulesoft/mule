/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.functional;


/**
 * See MULE-4491 "Http outbound endpoint does not use responseTimeout attribute"
 * See MULE-4743 "MuleClient.send() timeout is not respected with http transport"
 * See MULE-4490 "Outbound CXF endpoint does not propagate any properties to "protocol" endpoint"
 * See {@link org.mule.transport.http.functional.HttpResponseTimeoutTestCase}
 */
public class HttpResponseTimeoutTestCase extends
    org.mule.transport.http.functional.HttpResponseTimeoutTestCase
{

    private static String PAYLOAD = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                    + "<soap:Body><invoke><arg0>Eugene</arg0></invoke></soap:Body></soap:Envelope>";

    protected String getPayload()
    {
        return HttpResponseTimeoutTestCase.PAYLOAD;
    }

}
