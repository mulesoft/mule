/*
 * $Id: XFireServiceComponent.java 6617 2007-05-20 20:24:44Z hasari $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.config.ConfigurationException;
import org.mule.impl.MuleMessage;
import org.mule.impl.message.ExceptionPayload;
import org.mule.providers.cxf.transport.MuleUniversalDestination;
import org.mule.providers.cxf.transport.MuleUniversalTransport;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.soap.SoapConstants;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.EndpointNotFoundException;
import org.mule.umo.lifecycle.Callable;
import org.mule.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

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

/**
 * The CXF receives messages from Mule, converts them into CXF messages and dispatches
 * them into the receiving CXF destination.
 */
public class CxfServiceComponent implements Callable
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


    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(eventContext);
        }

        // if http request
        String request = eventContext.getMessage().getStringProperty(HttpConnector.HTTP_REQUEST_PROPERTY,
            StringUtils.EMPTY);

        Properties params = eventContext.getEndpointURI().getParams();
        if (params.size() > 0
            || request.toLowerCase().endsWith(org.mule.providers.soap.SoapConstants.WSDL_PROPERTY))
        {

            return generateWSDLOrXSD(eventContext, request, params);
        }
        else
        {
            String uri = eventContext.getEndpointURI().toString();

            return sendToDestination(eventContext, uri);
        }
    }

    private Object generateWSDLOrXSD(UMOEventContext eventContext, String req, Properties params)
        throws EndpointNotFoundException, IOException
    {
        // TODO: we need to handle ?xsd too, but the request URL from Mule looks
        // funny,
        // so I'm not sure about how this code should work
        String uri = eventContext.getEndpointURI().toString();
        if (!uri.contains("?"))
        {
            uri += "?wsdl";
        }
        String ctxUri = eventContext.getEndpointURI().getPath();

        EndpointInfo ei = receiver.getServer().getEndpoint().getEndpointInfo();

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

        UMOMessage result = new MuleMessage(msg);
        result.setProperty(HttpConstants.HEADER_CONTENT_TYPE, ct);

        return result;
    }

    Object sendToDestination(UMOEventContext ctx, String uri) throws UMOException, IOException
    {
        try
        {
            MessageImpl m = new MessageImpl();

            Object payload = ctx.getMessage().getPayload();

            if (payload instanceof InputStream)
            {
                m.put(Message.ENCODING, ctx.getEncoding());
                m.setContent(InputStream.class, (InputStream) payload);
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
                String text = ctx.getTransformedMessageAsString(ctx.getEncoding());
                StringReader strReader = new StringReader(text);
                m.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader(strReader));
            }

            // TODO: Not sure if this is 100% correct - DBD
            String soapAction = getSoapAction(ctx.getMessage());
            m.put("SOAPAction", soapAction);

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
            Object result = obs.getCachedStream().getOut().toString();

            // Handle a fault if there is one.
            Message resMsg = obs.getMessage();
            Exception ex = resMsg.getContent(Exception.class);
            if (ex != null)
            {
                ExceptionPayload exceptionPayload = new ExceptionPayload(new Exception(result.toString()));
                ctx.getMessage().setExceptionPayload(exceptionPayload);
            }

            return result;
        }
        catch (UMOException e)
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
     * @throws UMOException
     */

    protected InputStream getMessageStream(UMOEventContext context) throws UMOException
    {
        InputStream is;
        Object eventMsgPayload = context.getTransformedMessage();

        if (eventMsgPayload instanceof InputStream)
        {
            is = (InputStream) eventMsgPayload;
        }
        else
        {
            is = (InputStream) context.getTransformedMessage(InputStream.class);
        }
        return is;
    }

    private String getSoapAction(UMOMessage message)
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
}
