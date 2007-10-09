/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.transport;

import org.mule.impl.RequestContext;
import org.mule.providers.soap.xfire.MuleInvoker;
import org.mule.umo.UMOEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.service.Binding;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.soap.Soap11;
import org.codehaus.xfire.soap.Soap12;
import org.codehaus.xfire.soap.SoapTransport;
import org.codehaus.xfire.soap.SoapTransportHelper;
import org.codehaus.xfire.soap.SoapVersion;
import org.codehaus.xfire.transport.AbstractTransport;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.DefaultEndpoint;
import org.codehaus.xfire.wsdl11.WSDL11Transport;

/**
 * This is a custom xfire transport that implements custom interaction with mule as well
 * as supports a wider range of transport schemes that xfire's soap transport implementation.
 */
public class MuleUniversalTransport extends AbstractTransport 
    implements WSDL11Transport, SoapTransport
{
    public static final String SOAP11_HTTP_BINDING = "http://schemas.xmlsoap.org/soap/http";
    public static final String SOAP12_HTTP_BINDING = "http://www.w3.org/2003/05/soap/bindings/HTTP/";
    public static final String HTTP_BINDING = "http://www.w3.org/2004/08/wsdl/http";
    public static final String HTTP_TRANSPORT_NS = "http://schemas.xmlsoap.org/soap/mule";
    private static final String URI_PREFIX = "urn:xfire:transport:mule:";

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());
    
    public MuleUniversalTransport()
    {
        SoapTransportHelper.createSoapTransport(this);
    }

    protected Channel createNewChannel(String uri)
    {
        logger.debug("Creating new channel for uri: " + uri);

        MuleUniversalChannel c = new MuleUniversalChannel(uri, this);
        c.setEndpoint(new DefaultEndpoint());

        return c;
    }

    protected String getUriPrefix()
    {
        return URI_PREFIX;
    }

    /**
     * Get the URL for a particular service.
     */
    public String getServiceURL(Service service)
    {
        //return "http://localhost/services/" + service.getSimpleName();
        String ep = ((MuleInvoker) service.getInvoker()).getEndpoint().getEndpointURI().getAddress();
        return ep + "/" + service.getSimpleName();
    }

    public String getTransportURI(Service service)
    {
        return HTTP_TRANSPORT_NS;
    }

    public String[] getKnownUriSchemes()
    {
        return new String[]{"http://", "https://", "jms://", "vm://", "xmpp://", "smtp://", "tcp://"};
    }

    public String[] getSupportedBindings()
    {
        return new String[]{SOAP11_HTTP_BINDING, SOAP12_HTTP_BINDING};
    }

    public String getName()
    {
        UMOEvent event = RequestContext.getEvent();
        if (event != null && event.getEndpoint() != null)
        {
            String scheme = event.getEndpoint().getEndpointURI().getScheme();
            return scheme.substring(0, 1).toUpperCase() + scheme.substring(1);
            
        }
        
        return "Mule";
    }

    public Binding findBinding(MessageContext context, Service service)
    {
        SoapVersion version = context.getCurrentMessage().getSoapVersion();

        if (version instanceof Soap11)
        {
            return service.getBinding(SOAP11_HTTP_BINDING);
        }
        else if (version instanceof Soap12)
        {
            return service.getBinding(SOAP12_HTTP_BINDING);
        }

        return super.findBinding(context, service);
    }
}
