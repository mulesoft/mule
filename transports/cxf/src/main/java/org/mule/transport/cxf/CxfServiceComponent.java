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
import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointNotFoundException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.config.i18n.MessageFactory;
import org.mule.message.DefaultExceptionPayload;
import org.mule.module.xml.stax.StaxSource;
import org.mule.transport.cxf.support.DelegatingOutputStream;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.soap.SoapConstants;
import org.mule.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.http.AddressType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
    protected String transportClass;

    private CxfMessageReceiver receiver;

    public CxfServiceComponent(CxfConnector connector,
                               CxfMessageReceiver receiver) throws ConfigurationException
    {
        super();
        this.receiver = receiver;
        this.bus = receiver.connector.getCxfBus();
    }

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(eventContext);
        }

        // if http request
        String requestPath = parseHttpRequestProperty(
            eventContext.getMessage().getStringProperty(HttpConnector.HTTP_REQUEST_PROPERTY,
                StringUtils.EMPTY));
        

        if (requestPath.indexOf('?') > -1)
        {
            return generateWSDLOrXSD(eventContext, requestPath);
        }
        else
        {
            return sendToDestination(eventContext);
        }
    }

    private String parseHttpRequestProperty(String request)
    {
        String uriBase = "";
        
        if (!(request.contains("?wsdl")) && (!(request.contains("?xsd"))))
        {
            int qIdx = request.indexOf('?');
            if (qIdx > -1)
            {
                uriBase = request.substring(0, qIdx);
            }
        }
        else
        {
            uriBase = request;
        }
        
        return uriBase;
    }
    
    protected Object generateWSDLOrXSD(MuleEventContext eventContext, String req)
        throws EndpointNotFoundException, IOException
    {
        // TODO: Is there a way to make this not so ugly?       
        String ctxUri = (String) eventContext.getMessage().getProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
        String wsdlUri = getWsdlUri(eventContext, req);
        String serviceUri = wsdlUri.substring(0, wsdlUri.indexOf('?'));
        
        EndpointInfo ei = receiver.getServer().getEndpoint().getEndpointInfo();

        if (serviceUri != null) 
        {
            ei.setAddress(serviceUri);
            
            if (ei.getExtensor(AddressType.class) != null) 
            {
                ei.getExtensor(AddressType.class).setLocation(serviceUri);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String ct = null;

        for (QueryHandler qh : bus.getExtension(QueryHandlerRegistry.class).getHandlers())
        {
            if (qh.isRecognizedQuery(wsdlUri, ctxUri, ei))
            {
                ct = qh.getResponseContentType(wsdlUri, ctxUri);
                qh.writeResponse(wsdlUri, ctxUri, ei, out);
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

    private String getWsdlUri(MuleEventContext eventContext, String reqPath) 
    {
        EndpointURI epUri = eventContext.getEndpointURI();
        String host = (String) eventContext.getMessage().getProperty("Host", epUri.getHost());
        String ctx = (String) eventContext.getMessage().getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        return epUri.getScheme() + "://" + host + ctx;
    }
    
    protected Object sendToDestination(MuleEventContext ctx) throws MuleException, IOException
    {
        try
        {
            final MessageImpl m = new MessageImpl();
            final MuleMessage muleReqMsg = ctx.getMessage();
            String method = (String) muleReqMsg.getProperty(HttpConnector.HTTP_METHOD_PROPERTY);
            
            String ct = (String) muleReqMsg.getProperty(HttpConstants.HEADER_CONTENT_TYPE);
            if (ct != null) 
            {
                m.put(Message.CONTENT_TYPE, ct);
            }
            
            String path = (String) muleReqMsg.getProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);
            if (path == null) 
            {
                path = "";
            }
            
            if (method != null) 
            {
                m.put(Message.HTTP_REQUEST_METHOD, method);
                m.put(Message.PATH_INFO, path);
                Object basePath = muleReqMsg.getProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
                m.put(Message.BASE_PATH, basePath);
                
                method = method.toUpperCase();
            }
            
            if (!"GET".equals(method)) 
            {
                Object payload = ctx.transformMessage();

                setPayload(ctx, m, payload);
            }
            
            // TODO: Not sure if this is 100% correct - DBD
            String soapAction = getSoapAction(ctx.getMessage());
            m.put(org.mule.transport.soap.SoapConstants.SOAP_ACTION_PROPERTY_CAPS, soapAction);

            Server server = receiver.getServer();
            org.apache.cxf.transport.Destination d = server.getDestination();
            
            // Set up a listener for the response
            m.put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
            m.put(MuleProperties.MULE_EVENT_PROPERTY, RequestContext.getEvent());
            m.setDestination(d);
            
            OutputHandler outputHandler = new OutputHandler() 
            {
                public void write(MuleEvent event, OutputStream out) throws IOException
                {
                    Message outFaultMessage = m.getExchange().getOutFaultMessage();
                    Message outMessage = m.getExchange().getOutMessage();
                    
                    Message contentMsg = null;
                    if (outFaultMessage != null && outFaultMessage.getContent(OutputStream.class) != null) 
                    {
                        contentMsg = outFaultMessage;
                    } 
                    else if (outMessage != null) 
                    {
                        contentMsg = outMessage;
                    }
                    
                    if (contentMsg == null)
                    {
                        return;
                    }
                    
                    DelegatingOutputStream delegate = (DelegatingOutputStream) contentMsg.getContent(DelegatingOutputStream.class);
                    out.write(((ByteArrayOutputStream) delegate.getOutputStream()).toByteArray());
                    delegate.setOutputStream(out);
                    
                    out.flush();
                    
                    contentMsg.getInterceptorChain().resume();
                }
                
            };
            DefaultMuleMessage muleResMsg = new DefaultMuleMessage(outputHandler);
            
            ExchangeImpl exchange = new ExchangeImpl();
            exchange.setInMessage(m);
            m.put(CxfConstants.MULE_MESSAGE, muleReqMsg);
            
            exchange.put(CxfConstants.MULE_MESSAGE, muleResMsg);
            
            // invoke the actual web service up until right before we serialize the response
            d.getMessageObserver().onMessage(m);
            
            // Handle a fault if there is one.
            Message faultMsg = m.getExchange().getOutFaultMessage();
            if (faultMsg != null)
            {
                Exception ex = faultMsg.getContent(Exception.class);
                if (ex != null)
                {
                    ExceptionPayload exceptionPayload = new DefaultExceptionPayload(new Exception(""));
                    ctx.getMessage().setExceptionPayload(exceptionPayload);
                }
            }
            
            muleResMsg.setProperty(MuleProperties.MULE_REPLY_TO_STOP_PROPERTY, "true");
            return muleResMsg;
        }
        catch (MuleException e)
        {
            logger.warn("Could not dispatch message to CXF!", e);
            throw e;
        }
    }


    private void setPayload(MuleEventContext ctx, final MessageImpl m, Object payload)
        throws TransformerException
    {
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
        else if (payload instanceof StaxSource)
        {
            m.setContent(XMLStreamReader.class, ((StaxSource) payload).getXMLStreamReader());
        }
        else if (payload instanceof Source)
        {
            m.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader((Source) payload));
        }
        else if (payload instanceof XMLStreamReader)
        {
            m.setContent(XMLStreamReader.class, (XMLStreamReader) payload);
        }
        else if (payload instanceof Document)
        {
            DOMSource source = new DOMSource((Node) payload);
            m.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader(source));
        }
        else
        {
            InputStream is = (InputStream) ctx.transformMessage(InputStream.class);
            m.put(Message.ENCODING, ctx.getEncoding());
            m.setContent(InputStream.class, is);
        }
    }

    /**
     * Gets the stream representation of the current message.
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
