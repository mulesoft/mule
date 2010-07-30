package org.mule.transport.cxf.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.AbstractMetaEndpointBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.cxf.builder.WsdlClientMessageProcessorBuilder;

import java.util.Arrays;

public class WsdlCxfEndpointBuilder extends AbstractMetaEndpointBuilder
{

    private final String wsdlAddress;

    public WsdlCxfEndpointBuilder(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
        this.wsdlAddress = getEndpointBuilder().toString().substring(9);
    }

    public WsdlCxfEndpointBuilder(String address, MuleContext muleContext)
    {
        super(getAddressWithoutQuery(getEndpointAddressWithoutMetaScheme(address)), muleContext);
        this.wsdlAddress = address.substring(9);
    }
    
    @Override
    public InboundEndpoint buildInboundEndpoint() throws EndpointException, InitialisationException
    {
        throw new UnsupportedOperationException("Inbound meta CXF endpoints not supported");
    }

    @Override
    public OutboundEndpoint buildOutboundEndpoint() throws EndpointException, InitialisationException
    {
        WsdlClientMessageProcessorBuilder builder = new WsdlClientMessageProcessorBuilder();
        builder.setMuleContext(muleContext);
        builder.setWsdlLocation(wsdlAddress);
        builder.setOperation(getOperation());
        
        try
        {
            messageProcessors = Arrays.asList((MessageProcessor)builder.build());
        }
        catch (Exception e)
        {
            throw new EndpointException(e);
        }

        return super.buildOutboundEndpoint();
    }

    private String getOperation()
    {
        String query = wsdlAddress;
        int idx = wsdlAddress.lastIndexOf('?');
        if (idx != -1) {
            query = wsdlAddress.substring(idx+1);
        } else {
            return null;
        }
        
        String[] params = query.split("&");
        for (String p : params) {
            if (p.startsWith("method=")) {
                return p.substring(7);
            }
        }
        return null;
    }

    private static String getAddressWithoutQuery(String string)
    {
        int idx = string.indexOf('?');
        if (idx != -1)
        {
            string = string.substring(0, idx);
        }
        return string;
    }
    
}
