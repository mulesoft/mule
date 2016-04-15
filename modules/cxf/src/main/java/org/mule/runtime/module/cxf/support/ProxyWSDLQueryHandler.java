/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.support;

import org.mule.module.cxf.CxfConstants;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.xml.WSDLReader;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.Bus;
import org.apache.cxf.catalog.CatalogWSDLLocator;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ResourceManagerWSDLLocator;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class ProxyWSDLQueryHandler extends WSDLQueryHandler
{

    public static final String XSD_PARAMETER_NAME = "?xsd=";
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

        for (Element serviceEl : serviceList)
        {
            String serviceName = serviceEl.getAttribute("name");
            if (serviceName.equals(ei.getService().getName().getLocalPart()))
            {
                List<Element> elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                  "http://schemas.xmlsoap.org/wsdl/",
                                                                  "port");
                for (Element el : elementList)
                {
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
        WSDLManager wsdlManager = bus.getExtension(WSDLManager.class);
        if (wsdlManager != null)
        {
            String wsdlLocation = endpointInfo.getService().getProperty(CxfConstants.WSDL_LOCATION, String.class);
            if(wsdlLocation != null)
            {
                return loadDefinition(wsdlManager, wsdlLocation);
            }

        }
        return new ServiceWSDLBuilder(bus, endpointInfo.getService()).build();

    }

    // Make sure we have a new WSDL definition loaded and not the cached that the WSDLManager
    // would return because it might have unwanted changes that will impact the resulting WSDL
    private Definition loadDefinition(WSDLManager wsdlManager, String url) throws WSDLException {
        WSDLReader reader = wsdlManager.getWSDLFactory().newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        reader.setFeature("javax.wsdl.importDocuments", true);
        reader.setExtensionRegistry(wsdlManager.getExtensionRegistry());
        CatalogWSDLLocator catLocator = new CatalogWSDLLocator(url, bus);
        ResourceManagerWSDLLocator wsdlLocator = new ResourceManagerWSDLLocator(url, catLocator, bus);
        InputSource src = wsdlLocator.getBaseInputSource();
        Definition def;
        if (src.getByteStream() != null || src.getCharacterStream() != null)
        {
            Document doc;
            XMLStreamReader xmlReader = null;
            try
            {
                xmlReader = StaxUtils.createXMLStreamReader(src);
                doc = StaxUtils.read(xmlReader, true);
                if (src.getSystemId() != null)
                {
                    try
                    {
                        doc.setDocumentURI(new String(src.getSystemId()));
                    }
                    catch (Exception e)
                    {
                        //ignore - probably not DOM level 3
                    }
                }
            }
            catch (Exception e)
            {
                throw new WSDLException(WSDLException.PARSER_ERROR, e.getMessage(), e);
            }
            finally
            {
                StaxUtils.close(xmlReader);
            }
            def = reader.readWSDL(wsdlLocator, doc.getDocumentElement());
        }
        else
        {
            def = reader.readWSDL(wsdlLocator);
        }

        return def;
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

    @Override
    protected void checkSchemaUrl(Map<String, SchemaReference> doneSchemas, String start, String decodedStart, SchemaReference imp) throws MalformedURLException
    {
        super.checkSchemaUrl(doneSchemas, start, decodedStart, imp);
        doneSchemas.put(decodedStart, imp);
        String xsdParameterValue = getXsdParameterValue(decodedStart);
        if( xsdParameterValue != null )
        {
            doneSchemas.put(xsdParameterValue, imp);
        }
    }

    @Override
    protected String rewriteSchemaLocation(String base, String schemaLocation)
    {
        String xsdParameterValue = getXsdParameterValue(schemaLocation);
        if( xsdParameterValue != null )
        {
            schemaLocation =  xsdParameterValue;
        }
        return super.rewriteSchemaLocation(base, schemaLocation);
    }

    private String getXsdParameterValue(String schemaLocation)
    {
        int position = schemaLocation.indexOf(XSD_PARAMETER_NAME);
        if( position > -1 )
        {
            return schemaLocation.substring(position + XSD_PARAMETER_NAME.length());
        }
        return null;
    }

}
