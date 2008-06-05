/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationException;
import org.mule.api.endpoint.EndpointNotFoundException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.config.i18n.MessageFactory;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.cxf.transport.MuleUniversalDestination;
import org.mule.transport.cxf.transport.MuleUniversalTransport;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.soap.SoapConstants;
import org.mule.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.xmlsoap.schemas.wsdl.http.AddressType;

/**
 * The CXF receives messages from Mule, converts them into CXF messages and dispatches
 * them into the receiving CXF destination.
 */
public class CxfServiceComponent implements Callable, Lifecycle
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected Bus bus;

    // manager to the component
    protected MuleUniversalTransport universalTransport;
    protected String transportClass;

    private CxfMessageReceiver receiver;

    public CxfServiceComponent(CxfMessageReceiver receiver) throws ConfigurationException
    {
        super();
        this.receiver = receiver;
        this.bus = receiver.connector.getCxfBus();
        
        try
        {
            universalTransport = (MuleUniversalTransport) getBus().getExtension(
                DestinationFactoryManager.class).getDestinationFactory(MuleUniversalTransport.TRANSPORT_ID);
        }
        catch (BusException e)
        {
            throw new ConfigurationException(e);
        }
    }


    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(eventContext);
        }

        // if http request
        String request = eventContext.getMessage().getStringProperty(HttpConnector.HTTP_REQUEST_PROPERTY,
            StringUtils.EMPTY);
        String uri = eventContext.getEndpointURI().toString();

        if (request.indexOf('?') > -1 || uri.indexOf('?') > -1)
        {
            return generateWSDLOrXSD(eventContext, request, uri);
        }
        else
        {
            return sendToDestination(eventContext, uri);
        }
    }

    protected Object generateWSDLOrXSD(MuleEventContext eventContext, String req, String uri)
        throws EndpointNotFoundException, IOException
    {
        
        // TODO: Is there a way to make this not so ugly?
        String ctxUri;
        String uriBase = null;
        
        EndpointURI epUri = eventContext.getEndpointURI();
        String host = (String) eventContext.getMessage().getProperty("Host", epUri.getHost());
        
        // This is the case of the HTTP message receiver. The servlet one sends different info
        if (req != null && req.length() > 0) {
            
            uriBase = epUri.getScheme() + "://" + host + epUri.getPath();
            int qIdx = uriBase.indexOf('?');
            if (qIdx > -1) {
                uriBase = uriBase.substring(0, qIdx);
            }
            
            qIdx = req.indexOf('?');
            if (qIdx > -1) {
                req = req.substring(qIdx);
            }
            
            qIdx = req.indexOf('&');
            if (qIdx > -1) {
                req = req.substring(0, qIdx);
            }
            
            uri = uriBase + req;
        }
        
        ctxUri = eventContext.getEndpointURI().getPath();
        
        EndpointInfo ei = receiver.getServer().getEndpoint().getEndpointInfo();

        if (uriBase != null) {
            ei.setAddress(uriBase);
            
            if (ei.getExtensor(AddressType.class) != null) {
                ei.getExtensor(AddressType.class).setLocation(uriBase);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String ct = null;

        for (QueryHandler qh : bus.getExtension(QueryHandlerRegistry.class).getHandlers())
        {
            if (qh.isRecognizedQuery(uri, ctxUri, ei))
            {
                ct = qh.getResponseContentType(uri, ctxUri);
                qh.writeResponse(uri, ctxUri, ei, out);
                out.flush();
            }
        }

        String msg;
        if (ct == null)
        {
            ct = "text/plain";
            msg = "No query handler found for URL.";
        }
        else
        {
            msg = out.toString();
        }

        MuleMessage result = new DefaultMuleMessage(msg);
        result.setProperty(HttpConstants.HEADER_CONTENT_TYPE, ct);

        return result;
    }

    protected Object sendToDestination(MuleEventContext ctx, String uri) throws MuleException, IOException
    {
        try
        {
            MessageImpl m = new MessageImpl();
            MuleMessage muleMsg = ctx.getMessage();
            String method = (String) muleMsg.getProperty(HttpConnector.HTTP_METHOD_PROPERTY);
            String path = (String) muleMsg.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
            if (path == null) 
            {
                path = "";
            }
            
            if (method != null) 
            {
                m.put(Message.HTTP_REQUEST_METHOD, method);
                m.put(Message.PATH_INFO, path);
                m.put(Message.BASE_PATH, ctx.getEndpointURI().getPath());
                
                if (!"GET".equals(method.toUpperCase())) 
                {
                    Object payload = ctx.transformMessage();

                    if (payload instanceof InputStream)
                    {
                        m.put(Message.ENCODING, ctx.getEncoding());
                        m.setContent(InputStream.class, payload);
                    }
                    else if (payload instanceof Reader)
                    {
                        m.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader((Reader) payload));
                    }
                    else if (payload instanceof byte[])
                    {
                        m.setContent(InputStream.class, new ByteArrayInputStream((byte[]) payload));
                    }
                    else
                    {
                        InputStream is = (InputStream) ctx.transformMessage(InputStream.class);
                        m.put(Message.ENCODING, ctx.getEncoding());
                        m.setContent(InputStream.class, is);
                    }
                }
            }
            
            // TODO: Not sure if this is 100% correct - DBD
            String soapAction = getSoapAction(ctx.getMessage());
            m.put(org.mule.transport.soap.SoapConstants.SOAP_ACTION_PROPERTY_CAPS, soapAction);

            EndpointInfo ei = receiver.getServer().getEndpoint().getEndpointInfo();

            MuleUniversalDestination d = (MuleUniversalDestination) universalTransport.getDestination(ei);
            if (d.getMessageObserver() == null)
            {
                // TODO is this the right Mule exception?
                throw new EndpointNotFoundException(uri);
            }

            // Set up a listener for the response
            ResponseListener obs = new ResponseListener();
            m.put(MuleUniversalDestination.RESPONSE_OBSERVER, obs);

            m.put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
            m.setDestination(d);
            d.getMessageObserver().onMessage(m);

            // TODO: Make this streaming...
            Object result = obs.getCachedStream().getInputStream();

            // Handle a fault if there is one.
            Message resMsg = obs.getMessage();
            Exception ex = resMsg.getContent(Exception.class);
            if (ex != null)
            {
                ExceptionPayload exceptionPayload = new DefaultExceptionPayload(new Exception(result.toString()));
                ctx.getMessage().setExceptionPayload(exceptionPayload);
            }

            return result;
        }
        catch (MuleException e)
        {
            logger.warn("Could not dispatch message to XFire!", e);
            throw e;
        }
    }

    /**
     * Gets the stream representation of the current message. If the message is set
     * for streaming the input stream on the UMOStreamMEssageAdapter will be used,
     * otherwise a byteArrayInputStream will be used to hold the byte[]
     * representation of the current message.
     * 
     * @param context the event context
     * @return The inputstream for the current message
     * @throws MuleException
     */

    protected InputStream getMessageStream(MuleEventContext context) throws MuleException
    {
        InputStream is;
        Object eventMsgPayload = context.transformMessage();

        if (eventMsgPayload instanceof InputStream)
        {
            is = (InputStream) eventMsgPayload;
        }
        else
        {
            is = (InputStream) context.transformMessage(InputStream.class);
        }
        return is;
    }

    protected String getSoapAction(MuleMessage message)
    {
        String action = (String) message.getProperty(SoapConstants.SOAP_ACTION_PROPERTY);

        if (action != null && action.startsWith("\"") && action.endsWith("\"") && action.length() >= 2)
        {
            action = action.substring(1, action.length() - 1);
        }

        return action;
    }

    public Bus getBus()
    {
        return bus;
    }

    public void setBus(Bus bus)
    {
        this.bus = bus;
    }

    class ResponseListener implements MessageObserver
    {
        private Message message;

        public CachedOutputStream getCachedStream()
        {
            return message.getContent(CachedOutputStream.class);
        }

        public Message getMessage()
        {
            return message;
        }

        public synchronized void onMessage(Message message)
        {
            this.message = message;
        }
    }

    public void initialise() throws InitialisationException
    {
        if (bus == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("No Cxf bus instance, this component has not been initialized properly."), this);
        }
    }

    public void start() throws MuleException
    {
        // nothing to do
    }
    
    public void stop() throws MuleException
    {
        // nothing to do
    }

    public void dispose()
    {
        // template method
    }
}
