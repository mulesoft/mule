/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.ServiceRegistry;
import org.codehaus.xfire.transport.Transport;
import org.codehaus.xfire.transport.TransportManager;
import org.codehaus.xfire.transport.http.HtmlServiceWriter;
import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.UMODescriptorAware;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.soap.xfire.transport.MuleLocalChannel;
import org.mule.providers.soap.xfire.transport.MuleLocalTransport;
import org.mule.providers.streaming.OutStreamMessageAdapter;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.UMOStreamMessageAdapter;
import org.mule.util.StringUtils;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
        logger.debug(eventContext);
        String request = eventContext.getMessage().getStringProperty(HttpConnector.HTTP_REQUEST_PROPERTY, StringUtils.EMPTY);
        if (request.toLowerCase().endsWith(org.mule.providers.soap.SoapConstants.WSDL_PROPERTY)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            getXfire().generateWSDL(getServiceName(eventContext), out);
            return out.toString();
        } else {
            MuleLocalChannel channel = (MuleLocalChannel) transport.createChannel(eventContext.getEndpointURI().getFullScheme());
            return channel.onCall(eventContext);
        }

    }
    public void start() throws UMOException
    {
        // template method
    }

    public void stop() throws UMOException
    {
        // template method
    }

    public void initialise() throws InitialisationException
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
     * Gets the stream representation of the current message. If the message is set for streaming
     * the input stream on the UMOStreamMEssageAdapter will be used, otherwise a byteArrayInputStream will be used to
     * hold the byte[] representation of the current message.
     * @param context the event context
     * @return The inputstream for the current message
     * @throws UMOException
     *
     */

    protected InputStream getMessageStream(UMOEventContext context) throws UMOException
    {
        InputStream is;
        UMOMessage eventMsg = context.getMessage();
        Object eventMsgPayload = eventMsg.getPayload();

        if (eventMsgPayload instanceof InputStream) {
            is = (InputStream)eventMsgPayload;
        }
        else if (eventMsg.getAdapter() instanceof UMOStreamMessageAdapter) {
            StreamMessageAdapter sma = (StreamMessageAdapter)eventMsg.getAdapter();
            is = sma.getInputStream();

        }
        else {
            is = new ByteArrayInputStream(context.getTransformedMessageAsBytes());
        }
        return is;
    }

    /**
     * Get the service that is mapped to the specified request.
     * @param context the context from which to find the service name
     * @return the service that is mapped to the specified request.
     */
    protected String getServiceName(UMOEventContext context)
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
