
package org.mule.providers.cxf.support;

import java.io.IOException;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.MessageObserver;
import org.mule.umo.endpoint.EndpointNotFoundException;

public final class CxfUtils
{

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

}
