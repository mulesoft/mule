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
package org.mule.providers.soap.xfire;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.soap.SoapConstants;
import org.codehaus.xfire.attachments.JavaMailAttachments;
import org.codehaus.xfire.attachments.Attachments;
import org.codehaus.xfire.exchange.InMessage;
import org.codehaus.xfire.exchange.MessageExchange;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.ServiceRegistry;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.Transport;
import org.codehaus.xfire.transport.TransportManager;
import org.codehaus.xfire.transport.local.LocalTransport;
import org.codehaus.xfire.transport.local.LocalChannel;
import org.codehaus.xfire.transport.http.HtmlServiceWriter;
import org.codehaus.xfire.transport.http.XFireHttpSession;
import org.codehaus.xfire.transport.http.SoapHttpTransport;
import org.codehaus.xfire.util.STAXUtils;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.config.MuleProperties;
import org.mule.providers.WriterMessageAdapter;
import org.mule.providers.streaming.OutStreamMessageAdapter;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.providers.streaming.OutStreamMessageAdapter;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.soap.xfire.transport.MuleLocalTransport;
import org.mule.providers.soap.xfire.transport.MuleUniversalTransport;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.MuleManager;
import org.mule.MuleException;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.util.Utility;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * The Xfire service component recieves requests for Xfire services it manages and marshalls requests and responses
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireServiceComponent implements Callable, Initialisable, Lifecycle {

    public final String DEFAULT_CONTENT_TYPE = "text/html";

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected XFire xfire;

    //Todo Use MuleLocalTransport.  Need to find a clean way to supply a work manager to the component
    protected Transport transport = new LocalTransport();


    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        OutStreamMessageAdapter response = null;
        UMOEndpointURI endpointURI = null;
        String method = null;

        if (eventContext.isStreaming()) {
            StreamMessageAdapter sma = (StreamMessageAdapter) eventContext.getMessage().getPayload();
            if(sma.getOutput()!=null) {
                response = new OutStreamMessageAdapter(sma.getOutput());
            } else {
                response = new OutStreamMessageAdapter(new ByteArrayOutputStream());
            }
        } else {
            response = new OutStreamMessageAdapter(new ByteArrayOutputStream());
        }

        String endpointHeader = eventContext.getStringProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, null);

        if (eventContext.getEndpointURI().getScheme().startsWith("http")) {
            //We parse a new uri based on the listeneing host and port with the request parameters appended
            //Using the soap prefix ensures that we use a soap endpoint builder
            String uri = "soap:" + eventContext.getEndpointURI().toString();
            uri += eventContext.getStringProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
            endpointURI = new MuleEndpointURI(uri);
            method = endpointURI.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        } else {
            //If we're not using Http for the transport the method must be set as a property
            endpointURI = eventContext.getEndpointURI();
            method = eventContext.getStringProperty(MuleProperties.MULE_METHOD_PROPERTY);
            if (method == null) {
                if (endpointHeader != null) {
                    endpointURI = new MuleEndpointURI(endpointHeader);
                    method = endpointURI.getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY);
                }
            }
        }

        String serviceName = getService(eventContext);
        String request = eventContext.getStringProperty(MuleProperties.MULE_ENDPOINT_PROPERTY);
        
        if(request!=null && request.endsWith("?wsdl")) {
            generateWSDL(response, serviceName);
        }
        if (method == null) {
            throw new MuleException(new Message(Messages.PROPERTIES_X_NOT_SET, "method"));
        }

        ServiceRegistry reg = getServiceRegistry();
        if (serviceName == null || serviceName.length() == 0 || !reg.hasService(serviceName)) {
            if (!reg.hasService(serviceName)) {
                eventContext.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, String.valueOf(HttpConstants.SC_NOT_FOUND));
            }

            generateServices(response);
            return response;
        }
        invoke(eventContext, endpointURI, response, serviceName, method);

        //Todo currently defeating streaming
        return response.getPayloadAsBytes();
    }

    public void start() throws UMOException {

    }

    public void stop() throws UMOException {

    }

    public void initialise() throws InitialisationException, RecoverableException {

        if (xfire == null) {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "xfire"), this);
        }
    }

    public void dispose() {

    }

    protected void registerTransport() {
        TransportManager transportManager = getTransportManager();
        transportManager.register(transport);
    }


    protected TransportManager getTransportManager() {
        return getXfire().getTransportManager();
    }


    protected void generateService(OutStreamMessageAdapter response, String serviceName)
            throws IOException, XMLStreamException {
        response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/html");
        Service endpoint = getServiceRegistry().getService(serviceName);
        HtmlServiceWriter writer = new HtmlServiceWriter();
        writer.write(response.getStream(), endpoint);
    }


    /**
     * @param response
     */
    protected void generateServices(OutStreamMessageAdapter response) throws IOException, XMLStreamException {
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
                          String methodName)
            throws IOException, UnsupportedEncodingException, UMOException, javax.mail.MessagingException, NoSuchMethodException {

        MessageContext context = new MessageContext();
        context.setXFire(xfire);
        //todo Session handling
        // XFireHttpSession session = new XFireHttpSession(request);
        //context.setSession(session);
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
        } catch (Exception e) {
            logger.debug("Couldn't open channel.", e);
            throw new MessagingException(new Message("xfire", 7, uri), eventContext.getMessage(), e);

        }

        String contentType = eventContext.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE);
        String encoding = eventContext.getStringProperty(HttpConstants.HEADER_CONTENT_ENCODING);
        if (encoding == null) {
            encoding = MuleManager.getConfiguration().getEncoding();
        }
        if (contentType == null) {
            contentType = DEFAULT_CONTENT_TYPE + "; charset=" + encoding;
            logger.warn("Content-Type not set on request, defaulting to: " + contentType);
        } else if (contentType.indexOf("charset=") == -1) {
            contentType += "; charset=" + encoding;
        }

        if (contentType.toLowerCase().indexOf("multipart/related") != -1) {
            try {
                Attachments atts = new JavaMailAttachments(getMessageStream(eventContext), contentType);

                XMLStreamReader reader = STAXUtils.createXMLStreamReader(atts.getSoapMessage().getDataHandler().getInputStream(), encoding);
                InMessage message = new InMessage(reader, uri);
                message.setProperty(SoapConstants.SOAP_ACTION, eventContext.getStringProperty(SoapConstants.SOAP_ACTION, Utility.EMPTY_STRING));
                message.setAttachments(atts);

                context.getExchange().setInMessage(message);
                context.setCurrentMessage(message);
                context.setProperty(Channel.BACKCHANNEL_URI, response.getStream());
                channel.receive(context, message);
            } catch (javax.mail.MessagingException e) {
                throw new XFireRuntimeException("Couldn't parse request message.", e);
            }
        } else {
            XMLStreamReader reader = STAXUtils.createXMLStreamReader(getMessageStream(eventContext), encoding);
            InMessage message = new InMessage(reader, uri);
            context.getExchange().setInMessage(message);
            context.setCurrentMessage(message);
            context.setProperty(Channel.BACKCHANNEL_URI, response.getStream());
            channel.receive(context, message);
        }
    }

    protected InputStream getMessageStream(UMOEventContext context) throws UMOException {
        InputStream is = null;
        if (context.getMessage().getPayload() instanceof InputStream) {
            is = (InputStream) context.getMessage().getPayload();
        } else if (context.getMessage().getAdapter() instanceof StreamMessageAdapter) {
            StreamMessageAdapter sma = (StreamMessageAdapter) context.getMessage().getAdapter();
            if (sma.getInput() != null) {
                is = sma.getInput();
            } else {
                is = sma.getResponse();
            }
        } else {
            is = new ByteArrayInputStream(context.getMessageAsBytes());
        }
        return is;
    }

    /**
     * @param response
     * @param service
     * @throws IOException
     */
    protected void generateWSDL(OutStreamMessageAdapter response, String service)
            throws IOException {
        response.setProperty("Content-Type", "text/xml");
        getXfire().generateWSDL(service, response.getStream());
    }

    /**
     * Get the service that is mapped to the specified request.
     */
    protected String getService(UMOEventContext context) {
        String pathInfo = context.getEndpointURI().getPath();

        if (pathInfo == null || "".equals(pathInfo))
            return context.getEndpointURI().getHost();

        String serviceName;

        int i = pathInfo.lastIndexOf("/");

        if (i > -1) {
            serviceName = pathInfo.substring(i + 1);
        } else {
            serviceName = pathInfo;
        }

        return serviceName;
    }

    protected Service getService(String name) {
        return getXfire().getServiceRegistry().getService(name);
    }

    public XFire getXfire() {
        return xfire;
    }

    public void setXfire(XFire xfire) {
        this.xfire = xfire;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public ServiceRegistry getServiceRegistry() {
        return xfire.getServiceRegistry();
    }
}
