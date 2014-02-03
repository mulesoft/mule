/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.support;

import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProxyWSDLQueryHandler extends WSDLQueryHandler
{
    private final String port;

    public ProxyWSDLQueryHandler(Bus b, final String port)
    {
        super(b);
        this.port = port;
    }

    @Override
    protected void rewriteOperationAddress(EndpointInfo ei, Document doc, String base)
    {
        List<Element> serviceList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                        "http://schemas.xmlsoap.org/wsdl/",
                                                                        "service");

        for (Element serviceEl : serviceList) {
            String serviceName = serviceEl.getAttribute("name");
            if (serviceName.equals(ei.getService().getName().getLocalPart())) {
                List<Element> elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                  "http://schemas.xmlsoap.org/wsdl/",
                                                                  "port");
                for (Element el : elementList) {
                     if(rewritePortAddress(el))
                     {
                         List<Element> addresses = findAddresses(el, WSDLConstants.NS_SOAP);
                         if(addresses.isEmpty())
                         {
                             addresses = findAddresses(el, WSDLConstants.NS_SOAP12);
                         }
                         if(addresses.isEmpty())
                         {
                             addresses = findAddresses(el, WSDLConstants.QNAME_XMLHTTP_BINDING_ADDRESS.getNamespaceURI());
                         }

                         if(!addresses.isEmpty())
                         {
                             Element address = addresses.iterator().next();
                             address.setAttribute("location", base);
                         }
                     }
                }
            }
        }
    }

    @Override
    protected Definition getDefinition(EndpointInfo endpointInfo) throws WSDLException
    {
        WSDLManager manager = bus.getExtension(WSDLManager.class);
        if (manager != null)
        {
            String wsdlLocation = (String)endpointInfo.getService().getProperty("WSDL_LOCATION");
            if(wsdlLocation != null)
            {
                return manager.getDefinition((String)endpointInfo.getService().getProperty("WSDL_LOCATION"));
            }

        }
        return new ServiceWSDLBuilder(bus, endpointInfo.getService()).build();

    }

    private List<Element> findAddresses(Element port, String namespaceUri)
    {
        return DOMUtils.findAllElementsByTagNameNS(port, namespaceUri, "address");
    }

    // Rewrite all port addresses unless there's a port name specified by the proxy.
    private boolean rewritePortAddress(Element el)
    {
        return port == null || el.getAttribute("name").equals(port);
    }

}
