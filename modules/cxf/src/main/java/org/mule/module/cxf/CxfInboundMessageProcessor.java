/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.module.cxf.HttpRequestPropertyManager.getBasePath;
import static org.mule.module.cxf.HttpRequestPropertyManager.getRequestPath;
import static org.mule.module.cxf.HttpRequestPropertyManager.getScheme;
import static org.mule.module.cxf.support.CxfUtils.clearClientContextIfNeeded;
import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_CLIENT_SSL_SESSION;
import static org.mule.transport.http.HttpConnector.HTTP_STATUS_PROPERTY;
import static org.mule.transport.http.HttpConstants.HEADER_CONTENT_TYPE;
import static org.mule.transport.http.HttpConstants.SC_ACCEPTED;
import static org.mule.transport.http.HttpConstants.SC_INTERNAL_SERVER_ERROR;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLSession;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.jms.interceptor.SoapJMSConstants;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.wsdl.http.AddressType;
import org.mule.DefaultMuleEvent;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NonBlockingSupported;
import org.mule.api.endpoint.EndpointNotFoundException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.NonBlockingReplyToHandler;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.MessageFactory;
import org.mule.message.DefaultExceptionPayload;
import org.mule.module.cxf.support.DelegatingOutputStream;
import org.mule.module.cxf.transport.MuleUniversalDestination;
import org.mule.module.http.internal.listener.properties.SSLSessionProperties;
import org.mule.module.xml.stax.StaxSource;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * The CxfInboundMessageProcessor performs inbound CXF processing, sending an event
 * through the CXF service, then on to the next MessageProcessor. This processor gets
 * built by a MessageProcessorBuilder which is responsible for configuring it and the
 * {@link Server} that it dispatches to.
 */
public class CxfInboundMessageProcessor extends AbstractInterceptingMessageProcessor implements Lifecycle, NonBlockingSupported
{

    public static final String JMS_TRANSPORT = "jms";

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected Bus bus;

    // manager to the component
    protected String transportClass;

    protected Server server;

    private boolean proxy;

    private QueryHandler wsdlQueryHandler;

    private String mimeType;

    @Override
    public void initialise() throws InitialisationException
    {
        if (bus == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("No CXF bus instance, this component has not been initialized properly."),
                this);
        }
    }

    @Override
    public void stop() throws MuleException
    {
        if (server != null)
        {
            server.stop();
        }
    }

    @Override
    public void start() throws MuleException
    {
        // Start the CXF Server
        if (server != null)
        {
            server.start();
        }
    }

    @Override
    public void dispose()
    {
        // nothing to do
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        // if http request
        String requestPath = parseHttpRequestProperty(getRequestPath(event.getMessage()));
        try
        {
            if (requestPath.indexOf('?') > -1)
            {
                return generateWSDLOrXSD(event, requestPath);
            }
            else
            {
                return sendToDestination(event);
            }
        }
        catch (IOException e)
        {
            throw new DefaultMuleException(e);
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

    protected MuleEvent generateWSDLOrXSD(MuleEvent event, String req)
        throws EndpointNotFoundException, IOException
    {
        // TODO: Is there a way to make this not so ugly?
        String ctxUri = event.getMessage().getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);
        String wsdlUri = getUri(event);
        String serviceUri = wsdlUri.substring(0, wsdlUri.indexOf('?'));

        EndpointInfo ei = getServer().getEndpoint().getEndpointInfo();

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

        if (wsdlQueryHandler.isRecognizedQuery(wsdlUri, ctxUri, ei))
        {
            ct = wsdlQueryHandler.getResponseContentType(wsdlUri, ctxUri);
            wsdlQueryHandler.writeResponse(wsdlUri, ctxUri, ei, out);
            out.flush();
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

        event.getMessage().setPayload(msg, DataTypeFactory.XML_STRING);
        event.getMessage().setOutboundProperty(HEADER_CONTENT_TYPE, ct);
        return event;
    }

    private String getUri(MuleEvent event)
    {
        String scheme = getScheme(event);
        String host = event.getMessage().getInboundProperty("Host");
        String ctx = getRequestPath(event.getMessage());

        return scheme + "://" + host + ctx;
    }

    protected MuleEvent sendToDestination(MuleEvent event) throws MuleException, IOException
    {
        try
        {
            final Exchange exchange = new ExchangeImpl();

            final MuleEvent originalEvent = event;
            if (event.isAllowNonBlocking())
            {
                final ReplyToHandler originalReplyToHandler = event.getReplyToHandler();

                event = new DefaultMuleEvent(event, new NonBlockingReplyToHandler()
                {
                    @Override
                    public void processReplyTo(MuleEvent responseEvent, MuleMessage returnMessage, Object replyTo) throws MuleException
                    {
                        try
                        {
                            // CXF execution chain was suspended, so we need to resume it.
                            // The MuleInvoker component will be recalled, by using the CxfConstants.NON_BLOCKING_RESPONSE flag we force using the received response event instead of re-invoke the flow
                            CountDownLatch countDownLatch = (CountDownLatch) exchange.get(CxfConstants.NON_BLOCKING_LATCH);
                            countDownLatch.await(1000, SECONDS);
                            exchange.put(CxfConstants.MULE_EVENT, responseEvent);
                            exchange.put(CxfConstants.NON_BLOCKING_RESPONSE, true);
                            exchange.getInMessage().getInterceptorChain().resume();

                            // Process the response
                            responseEvent = (MuleEvent) exchange.get(CxfConstants.MULE_EVENT);
                            responseEvent = processResponse(originalEvent, exchange, responseEvent);

                            // Continue the non blocking execution
                            originalReplyToHandler.processReplyTo(responseEvent, responseEvent.getMessage(), replyTo);
                        }
                        catch (Exception e)
                        {
                            ExceptionPayload exceptionPayload = new DefaultExceptionPayload(e);
                            responseEvent.getMessage().setExceptionPayload(exceptionPayload);
                            returnMessage.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 500);
                            responseEvent.setMessage(returnMessage);
                            processExceptionReplyTo(new MessagingException(responseEvent, e, CxfInboundMessageProcessor.this), replyTo);
                        }
                        finally
                        {
                            exchange.put(CxfConstants.NON_BLOCKING_LATCH, new CountDownLatch(1));
                        }
                    }

                    @Override
                    public void processExceptionReplyTo(MessagingException exception, Object replyTo)
                    {
                        originalReplyToHandler.processExceptionReplyTo(exception, replyTo);
                    }
                });
                // Update RequestContext ThreadLocal for backwards compatibility
                OptimizedRequestContext.unsafeSetEvent(event);
            }

            MuleEvent responseEvent = sendThroughCxf(event, exchange);

            if (responseEvent == null || !responseEvent.equals(NonBlockingVoidMuleEvent.getInstance()))
            {
                return processResponse(event, exchange, responseEvent);
            }

            return responseEvent;
        }
        catch (MuleException e)
        {
            logger.warn("Could not dispatch message to CXF!", e);
            throw e;
        }
    }

    private MuleEvent sendThroughCxf(MuleEvent event, Exchange exchange) throws TransformerException, IOException
    {
        final MessageImpl m = new MessageImpl();
        m.setExchange(exchange);
        final MuleMessage muleReqMsg = event.getMessage();
        
        if (muleReqMsg.getInboundProperty(HTTP_CLIENT_SSL_SESSION) != null)
        {
            SSLSessionProperties sessionProperties = muleReqMsg.getInboundProperty(HTTP_CLIENT_SSL_SESSION); 
            SSLSession session = sessionProperties.retrieveSession();
            TLSSessionInfo tlsinfo = new TLSSessionInfo(session.getCipherSuite(), session, session.getLocalCertificates());
            m.put(TLSSessionInfo.class, tlsinfo);
        }
        String method = muleReqMsg.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY);

        String ct = muleReqMsg.getInboundProperty(HEADER_CONTENT_TYPE);
        if (ct != null)
        {
            m.put(Message.CONTENT_TYPE, ct);
        }

        String path = muleReqMsg.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);
        if (path == null)
        {
            path = "";
        }

        if (method != null)
        {
            m.put(Message.HTTP_REQUEST_METHOD, method);
            m.put(Message.PATH_INFO, path);
            String basePath = getBasePath(muleReqMsg);
            m.put(Message.BASE_PATH, basePath);

            method = method.toUpperCase();
        }

        if (!"GET".equals(method))
        {
            Object payload = event.getMessage().getPayload();

            setPayload(event, m, payload);
        }

        // TODO: Not sure if this is 100% correct - DBD
        String soapAction = getSoapAction(event.getMessage());
        m.put(SoapConstants.SOAP_ACTION_PROPERTY_CAPS, soapAction);

        org.apache.cxf.transport.Destination d;

        if (server != null)
        {
            d = server.getDestination();
        }
        else
        {
            String serviceUri = getUri(event);

            DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
            DestinationFactory df = dfm.getDestinationFactoryForUri(serviceUri);

            EndpointInfo ei = new EndpointInfo();
            ei.setAddress(serviceUri);
            d = df.getDestination(ei);
        }

        // For MULE-6829
        if (shouldSoapActionHeader())
        {
            // Add protocol headers with the soap action so that the SoapActionInInterceptor can find them if it is soap v1.1
            Map<String, List<String>> protocolHeaders = new HashMap<String, List<String>>();
            if (soapAction != null && !soapAction.isEmpty())
            {
                List<String> soapActions = new ArrayList<String>();
                // An HTTP client MUST use [SOAPAction] header field when issuing a SOAP HTTP Request.
                // The header field value of empty string ("") means that the intent of the SOAP message is provided by the HTTP Request-URI.
                // No value means that there is no indication of the intent of the message.
                soapActions.add(soapAction);
                protocolHeaders.put(SoapBindingConstants.SOAP_ACTION, soapActions);
            }

            String eventRequestUri = event.getMessageSourceURI().toString();
            if (eventRequestUri.startsWith(JMS_TRANSPORT) || SoapJMSConstants.SOAP_JMS_NAMESPACE.equals(((MuleUniversalDestination) d).getEndpointInfo().getTransportId()))
            {
                String contentType = muleReqMsg.getInboundProperty(SoapJMSConstants.CONTENTTYPE_FIELD);
                if (contentType == null)
                {
                    contentType = "text/xml";
                }
                protocolHeaders.put(SoapJMSConstants.CONTENTTYPE_FIELD, Collections.singletonList(contentType));

                String requestUri = muleReqMsg.getInboundProperty(SoapJMSConstants.REQUESTURI_FIELD);
                if (requestUri == null)
                {
                    requestUri = eventRequestUri;
                }
                protocolHeaders.put(SoapJMSConstants.REQUESTURI_FIELD, Collections.singletonList(requestUri));
            }

            m.put(Message.PROTOCOL_HEADERS, protocolHeaders);
        }

        // Set up a listener for the response
        m.put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        m.setDestination(d);

        // mule will close the stream so don't let cxf, otherwise cxf will close it too early
        exchange.put(StaxInEndingInterceptor.STAX_IN_NOCLOSE, Boolean.TRUE);
        exchange.setInMessage(m);

        // if there is a fault, then we need an event in here because we won't
        // have a responseEvent from the MuleInvoker
        exchange.put(CxfConstants.MULE_EVENT, event);

        // invoke the actual web service up until right before we serialize the
        // response
        try
        {
            d.getMessageObserver().onMessage(m);
        }
        catch (SuspendedInvocationException e)
        {
            MuleEvent responseEvent = (MuleEvent) exchange.get(CxfConstants.MULE_EVENT);

            if (responseEvent == null || !responseEvent.equals(NonBlockingVoidMuleEvent.getInstance()))
            {
                throw e;
            }
            
            CountDownLatch nonBlockingLatch = (CountDownLatch) exchange.get(CxfConstants.NON_BLOCKING_LATCH);
            nonBlockingLatch.countDown();
        }
        finally
        {
            clearClientContextIfNeeded(d.getMessageObserver());
        }

        // get the response event
        return (MuleEvent) exchange.get(CxfConstants.MULE_EVENT);
    }

    private MuleEvent processResponse(MuleEvent event, Exchange exchange, MuleEvent responseEvent)
    {
        // If there isn't one, there was probably a fault, so use the original event
        if (responseEvent == null || VoidMuleEvent.getInstance().equals(responseEvent) || !event.getExchangePattern().hasResponse())
        {
            return null;
        }

        MuleMessage muleResMsg = responseEvent.getMessage();

        BindingOperationInfo binding = exchange.get(BindingOperationInfo.class);
        if (null != binding && null != binding.getOperationInfo() && binding.getOperationInfo().isOneWay())
        {
            // For one-way operations, no envelope should be returned
            // (http://www.w3.org/TR/soap12-part2/#http-reqbindwaitstate)
            muleResMsg.setOutboundProperty(HTTP_STATUS_PROPERTY, SC_ACCEPTED);
            muleResMsg.setPayload(NullPayload.getInstance());
        }
        else
        {
            muleResMsg.setPayload(getResponseOutputHandler(exchange), DataTypeFactory.XML_STRING);
        }

        // Handle a fault if there is one.
        Message faultMsg = exchange.getOutFaultMessage();
        if (faultMsg != null)
        {
            if (null != binding && null != binding.getOperationInfo() && binding.getOperationInfo().isOneWay())
            {
                muleResMsg.setPayload(getResponseOutputHandler(exchange), DataTypeFactory.XML_STRING);
            }
            Exception ex = faultMsg.getContent(Exception.class);
            if (ex != null)
            {
                ExceptionPayload exceptionPayload = new DefaultExceptionPayload(ex);
                event.getMessage().setExceptionPayload(exceptionPayload);
                muleResMsg.setOutboundProperty(HTTP_STATUS_PROPERTY, SC_INTERNAL_SERVER_ERROR);
            }
        }

        return responseEvent;
    }

    protected boolean shouldSoapActionHeader()
    {
        // Only add soap headers if we can validate the bindings. if not, cxf will throw a fault in SoapActionInInterceptor
        // we cannot validate the bindings if we're using mule's pass-through invoke proxy service 
        boolean isGenericProxy = "http://support.cxf.module.mule.org/"
                .equals(getServer().getEndpoint().getService().getName().getNamespaceURI()) && 
                "ProxyService".equals(getServer().getEndpoint().getService().getName().getLocalPart());
        return !isGenericProxy;
    }

    @Override
    public MuleEvent processNext(MuleEvent event) throws MuleException
    {
        return super.processNext(event);
    }

    @Deprecated
    protected OutputHandler getRessponseOutputHandler(final MessageImpl m)
    {
        return getResponseOutputHandler(m);
    }
    protected OutputHandler getResponseOutputHandler(final MessageImpl m)
    {
        return getResponseOutputHandler(m.getExchange());
    }

    protected OutputHandler getResponseOutputHandler(final Exchange exchange)
    {
        OutputHandler outputHandler = new OutputHandler()
        {
            @Override
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                Message outFaultMessage = exchange.getOutFaultMessage();
                Message outMessage = exchange.getOutMessage();

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

                DelegatingOutputStream delegate = contentMsg.getContent(DelegatingOutputStream.class);
                if (delegate.getOutputStream() instanceof ByteArrayOutputStream)
                {
                    out.write(((ByteArrayOutputStream) delegate.getOutputStream()).toByteArray());
                }
                delegate.setOutputStream(out);

                out.flush();

                contentMsg.getInterceptorChain().resume();
            }

        };
        return outputHandler;
    }

    private void setPayload(MuleEvent ctx, final MessageImpl m, Object payload) throws TransformerException
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
            m.setContent(XMLStreamReader.class, payload);
        }
        else if (payload instanceof Document)
        {
            DOMSource source = new DOMSource((Node) payload);
            m.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader(source));
        }
        else
        {
            InputStream is = ctx.transformMessage(DataTypeFactory.create(InputStream.class));
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
    protected InputStream getMessageStream(MuleEvent context) throws MuleException
    {
        InputStream is;
        Object eventMsgPayload = context.getMessage().getPayload();

        if (eventMsgPayload instanceof InputStream)
        {
            is = (InputStream) eventMsgPayload;
        }
        else
        {
            is = context.transformMessage(DataTypeFactory.create(InputStream.class));
        }
        return is;
    }

    protected String getSoapAction(MuleMessage message)
    {
        String action = message.getInboundProperty(SoapConstants.SOAP_ACTION_PROPERTY);

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

    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }

    public void setProxy(boolean proxy)
    {
        this.proxy = proxy;
    }

    public boolean isProxy()
    {
        return proxy;
    }

    public void setWSDLQueryHandler(QueryHandler wsdlQueryHandler)
    {
        this.wsdlQueryHandler = wsdlQueryHandler;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }
}
