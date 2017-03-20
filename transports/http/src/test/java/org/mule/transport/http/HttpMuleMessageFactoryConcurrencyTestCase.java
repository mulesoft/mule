/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.util.FileCopyUtils.copyToByteArray;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;


public class HttpMuleMessageFactoryConcurrencyTestCase extends AbstractMuleTestCase implements Runnable
{
    private final static int NUMBER_OF_THREADS = 50;

    private final HttpMultipartMuleMessageFactory httpMuleMessageFactory = new HttpMultipartMuleMessageFactory();
    private final MuleMessage previousMessage = mock(MuleMessage.class);
    private final MuleContext context = mock(MuleContext.class);
    private final CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
    private final CountDownLatch interleavingLatch = new CountDownLatch(1);
    private final DataType dataType = mock(DataType.class);
    private Exception exception = null;

    @Before
    public void setUp() throws Exception
    {
        when(previousMessage.getDataType()).thenReturn(dataType);
        when(dataType.getMimeType()).thenReturn("application/json");
    }

    @Test
    public void testDoCreate() throws Exception
    {
        for (int i = 0; i < NUMBER_OF_THREADS; i++)
        {
            Thread testThread = new Thread(this);
            testThread.start();
        }
        interleavingLatch.countDown();

        if(latch.await(3600, SECONDS))
        {
            if(this.exception != null)
            {
                fail("Concurrency exception was caught: " + exception);
            }
        }
        else
        {
            fail("Failed by Timeout");
        }
    }

    public HttpRequest createMultipartFormDataRequest() throws IOException {
        HttpRequest httpRequest = mock(HttpRequest.class, RETURNS_DEEP_STUBS);
        Part[] parts = getPartArray();
        MultipartRequestEntity multipartRequestEntity =
                new MultipartRequestEntity(parts, new PostMethod().getParams());
        ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
        multipartRequestEntity.writeRequest(requestContent);
        final ByteArrayInputStream is = new ByteArrayInputStream (requestContent.toByteArray());
        when(httpRequest.getContentType()).thenReturn(multipartRequestEntity.getContentType());
        when(httpRequest.getBody()).thenReturn(is);
        when(httpRequest.getRequestLine().getUri()).thenReturn("");
        when(httpRequest.getHeaders()).thenReturn(new Header[0]);
        return httpRequest;
    }

    @Override
    public void run()
    {
        try
        {
            interleavingLatch.await();
            httpMuleMessageFactory.doCreate(createMultipartFormDataRequest(), previousMessage, UTF_8, context);
        }
        catch (Exception e)
        {
            exception = e;
        }
        finally
        {
            latch.countDown();
        }
    }

    private Part [] getPartArray () throws IOException
    {
        byte[] fileContent = copyToByteArray(getClass().getResourceAsStream("/utils/test-request-multipart-data"));
        return new Part[] {
                new FilePart("part1", new ByteArrayPartSource("/chunking-test.xml", fileContent)),
                new FilePart("part2", new ByteArrayPartSource("/chunking-test.xml", fileContent)),
                new FilePart("part3", new ByteArrayPartSource("/chunking-test.xml", fileContent)),
                new FilePart("part4", new ByteArrayPartSource("/chunking-test.xml", fileContent)),
                new FilePart("payload", new ByteArrayPartSource("/chunking-test.xml", fileContent))
        };
    }


}
