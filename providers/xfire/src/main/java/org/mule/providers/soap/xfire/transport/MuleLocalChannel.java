/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.providers.soap.xfire.transport;

import edu.emory.mathcs.backport.java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.XFireException;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.exchange.InMessage;
import org.codehaus.xfire.exchange.OutMessage;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.transport.AbstractChannel;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.Session;
import org.codehaus.xfire.util.STAXUtils;
import org.mule.umo.manager.UMOWorkManager;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleLocalChannel extends AbstractChannel
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected static final String SENDER_URI = "senderUri";
    protected static final String OLD_CONTEXT = "urn:xfire:transport:local:oldContext";

    private final Session session;

    protected UMOWorkManager workManager;

    public MuleLocalChannel(String uri, MuleLocalTransport transport, Session session)
    {
        this.session = session;
        setUri(uri);
        setTransport(transport);
    }

    public void open()
    {
        // template method
    }

    public void send(final MessageContext context, final OutMessage message) throws XFireException
    {
        if (message.getUri().equals(Channel.BACKCHANNEL_URI)) {
            final OutputStream out = (OutputStream)context.getProperty(Channel.BACKCHANNEL_URI);
            if (out != null) {
                final XMLStreamWriter writer = STAXUtils.createXMLStreamWriter(out, message
                        .getEncoding(), context);

                message.getSerializer().writeMessage(message, writer, context);
            }
            else {
                MessageContext oldContext = (MessageContext)context.getProperty(OLD_CONTEXT);

                sendViaNewChannel(context, oldContext, message, (String)context.getProperty(SENDER_URI));
            }
        }
        else {
            MessageContext receivingContext = new MessageContext();
            receivingContext.setXFire(context.getXFire());
            receivingContext.setService(getService(context.getXFire(), message.getUri()));
            receivingContext.setProperty(OLD_CONTEXT, context);
            receivingContext.setProperty(SENDER_URI, getUri());
            receivingContext.setSession(session);

            sendViaNewChannel(context, receivingContext, message, message.getUri());
        }
    }

    protected Service getService(XFire xfire, String uri) throws XFireException
    {
        if (null == xfire) {
            logger.warn("No XFire instance in context, unable to determine service");
            return null;
        }

        int i = uri.indexOf("//");

        if (i == -1) {
            throw new XFireException("Malformed service URI");
        }

        String name = uri.substring(i + 2);
        Service service = xfire.getServiceRegistry().getService(name);

        if (null == service) {
            // TODO this should be an exception...
            logger.warn("Unable to locate '" + name + "' in ServiceRegistry");
        }

        return service;
    }

    private void sendViaNewChannel(final MessageContext context,
            final MessageContext receivingContext,
            final OutMessage message,
            final String uri) throws XFireException
    {
        try {
            Channel channel;
            PipedInputStream stream = new PipedInputStream();
            PipedOutputStream outStream = new PipedOutputStream(stream);
            try {
                channel = getTransport().createChannel(uri);
            }
            catch (Exception e) {
                throw new XFireException("Couldn't create channel.", e);
            }

            Semaphore s = new Semaphore(2);
            try {
                getWorkManager().scheduleWork(new WriterWorker(outStream, message, context, s));
                getWorkManager().scheduleWork(
                        new ReaderWorker(stream, message, channel, uri, receivingContext, s));
            }
            catch (WorkException e) {
                throw new XFireException("Couldn't schedule worker threads. " + e.getMessage(), e);
            }

            try {
                s.acquire();
            }
            catch (InterruptedException e) {
                // ignore is ok
            }
        }
        catch (IOException e) {
            throw new XFireRuntimeException("Couldn't create stream.", e);
        }
    }

    public void close()
    {
        // template method
    }

    public boolean isAsync()
    {
        return true;
    }

    UMOWorkManager getWorkManager()
    {
        return workManager;
    }

    void setWorkManager(UMOWorkManager workManager)
    {
        this.workManager = workManager;
    }

    private class ReaderWorker implements Work
    {

        private InputStream stream;
        private OutMessage message;
        private Channel channel;
        private String uri;
        private MessageContext context;
        private Semaphore semaphore;

        public ReaderWorker(InputStream stream,
                            OutMessage message,
                            Channel channel,
                            String uri,
                            MessageContext context,
                            Semaphore semaphore)
        {
            this.stream = stream;
            this.message = message;
            this.channel = channel;
            this.uri = uri;
            this.context = context;
            this.semaphore = semaphore;
        }

        public void run()
        {
            try {
                final XMLStreamReader reader = STAXUtils.createXMLStreamReader(stream, message
                        .getEncoding(), context);
                final InMessage inMessage = new InMessage(reader, uri);
                inMessage.setEncoding(message.getEncoding());

                channel.receive(context, inMessage);

                reader.close();
                stream.close();
            }
            catch (Exception e) {
                throw new XFireRuntimeException("Couldn't read stream.", e);
            }
            finally {
                semaphore.release();
            }
        }

        public void release()
        {
            // template method
        }
    }

    private class WriterWorker implements Work
    {

        private OutputStream stream;
        private OutMessage message;
        private MessageContext context;
        private Semaphore semaphore;

        public WriterWorker(OutputStream stream,
                            OutMessage message,
                            MessageContext context,
                            Semaphore semaphore)
        {
            this.stream = stream;
            this.message = message;
            this.context = context;
            this.semaphore = semaphore;
        }

        public void run()
        {
            try {
                final XMLStreamWriter writer = STAXUtils.createXMLStreamWriter(stream, message
                        .getEncoding(), context);
                message.getSerializer().writeMessage(message, writer, context);

                writer.close();
                stream.close();

            }
            catch (Exception e) {
                throw new XFireRuntimeException("Couldn't write stream.", e);
            }
            finally {
                semaphore.release();
            }
        }

        public void release()
        {
            // template method
        }
    }
}
