/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.module.http.api.HttpHeaders.Names.EXPECT;
import static org.mule.module.http.api.HttpHeaders.Names.HOST;
import static org.mule.module.http.api.HttpHeaders.Values.CONTINUE;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpListenerExpectHeaderStreamingNeverTestCase extends FunctionalTestCase
{

    private static final String HTTP_11 = "HTTP/1.1";
    private static final String LISTEN_HOST = "localhost";
    private static final String CONTINUE_RESPONSE = "HTTP/1.1 100 Continue\r\n\r\n";
    private static final String EXPECTATION_FAILED_RESPONSE = "HTTP/1.1 417 Expectation Failed\r\n";

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Rule
    public SystemProperty persistentConnections;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{TRUE.toString()},{FALSE.toString()}});
    }

    @Override
    protected String getConfigFile()
    {
        return "http-listener-expect-header-streaming-never-config.xml";
    }

    public HttpListenerExpectHeaderStreamingNeverTestCase(String persistentConnections)
    {
        this.persistentConnections = new SystemProperty("persistentConnections", persistentConnections);
    }

    @Before
    public void setup() throws IOException
    {
        socket = new Socket();
        socket.connect(new InetSocketAddress(LISTEN_HOST, listenPort.getNumber()));
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
    }

    @After
    public void tearDown() throws Exception
    {
        outputStream.close();
        inputStream.close();
        socket.close();
    }

    @Test
    public void http11WithoutExpectHeader() throws Exception
    {
        sendAndReceiveWithoutExpectContinue();

        // If persistent connection are being used ensure first request/response completed cleanly and second request
        // can be made.
        if (isPersistentConnections())
        {
            sendAndReceiveWithoutExpectContinue();
        }
    }

    @Test
    public void http11WithExpectHeader() throws Exception
    {
        sendHeaders(outputStream, HTTP_11, CONTINUE);
        readAndAssertContinueResponse(inputStream);
        sendBody(outputStream);
        readAndAssertResponse(inputStream);

        // If persistent connection are being used ensure first request/response completed cleanly and second request
        // can be made.
        if (isPersistentConnections())
        {
            sendAndReceiveWithoutExpectContinue();
        }
    }

    @Test
    public void http11WithExpectHeaderInvalidValue() throws Exception
    {
        sendHeaders(outputStream, HTTP_11, "invalidExpect");
        assertFromStream(inputStream, EXPECTATION_FAILED_RESPONSE);
    }

    private void sendAndReceiveWithoutExpectContinue() throws IOException
    {
        sendHeaders(outputStream, HTTP_11, null);
        sendBody(outputStream);
        readAndAssertResponse(inputStream);
    }

    private boolean isPersistentConnections() throws IOException
    {
        return Boolean.parseBoolean(persistentConnections.getValue());
    }

    /**
     * Sends the request line and headers of an HTTP request over an output stream.
     */
    private void sendHeaders(OutputStream outputStream, String httpVersion, String expectHeaderValue) throws IOException
    {
        String request = String.format("%s / %s\n" +
                                       "%s: %s\n" +
                                       "%s: %d\n", POST.name(), httpVersion, HOST, LISTEN_HOST, CONTENT_LENGTH, TEST_MESSAGE.length());

        if (expectHeaderValue != null)
        {
            request += String.format("%s: %s\n", EXPECT, expectHeaderValue);
        }

        request += "\n";

        outputStream.write(request.getBytes());
        outputStream.flush();
    }

    private void sendBody(OutputStream outputStream) throws IOException
    {
        outputStream.write(TEST_MESSAGE.getBytes());
        outputStream.flush();
    }

    private void readAndAssertContinueResponse(InputStream inputStream) throws IOException
    {
        assertFromStream(inputStream, CONTINUE_RESPONSE);
    }

    /**
     * Reads and HTTP response from an InputStream, and asserts that the body matches TEST_MESSAGE.
     */
    private void readAndAssertResponse(InputStream inputStream) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 1);

        while (!reader.readLine().isEmpty())
        {
            // Do nothing, consume headers until blank line.
        }

        char[] body = new char[getExpectedResponseBody().length()];
        IOUtils.read(reader, body);
        assertThat(new String(body), equalTo(getExpectedResponseBody()));
    }

    private void assertFromStream(InputStream inputStream, String expectedInput) throws IOException
    {
        byte[] actualInput = new byte[expectedInput.length()];
        IOUtils.read(inputStream, actualInput);
        assertThat(new String(actualInput), equalTo(expectedInput));
    }

    protected String getExpectedResponseBody()
    {
        return TEST_MESSAGE;
    }

}
