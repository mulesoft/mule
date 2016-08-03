/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;

import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.module.xml.stax.DelegateXMLStreamReader;
import org.mule.runtime.module.xml.stax.StaxSource;
import org.mule.runtime.module.xml.util.XMLUtils;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import org.xml.sax.InputSource;

public class CloseStreamOnMuleExceptionTestCase extends AbstractIntegrationTestCase
{
    private final int timeoutMs = 3000;
    private static Latch inputStreamLatch = new Latch();
    private static Latch streamReaderLatch;
    private String xmlText = "<test attribute=\"1\"/>";
    private TestByteArrayInputStream inputStream;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/streaming/close-stream-on-mule-exception-test-flow.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        inputStream = new TestByteArrayInputStream(xmlText.getBytes());
        streamReaderLatch = new Latch();
    }

    @Test
    public void testCloseStreamOnComponentException() throws Exception
    {
        flowRunner("echo").withPayload(inputStream).asynchronously().run();

        streamReaderLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        assertTrue(inputStream.isClosed());
    }

    @Test
    public void testCloseXMLInputSourceOnComponentException() throws Exception
    {
        InputSource stream = new InputSource(inputStream);

        flowRunner("echo").withPayload(stream).asynchronously().run();

        streamReaderLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        assertTrue(((TestByteArrayInputStream) stream.getByteStream()).isClosed());
    }

    @Test
    public void testCloseXMLStreamSourceOnComponentException() throws Exception
    {
        Source stream = XMLUtils.toXmlSource(XMLInputFactory.newInstance(), false, inputStream);

        flowRunner("echo").withPayload(stream).asynchronously().run();

        streamReaderLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        assertTrue(((TestByteArrayInputStream) ((StreamSource) stream).getInputStream()).isClosed());
    }

    @Test
    public void testCloseXMLStreamReaderOnComponentException() throws Exception
    {
        TestXMLStreamReader stream = new TestXMLStreamReader(XMLInputFactory.newInstance()
            .createXMLStreamReader(inputStream));

        flowRunner("echo").withPayload(stream).asynchronously().run();

        streamReaderLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        assertTrue(stream.isClosed());
    }

    @Test
    public void testCloseSaxSourceOnComponentException() throws Exception
    {
        SAXSource stream = new SAXSource(new InputSource(inputStream));

        flowRunner("echo").withPayload(stream).asynchronously().run();

        verifyInputStreamIsClosed(((TestByteArrayInputStream) stream.getInputSource().getByteStream()));
    }

    @Test
    public void testCloseStaxSourceOnComponentException() throws Exception
    {
        StaxSource stream = new StaxSource(new TestXMLStreamReader(XMLInputFactory.newInstance()
            .createXMLStreamReader(inputStream)));

        flowRunner("echo").withPayload(stream).asynchronously().run();

        verifyInputStreamIsClosed(((TestXMLStreamReader) stream.getXMLStreamReader()));
    }

    @Test
    public void testCloseStreamOnInboundFilterException() throws Exception
    {
        flowRunner("inboundFilterExceptionBridge").withPayload(inputStream).asynchronously().run();

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

        @Override
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

        @Override
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
