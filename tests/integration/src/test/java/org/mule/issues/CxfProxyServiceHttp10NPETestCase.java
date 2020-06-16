/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import static org.apache.http.HttpVersion.HTTP_1_0;
import static org.apache.http.client.fluent.Request.Post;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;

public class CxfProxyServiceHttp10NPETestCase extends FunctionalTestCase {

    private static final int DEFAULT_TIMEOUT = 1000;

    public static final String SOAP_REQUEST_BODY = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:test=\"http://test.Pablo.name/\">"
            + "<soapenv:Header/>"
            + "<soapenv:Body>"
            + "<test:Hi/>"
            + "</soapenv:Body>"
            + "</soapenv:Envelope>";

    public static final String EXPECTED_RESPONSE_BODY = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><test:Hi xmlns:test=\"http://test.Pablo.name/\"/></soap:Body></soap:Envelope>";

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    protected HttpVersion getHttpVersion()
    {
        return HTTP_1_0;
    }

    protected String getUrl(String path)
    {
        return String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
    }

    @Override
    protected String getConfigFile()
    {
        return "cxf-proxy-service-http-10.xml";
    }

    @Test
    public void cxfOutputHandler() throws Exception
    {
        // MULE-18493: HTTP Connector leaking sockets in state CLOSE_WAIT when client uses HTTP/1.0 due to a NPE.
        final String url = getUrl("cxfOutputHandlerFlow");

        final Response response = Post(url).version(getHttpVersion())
                .connectTimeout(DEFAULT_TIMEOUT)
                .socketTimeout(DEFAULT_TIMEOUT)
                .bodyByteArray(SOAP_REQUEST_BODY.getBytes())
                .execute();
        final HttpResponse httpResponse = response.returnResponse();
        final Header transferEncodingHeader = httpResponse.getFirstHeader(TRANSFER_ENCODING);
        assertThat(transferEncodingHeader, is(nullValue()));
        assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(EXPECTED_RESPONSE_BODY));
    }

}
