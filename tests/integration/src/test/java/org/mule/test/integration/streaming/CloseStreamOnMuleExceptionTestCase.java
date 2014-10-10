/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;

import static org.junit.Assert.assertTrue;

import org.mule.api.client.MuleClient;
import org.mule.module.xml.stax.DelegateXMLStreamReader;
import org.mule.module.xml.stax.StaxSource;
import org.mule.module.xml.util.XMLUtils;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.concurrent.Latch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.InputSource;

public class CloseStreamOnMuleExceptionTestCase extends AbstractServiceAndFlowTestCase
{
    private final int timeoutMs = 3000;
    private static Latch inputStreamLatch = new Latch();
    private static Latch streamReaderLatch;
    private String xmlText = "<test attribute=\"1\"/>";
    private TestByteArrayInputStream inputStream;
    private MuleClient client;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
            "org/mule/test/integration/streaming/close-stream-on-mule-exception-test-service.xml"},
            {ConfigVariant.FLOW,
            "org/mule/test/integration/streaming/close-stream-on-mule-exception-test-flow.xml"}});
    }

    public CloseStreamOnMuleExceptionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        client = muleContext.getClient();
        inputStream = new TestByteArrayInputStream(xmlText.getBytes());
        streamReaderLatch = new Latch();
    }

    @Test
    public void testCloseStreamOnComponentException() throws Exception
    {
        client.dispatch("vm://inEcho?connector=vm", inputStream, null);
        streamReaderLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        assertTrue(inputStream.isClosed());
    }

    @Test
    public void testCloseXMLInputSourceOnComponentException() throws Exception
    {
        InputSource stream = new InputSource(inputStream);

        client.dispatch("vm://inEcho?connector=vm", stream, null);

        streamReaderLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        assertTrue(((TestByteArrayInputStream) stream.getByteStream()).isClosed());
    }

    @Test
    public void testCloseXMLStreamSourceOnComponentException() throws Exception
    {
        Source stream = XMLUtils.toXmlSource(XMLInputFactory.newInstance(), false, inputStream);

        client.dispatch("vm://inEcho?connector=vm", stream, null);

        streamReaderLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        assertTrue(((TestByteArrayInputStream) ((StreamSource) stream).getInputStream()).isClosed());
    }

    @Test
    public void testCloseXMLStreamReaderOnComponentException() throws Exception
    {
        TestXMLStreamReader stream = new TestXMLStreamReader(XMLInputFactory.newInstance()
            .createXMLStreamReader(inputStream));

        client.dispatch("vm://inEcho?connector=vm", stream, null);

        streamReaderLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        assertTrue(stream.isClosed());
    }

    @Test
    public void testCloseSaxSourceOnComponentException() throws Exception
    {
        SAXSource stream = new SAXSource(new InputSource(inputStream));

        client.dispatch("vm://inEcho?connector=vm", stream, null);

        verifyInputStreamIsClosed(((TestByteArrayInputStream) stream.getInputSource().getByteStream()));
    }

    @Test
    public void testCloseStaxSourceOnComponentException() throws Exception
    {

        StaxSource stream = new StaxSource(new TestXMLStreamReader(XMLInputFactory.newInstance()
            .createXMLStreamReader(inputStream)));

        client.dispatch("vm://inEcho?connector=vm", stream, null);

        verifyInputStreamIsClosed(((TestXMLStreamReader) stream.getXMLStreamReader()));
    }

    @Test
    public void testCloseStreamOnDispatcherException() throws Exception
    {
        client.dispatch("vm://dispatcherExceptionBridge?connector=vm", inputStream, null);
        verifyInputStreamIsClosed(inputStream);
    }

    @Test
    public void testCloseStreamOnInboundFilterException() throws Exception
    {
        client.dispatch("vm://inboundFilterExceptionBridge?connector=vm", inputStream, null);

        verifyInputStreamIsClosed(inputStream);
    }

    private void verifyInputStreamIsClosed(final ClosableInputStream is)
    {
        final PollingProber pollingProber = new PollingProber(timeoutMs, 100);
        pollingProber.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return is.isClosed();
            }

            @Override
            public String describeFailure()
            {
                return "Input stream was never closed";
            }
        });
    }

    interface ClosableInputStream
    {
        boolean isClosed();
    }

    static class TestByteArrayInputStream extends ByteArrayInputStream implements ClosableInputStream
    {
        private boolean closed;

        public boolean isClosed()
        {
            return closed;
        }

        public TestByteArrayInputStream(byte[] arg0)
        {
            super(arg0);
        }

        public TestByteArrayInputStream(byte[] buf, int offset, int length)
        {
            super(buf, offset, length);
        }

        @Override
        public void close() throws IOException
        {
            super.close();
            closed = true;
            inputStreamLatch.countDown();
        }
    }

    static class TestXMLStreamReader extends DelegateXMLStreamReader implements ClosableInputStream
    {
        private boolean closed;

        public boolean isClosed()
        {
            return closed;
        }

        public TestXMLStreamReader(XMLStreamReader reader)
        {
            super(reader);
        }

        @Override
        public void close() throws XMLStreamException
        {
            super.close();
            closed = true;
            streamReaderLatch.countDown();
        }
    }
}
