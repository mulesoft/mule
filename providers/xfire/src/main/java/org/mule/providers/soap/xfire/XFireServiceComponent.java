/*
 * $Id$
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

package org.mule.providers.soap.xfire;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.attachments.Attachments;
import org.codehaus.xfire.attachments.JavaMailAttachments;
import org.codehaus.xfire.exchange.InMessage;
import org.codehaus.xfire.exchange.MessageExchange;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.ServiceRegistry;
import org.codehaus.xfire.soap.SoapConstants;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.Transport;
import org.codehaus.xfire.transport.TransportManager;
import org.codehaus.xfire.transport.http.HtmlServiceWriter;
import org.codehaus.xfire.util.STAXUtils;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.MuleRuntimeException;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.UMODescriptorAware;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.soap.xfire.transport.MuleLocalTransport;
import org.mule.providers.streaming.OutStreamMessageAdapter;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.manager.UMOWorkManager;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * The Xfire service component receives requests for Xfire services it manages
 * and marshalls requests and responses
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireServiceComponent implements Callable, Initialisable, Lifecycle, UMODescriptorAware
{

    public final String DEFAULT_CONTENT_TYPE = "text/html";

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected XFire xfire;

    // manager to the component
    protected Transport transport;

    public void setDescriptor(UMODescriptor descriptor) {
        UMOWorkManager wm = ((MuleDescriptor)descriptor).getThreadingProfile().createWorkManager("xfire-local-transport");
        try {
            wm.start();
        } catch (UMOException e) {
            throw new MuleRuntimeException(new Message(Messages.FAILED_TO_START_X, "local channel work manager", e));
        }
        transport = new MuleLocalTransport(wm);
        getTransportManager().register(transport);
    }

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        OutStreamMessageAdapter response;
        UMOEndpointURI endpointURI = eventContext.getEndpointURI();
        String method;

        if (eventContext.isStreaming()) {
            StreamMessageAdapter sma = (StreamMessageAdapter)eventContext.getMessage().getPayload();
            if (sma.getOutput() != null) {
                response = new OutStreamMessageAdapter(sma.getOutput());
            }
            else {
                response = new OutStreamMessageAdapter(new ByteArrayOutputStream());
            }
        }
        else {
            response = new OutStreamMessageAdapter(new ByteArrayOutputStream());
        }

        UMOMessage eventMsg = eventContext.getMessage();
        String endpointHeader = eventMsg.getStringProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, null);
        String request = eventMsg.getStringProperty(HttpConnector.HTTP_REQUEST_PROPERTY, null);
        //If a http request is set we can get the method from the request
        if(request!=null) {
            endpointURI = new MuleEndpointURI("soap:" + endpointURI.toString() + request);
        }

        method = endpointURI.getParams().getProperty(org.mule.providers.soap.SoapConstants.SOAP_METHOD_PROPERTY);

        if (method == null) {
            method = eventMsg.getStringProperty(org.mule.providers.soap.SoapConstants.SOAP_METHOD_PROPERTY, null);
        }
        if (method == null) {
            if (endpointHeader != null) {
                endpointURI = new MuleEndpointURI(endpointHeader);
                method = endpointURI.getParams().getProperty(org.mule.providers.soap.SoapConstants.SOAP_METHOD_PROPERTY);
            }
        }

        String serviceName = getService(eventContext);
        if(request==null) {
            request = endpointHeader;
        }

        if (request != null && request.endsWith("?wsdl")) {
            generateWSDL(response, serviceName);
        } else {

            if (method == null) {
                throw new MuleException(new Message(Messages.PROPERTIES_X_NOT_SET, "method"));
            }


            ServiceRegistry reg = getServiceRegistry();
            if (serviceName == null || serviceName.length() == 0 || !reg.hasService(serviceName)) {
                if (!reg.hasService(serviceName)) {
                    eventMsg.setProperty(HttpConnector.HTTP_STATUS_PROPERTY,
                            String.valueOf(HttpConstants.SC_NOT_FOUND));
                }

                generateServices(response);
                return response;
            }
            invoke(eventContext, endpointURI, response, serviceName, method);
        }
        // Todo currently defeating streaming
        return response.getPayloadAsBytes();
    }

    public void start() throws UMOException
    {
        // template method
    }

    public void stop() throws UMOException
    {
        // template method
    }

    public void initialise() throws InitialisationException, RecoverableException
    {
        if (xfire == null) {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "xfire"), this);
        }
    }

    public void dispose()
    {
        // template method
    }

    protected TransportManager getTransportManager()
    {
        return getXfire().getTransportManager();
    }

    protected void generateService(OutStreamMessageAdapter response, String serviceName)
            throws IOException, XMLStreamException
    {
        response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/html");
        Service endpoint = getServiceRegistry().getService(serviceName);
        HtmlServiceWriter writer = new HtmlServiceWriter();
        writer.write(response.getStream(), endpoint);
    }

    /**
     * @param response
     */
    protected void generateServices(OutStreamMessageAdapter response) throws IOException,
            XMLStreamException
    {
        response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/html");

        HtmlServiceWriter writer = new HtmlServiceWriter();
        writer.write(response.getStream(), getServiceRegistry().getServices());
    }

    /**
     * @param eventContext
     * @param response
     * @param service
     * @throws IOException
     * @throws java.io.UnsupportedEncodingException
     *
     */
    protected void invoke(UMOEventContext eventContext,
            UMOEndpointURI endpointURI,
            OutStreamMessageAdapter response,
            String service,
            String methodName) throws IOException, UnsupportedEncodingException, UMOException,
            javax.mail.MessagingException, NoSuchMethodException
    {

        MessageContext context = new MessageContext();
        context.setXFire(xfire);

        XFireMuleSession session = new XFireMuleSession(eventContext.getSession());
        context.setSession(session);
        context.setService(getService(service));

        OperationInfo op = context.getService().getServiceInfo().getOperation(methodName);
        if (op == null) {
            throw new NoSuchMethodException(methodName);
        }
        context.setExchange(new MessageExchange(context));
        context.getExchange().setOperation(op);

        Channel channel;
        String uri = endpointURI.toString();
        try {
            channel = transport.createChannel(uri);
        }
        catch (Exception e) {
            logger.debug("Couldn't open channel.", e);
            throw new MessagingException(new Message("xfire", 7, uri), eventContext.getMessage(), e);

        }

        UMOMessage eventMsg = eventContext.getMessage();

        String encoding = eventMsg.getStringProperty(HttpConstants.HEADER_CONTENT_ENCODING,
                MuleManager.getConfiguration().getEncoding());

        String contentType = eventMsg.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, null);

        if (contentType == null) {
            contentType = DEFAULT_CONTENT_TYPE + "; charset=" + encoding;
            logger.warn("Content-Type not set on request, defaulting to: " + contentType);
        }
        else if (contentType.indexOf("charset=") == -1) {
            contentType += "; charset=" + encoding;
        }

        if (contentType.toLowerCase().indexOf("multipart/related") != -1) {
            try {
                //Todo use  streamed attachments
                Attachments atts = new JavaMailAttachments(getMessageStream(eventContext), contentType);

                XMLStreamReader reader = STAXUtils.createXMLStreamReader(atts.getSoapMessage()
                        .getDataHandler().getInputStream(), encoding, context);
                InMessage message = new InMessage(reader, uri);
                message.setProperty(SoapConstants.SOAP_ACTION, eventMsg.getStringProperty(
                        SoapConstants.SOAP_ACTION, StringUtils.EMPTY));
                message.setAttachments(atts);

                context.getExchange().setInMessage(message);
                context.setCurrentMessage(message);
                context.setProperty(Channel.BACKCHANNEL_URI, response.getStream());
                channel.receive(context, message);
            }
            catch (javax.mail.MessagingException e) {
                throw new XFireRuntimeException("Couldn't parse request message.", e);
            }
        }
        else {
            XMLStreamReader reader = STAXUtils.createXMLStreamReader(getMessageStream(eventContext),
                    encoding, context);
            InMessage message = new InMessage(reader, uri);
            context.getExchange().setInMessage(message);
            context.setCurrentMessage(message);
            context.setProperty(Channel.BACKCHANNEL_URI, response.getStream());
            channel.receive(context, message);
        }
    }

    protected InputStream getMessageStream(UMOEventContext context) throws UMOException
    {
        InputStream is;
        UMOMessage eventMsg = context.getMessage();
        Object eventMsgPayload = eventMsg.getPayload();

        if (eventMsgPayload instanceof InputStream) {
            is = (InputStream)eventMsgPayload;
        }
        else if (eventMsg.getAdapter() instanceof StreamMessageAdapter) {
            StreamMessageAdapter sma = (StreamMessageAdapter)eventMsg.getAdapter();
            if (sma.getInput() != null) {
                is = sma.getInput();
            }
            else {
                is = sma.getResponse();
            }
        }
        else {
            is = new ByteArrayInputStream(context.getTransformedMessageAsBytes());
        }
        return is;
    }

    /**
     * @param response
     * @param service
     * @throws IOException
     */
    protected void generateWSDL(OutStreamMessageAdapter response, String service) throws IOException
    {
        response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");
        getXfire().generateWSDL(service, response.getStream());
    }

    /**
     * Get the service that is mapped to the specified request.
     */
    protected String getService(UMOEventContext context)
    {
        String pathInfo = context.getEndpointURI().getPath();

        if (StringUtils.isEmpty(pathInfo)) {
            return context.getEndpointURI().getHost();
        }

        String serviceName;

        int i = pathInfo.lastIndexOf("/");

        if (i > -1) {
            serviceName = pathInfo.substring(i + 1);
        }
        else {
            serviceName = pathInfo;
        }

        return serviceName;
    }

    protected Service getService(String name)
    {
        return getXfire().getServiceRegistry().getService(name);
    }

    public XFire getXfire()
    {
        return xfire;
    }

    public void setXfire(XFire xfire)
    {
        this.xfire = xfire;
    }

    public void setTransport(Transport transport)
    {
        this.transport = transport;
    }

    public ServiceRegistry getServiceRegistry()
    {
        return xfire.getServiceRegistry();
    }
}
