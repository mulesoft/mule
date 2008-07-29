/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.streaming;

import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.module.xml.stax.DelegateXMLStreamReader;
import org.mule.module.xml.stax.StaxSource;
import org.mule.module.xml.util.XMLUtils;
import org.mule.tck.FunctionalTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;

public class CloseStreanOnMuleExceptionTestCase extends FunctionalTestCase
{

    private String xmlText = "<test attribute=\"1\"/>";
    private TestByteArrayInputStream inputStream;
    MuleClient client;

    // @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        client = new MuleClient();
        inputStream = new TestByteArrayInputStream(xmlText.getBytes());
    }

    public void testCloseStreamOnComponentException() throws MuleException, InterruptedException, IOException
    {

        client.send("vm://inEcho?connector=vm", inputStream, null);
        assertTrue(inputStream.isClosed());
    }

    public void testCloseXMLInputSourceOnComponentException()
        throws MuleException, InterruptedException, IOException
    {
        InputSource stream = new InputSource(inputStream);

        client.send("vm://inEcho?connector=vm", stream, null);

        assertTrue(((TestByteArrayInputStream) stream.getByteStream()).isClosed());
    }

    public void testCloseXMLStreamSourceOnComponentException() throws FactoryConfigurationError, Exception
    {
        Source stream = XMLUtils.toXmlSource(XMLInputFactory.newInstance(), false, inputStream);

        client.send("vm://inEcho?connector=vm", stream, null);

        assertTrue(((TestByteArrayInputStream) ((StreamSource) stream).getInputStream()).isClosed());
    }

    public void testCloseXMLStreamReaderOnComponentException()
        throws MuleException, InterruptedException, IOException, XMLStreamException,
        FactoryConfigurationError
    {
        TestXMLStreamReader stream = new TestXMLStreamReader(XMLInputFactory.newInstance()
            .createXMLStreamReader(inputStream));

        client.send("vm://inEcho?connector=vm", stream, null);

        assertTrue(stream.isClosed());
    }

    public void testCloseSaxSourceOnComponentException()
        throws MuleException, InterruptedException, IOException, XMLStreamException,
        FactoryConfigurationError
    {
        SAXSource stream = new SAXSource(new InputSource(inputStream));

        client.send("vm://inEcho?connector=vm", stream, null);

        assertTrue(((TestByteArrayInputStream) stream.getInputSource().getByteStream()).isClosed());
    }

    public void testCloseStaxSourceOnComponentException()
        throws MuleException, InterruptedException, IOException, XMLStreamException,
        FactoryConfigurationError
    {

        StaxSource stream = new StaxSource(new TestXMLStreamReader(XMLInputFactory.newInstance()
            .createXMLStreamReader(inputStream)));

        client.send("vm://inEcho?connector=vm", stream, null);

        assertTrue(((TestXMLStreamReader) stream.getXMLStreamReader()).isClosed());
    }

    public void testCloseStreamOnDispatcherException()
        throws MuleException, InterruptedException, IOException
    {
        client.send("vm://dispatcherExceptionBridge?connector=vm", inputStream, null);

        assertTrue(((TestByteArrayInputStream) inputStream).isClosed());
    }

    // TODO MULE-3558 Streams are not closed if there are exceptions in the message
    // receiver. Protocol/Transport workers should clean up after themselves if there
    // is an error (MULE-3559) but exceptions thrown by AbstractMessageReciever will
    // not result in stream being closed. These exceptions result in
    // exceptionStrategy being called but because RequestContext is empty the message
    // is not available in the AbstractExceptionListener and cannot be closed.

//    public void testCloseStreamOnInboundFilterException()
//        throws MuleException, InterruptedException, IOException
//    {
//        client.dispatch("vm://inboundFilterExceptionBridge?connector=vm", inputStream, null);
//
//        Thread.sleep(200);
//
//        assertTrue(((TestByteArrayInputStream) inputStream).isClosed());
//    }


    // @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/streaming/close-stream-on-mule-exception-test.xml";
    }

    static class TestByteArrayInputStream extends ByteArrayInputStream
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
        }
    }

    static class TestXMLStreamReader extends DelegateXMLStreamReader
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

        // @Override
        public void close() throws XMLStreamException
        {
            super.close();
            closed = true;
        }
    }

}
