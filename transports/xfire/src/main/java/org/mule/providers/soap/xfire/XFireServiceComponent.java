/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.config.i18n.MessageFactory;
import org.mule.impl.MuleMessage;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.soap.SoapConstants;
import org.mule.providers.soap.xfire.transport.MuleLocalChannel;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.util.Enumeration;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.ServiceRegistry;
import org.codehaus.xfire.transport.Transport;
import org.codehaus.xfire.transport.TransportManager;
import org.codehaus.xfire.transport.http.HtmlServiceWriter;

/**
 * The Xfire service component receives requests for Xfire services it manages and
 * marshalls requests and responses
 * 
 */
public class XFireServiceComponent implements Callable, Lifecycle
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    
    protected XFire xfire;

    // manager to the component
    protected Transport transport;
    protected String transportClass;
    

    /** For IoC */
    public XFireServiceComponent(XFireMessageReceiver receiver) 
    {
        super();
    }
   
    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(eventContext);
        }

        boolean wsdlRequested = false;
        boolean servicesRequested = false;

        //if http request
        String request = eventContext.getMessage().getStringProperty(HttpConnector.HTTP_REQUEST_PROPERTY,
            StringUtils.EMPTY);
        if (request.toLowerCase().endsWith(org.mule.providers.soap.SoapConstants.WSDL_PROPERTY))
        {
            wsdlRequested = true;
        }
        else if (request.toLowerCase().endsWith(org.mule.providers.soap.SoapConstants.LIST_PROPERTY))
        {
            servicesRequested = true;
        }
        else //if servlet request
        {
            Enumeration keys = eventContext.getEndpointURI().getParams().keys();
            while(keys.hasMoreElements()){
                if ((keys.nextElement()).toString().equalsIgnoreCase(SoapConstants.WSDL_PROPERTY)) {
                    wsdlRequested = true;
                    break;
                }
            }
        }
        
        if (wsdlRequested)
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            getXfire().generateWSDL(getServiceName(eventContext), out);
            UMOMessage result = new MuleMessage(out.toString(eventContext.getEncoding()));
            result.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");
            return result;
        }
        else if(servicesRequested)
        {
            // In order to list all services we need to pass in a HttpServletRequest to the write, which we don't have
            //So we can just list the current service
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            HtmlServiceWriter writer = new HtmlServiceWriter();
            writer.write(out, getServiceRegistry().getService(getServiceName(eventContext)));
            UMOMessage result = new MuleMessage(out.toString(eventContext.getEncoding()));
            result.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");
            return result;
        }
        else
        {
            if (transport == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("transport is null, this service has not been initialized properly"), this);
            }
            
            MuleLocalChannel channel = (MuleLocalChannel)transport.createChannel(eventContext.getEndpointURI()
                .getFullScheme());
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
        if (xfire == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("No XFire instance, this component has not been initialized properly."), this);
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
        return (InputStream) context.getMessage().getPayload(InputStream.class);
    }

    /**
     * Get the service that is mapped to the specified request.
     * 
     * @param context the context from which to find the service name
     * @return the service that is mapped to the specified request.
     */
    protected String getServiceName(UMOEventContext context)
    {
        String pathInfo = context.getEndpointURI().getPath();

        if (StringUtils.isEmpty(pathInfo))
        {
            return context.getEndpointURI().getHost();
        }

        String serviceName;

        int i = pathInfo.lastIndexOf("/");

        if (i > -1)
        {
            serviceName = pathInfo.substring(i + 1);
        }
        else
        {
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
    
    public void setTransportClass(String clazz)
    {
        transportClass = clazz;
    }

    public ServiceRegistry getServiceRegistry()
    {
        return xfire.getServiceRegistry();
    }
}
