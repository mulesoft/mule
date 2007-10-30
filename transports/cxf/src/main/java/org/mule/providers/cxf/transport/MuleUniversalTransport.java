
package org.mule.providers.cxf.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.Port;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;

import org.mule.providers.cxf.CxfConnector;

import org.xmlsoap.schemas.wsdl.http.AddressType;

public class MuleUniversalTransport extends AbstractTransportFactory
    implements ConduitInitiator, DestinationFactory, WSDLEndpointFactory
{

    public static final String TRANSPORT_ID = "http://mule.codehaus.org/cxf";

    private static Set<String> PREFIXES = new HashSet<String>();
    static
    {
        PREFIXES.add("http://");
        PREFIXES.add("https://");
        PREFIXES.add("jms://");
        PREFIXES.add("vm://");
        PREFIXES.add("xmpp://");
        PREFIXES.add("smtp://");
        PREFIXES.add("tcp://");
    }

    private Map<String, Destination> destinations = new HashMap<String, Destination>();

    private Bus bus;

    private CxfConnector connector;

    public MuleUniversalTransport(CxfConnector connector)
    {
        super();

        ArrayList<String> tids = new ArrayList<String>();
        tids.add("http://schemas.xmlsoap.org/soap/http");
        setTransportIds(tids);

        this.connector = connector;
    }

    public Destination getDestination(EndpointInfo ei) throws IOException
    {
        return getDestination(ei, createReference(ei));
    }

    protected Destination getDestination(EndpointInfo ei, EndpointReferenceType reference) throws IOException
    {
        String uri = reference.getAddress().getValue();
        int idx = uri.indexOf('?');
        if (idx != -1)
        {
            uri = uri.substring(0, idx);
        }

        synchronized (this)
        {
            Destination d = destinations.get(uri);
            if (d == null)
            {
                d = createDestination(ei, reference);
                destinations.put(uri, d);
            }
            return d;
        }
    }

    private Destination createDestination(EndpointInfo ei, EndpointReferenceType reference)
    {
        return new MuleUniversalDestination(this, reference, ei);
    }

    public Conduit getConduit(EndpointInfo ei) throws IOException
    {
        return new MuleUniversalConduit(this, connector, ei, null);
    }

    public Conduit getConduit(EndpointInfo ei, EndpointReferenceType target) throws IOException
    {
        return new MuleUniversalConduit(this, connector, ei, target);
    }

    EndpointReferenceType createReference(EndpointInfo ei)
    {
        EndpointReferenceType epr = new EndpointReferenceType();
        AttributedURIType address = new AttributedURIType();
        address.setValue(ei.getAddress());
        epr.setAddress(address);
        return epr;
    }

    @Override
    public Set<String> getUriPrefixes()
    {
        return PREFIXES;
    }

    public Bus getBus()
    {
        return bus;
    }

    public void setBus(Bus bus)
    {
        this.bus = bus;
    }

    void remove(MuleUniversalDestination destination)
    {
        destinations.remove(destination.getAddress().getAddress().getValue());
    }

    public CxfConnector getConnector()
    {
        return connector;
    }

    // Stuff relating to building of the <soap:address/> -
    // I have no idea how this really works, but it does

    public void createPortExtensors(EndpointInfo ei, Service service)
    {
        // TODO
    }

    @SuppressWarnings("unchecked")
    public EndpointInfo createEndpointInfo(ServiceInfo serviceInfo, BindingInfo b, Port port)
    {
        if (port != null)
        {
            List ees = port.getExtensibilityElements();
            for (Iterator itr = ees.iterator(); itr.hasNext();)
            {
                Object extensor = itr.next();

                if (extensor instanceof HTTPAddress)
                {
                    final HTTPAddress httpAdd = (HTTPAddress) extensor;

                    EndpointInfo info = new HttpEndpointInfo(serviceInfo,
                        "http://schemas.xmlsoap.org/wsdl/http/");
                    info.setAddress(httpAdd.getLocationURI());
                    info.addExtensor(httpAdd);
                    return info;
                }
                else if (extensor instanceof AddressType)
                {
                    final AddressType httpAdd = (AddressType) extensor;

                    EndpointInfo info = new HttpEndpointInfo(serviceInfo,
                        "http://schemas.xmlsoap.org/wsdl/http/");
                    info.setAddress(httpAdd.getLocation());
                    info.addExtensor(httpAdd);
                    return info;
                }
            }
        }
        HttpEndpointInfo hei = new HttpEndpointInfo(serviceInfo, "http://schemas.xmlsoap.org/wsdl/http/");
        AddressType at = new HttpAddressType();
        hei.addExtensor(at);

        return hei;
    }

    private static class HttpEndpointInfo extends EndpointInfo
    {
        AddressType saddress;

        HttpEndpointInfo(ServiceInfo serv, String trans)
        {
            super(serv, trans);
        }

        public void setAddress(String s)
        {
            super.setAddress(s);
            if (saddress != null)
            {
                saddress.setLocation(s);
            }
        }

        public void addExtensor(Object el)
        {
            super.addExtensor(el);
            if (el instanceof AddressType)
            {
                saddress = (AddressType) el;
            }
        }
    }

    private static class HttpAddressType extends AddressType implements HTTPAddress, SOAPAddress
    {
        public HttpAddressType()
        {
            super();
            setElementType(new QName("http://schemas.xmlsoap.org/wsdl/soap/", "address"));
        }

        public String getLocationURI()
        {
            return getLocation();
        }

        public void setLocationURI(String locationURI)
        {
            setLocation(locationURI);
        }

    }
}
