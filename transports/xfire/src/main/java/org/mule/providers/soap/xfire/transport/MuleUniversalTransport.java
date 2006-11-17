/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.transport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.service.Binding;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.soap.Soap11;
import org.codehaus.xfire.soap.Soap12;
import org.codehaus.xfire.soap.SoapTransportHelper;
import org.codehaus.xfire.soap.SoapVersion;
import org.codehaus.xfire.transport.AbstractTransport;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.DefaultEndpoint;
import org.codehaus.xfire.wsdl11.WSDL11Transport;

/**
 * TODO document
 */
public class MuleUniversalTransport extends AbstractTransport implements WSDL11Transport
{
    public static final String SOAP11_HTTP_BINDING = "http://schemas.xmlsoap.org/soap/http";
    public static final String SOAP12_HTTP_BINDING = "http://www.w3.org/2003/05/soap/bindings/HTTP/";
    public final static String HTTP_BINDING = "http://www.w3.org/2004/08/wsdl/http";
    public final static String HTTP_TRANSPORT_NS = "http://schemas.xmlsoap.org/soap/mule";
    private final static String URI_PREFIX = "urn:xfire:transport:mule:";

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
        return "http://localhost/services/" + service.getSimpleName();
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
