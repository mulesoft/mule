/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.support;

import org.mule.api.endpoint.EndpointNotFoundException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.SoapVersionFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.MessageObserver;

public final class CxfUtils
{

    @SuppressWarnings("unchecked")
    public static boolean removeInterceptor(List<Interceptor<? extends Message>> inInterceptors, String name)
    {

        for (Interceptor<?> i : inInterceptors)
        {
            if (i instanceof PhaseInterceptor)
            {
                PhaseInterceptor<Message> p = (PhaseInterceptor<Message>)i;

                if (p.getId().equals(name))
                {
                    inInterceptors.remove(p);
                    return true;
                }
            }
        }

        return false;
    }


    public static Endpoint getEndpoint(DestinationFactory df, String uri)
        throws IOException, EndpointNotFoundException
    {
        int idx = uri.indexOf('?');
        if (idx != -1)
        {
            uri = uri.substring(0, idx);
        }

        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(uri);

        Destination d = df.getDestination(ei);
        if (d.getMessageObserver() == null)
        {
            // TODO is this the right Mule exception?
            throw new EndpointNotFoundException(uri);
        }

        MessageObserver mo = d.getMessageObserver();
        if (!(mo instanceof ChainInitiationObserver))
        {
            throw new EndpointNotFoundException(uri);
        }

        ChainInitiationObserver co = (ChainInitiationObserver) mo;
        return co.getEndpoint();
    }

    public static String getBindingIdForSoapVersion(String version)
    {
        Iterator<SoapVersion> soapVersions = SoapVersionFactory.getInstance().getVersions();
        while(soapVersions.hasNext())
        {
            SoapVersion soapVersion = soapVersions.next();
            if(Double.toString(soapVersion.getVersion()).equals(version))
            {
                return soapVersion.getBindingId();
            }
        }
        throw new IllegalArgumentException("Invalid Soap version " + version);
    }

    public static String mapUnsupportedSchemas(String url)
    {
        //hack for CXF to work correctly with servlet and jetty urls
        if(url != null)
        {
            url = url.replace("servlet://", "http://");
            url = url.replace("jetty://", "http://");
            url = url.replace("jetty-ssl://", "https://");
        }
        return url;
    }

}
