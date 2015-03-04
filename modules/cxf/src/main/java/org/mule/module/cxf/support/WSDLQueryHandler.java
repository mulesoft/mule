/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.frontend.WSDLQueryException;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.transports.http.StemMatchingQueryHandler;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ResourceManagerWSDLLocator;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;


/**
 *  WSDLQueryHandler class preserved from cxf 2.3.
 *  CXF 2.5 removed the class and now relies on the WSDLGetInterceptor to perform the same logic, but the interceptor
 *  in its finally clause removes the content of the message from the exchange and it can be retrieved.
 */
public class WSDLQueryHandler implements StemMatchingQueryHandler {
    private static final Logger LOG = LogUtils.getL7dLogger(WSDLQueryHandler.class);
    protected Bus bus;

    public WSDLQueryHandler(Bus b) {
        bus = b;
    }

    @Override
    public String getResponseContentType(String baseUri, String ctx) {
        if (baseUri.toLowerCase().contains("?wsdl")
            || baseUri.toLowerCase().contains("?xsd=")) {
            return "text/xml";
        }
        return null;
    }

    @Override
    public boolean isRecognizedQuery(String baseUri, String ctx,
                                     EndpointInfo endpointInfo, boolean contextMatchExact) {
        if (baseUri != null
            && (baseUri.contains("?")
                && (baseUri.toLowerCase().contains("wsdl")
                || baseUri.toLowerCase().contains("xsd=")))) {

            int idx = baseUri.indexOf("?");
            Map<String, String> map = UrlUtils.parseQueryString(baseUri.substring(idx + 1));
            if (map.containsKey("wsdl")
                || map.containsKey("xsd")) {
                if (contextMatchExact) {
                    return endpointInfo.getAddress().contains(ctx);
                } else {
                    // contextMatchStrategy will be "stem"
                    return endpointInfo.getAddress().
                                contains(UrlUtils.getStem(baseUri.substring(0, idx)));
                }
            }
        }
        return false;
    }

    @Override
    public void writeResponse(String baseUri, String ctxUri,
                              EndpointInfo endpointInfo, OutputStream os) {
        try {
            int idx = baseUri.toLowerCase().indexOf("?");
            Map<String, String> params = UrlUtils.parseQueryString(baseUri.substring(idx + 1));

            String base;

            if (endpointInfo.getProperty("publishedEndpointUrl") != null) {
                base = String.valueOf(endpointInfo.getProperty("publishedEndpointUrl"));
            } else {
                base = baseUri.substring(0, baseUri.toLowerCase().indexOf("?"));
            }

            String wsdl = params.get("wsdl");
            if (wsdl != null) {
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.
                wsdl = URLDecoder.decode(wsdl, "utf-8");
            }

            String xsd =  params.get("xsd");
            if (xsd != null) {
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.
                xsd = URLDecoder.decode(xsd, "utf-8");
            }

            Map<String, Definition> mp = CastUtils.cast((Map)endpointInfo.getService()
                                                        .getProperty(WSDLQueryHandler.class.getName()));
            Map<String, SchemaReference> smp = CastUtils.cast((Map)endpointInfo.getService()
                                                        .getProperty(WSDLQueryHandler.class.getName()
                                                                     + ".Schemas"));

            if (mp == null) {
                endpointInfo.getService().setProperty(WSDLQueryHandler.class.getName(),
                                                      new ConcurrentHashMap());
                mp = CastUtils.cast((Map)endpointInfo.getService()
                                    .getProperty(WSDLQueryHandler.class.getName()));
            }
            if (smp == null) {
                endpointInfo.getService().setProperty(WSDLQueryHandler.class.getName()
                                                      + ".Schemas",
                                                      new ConcurrentHashMap());
                smp = CastUtils.cast((Map)endpointInfo.getService()
                                    .getProperty(WSDLQueryHandler.class.getName()
                                                 + ".Schemas"));
            }

            if (!mp.containsKey("")) {
                Definition def = getDefinition(endpointInfo);

                mp.put("", def);
                updateDefinition(def, mp, smp, base, endpointInfo);
            }


            Document doc;
            if (xsd == null) {
                Definition def = mp.get(wsdl);
                if (def == null) {
                    String wsdl2 = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
                                                       wsdl,
                                                       base);
                    if (wsdl2 != null) {
                        def = mp.get(wsdl2);
                    }
                }
                if (def == null) {
                   throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("WSDL_NOT_FOUND",
                                                                                        LOG, wsdl), null);
                }

                synchronized (def) {
                    //writing a def is not threadsafe.  Sync on it to make sure
                    //we don't get any ConcurrentModificationExceptions
                    if (endpointInfo.getProperty("publishedEndpointUrl") != null) {
                        String publishingUrl =
                            String.valueOf(endpointInfo.getProperty("publishedEndpointUrl"));
                        updatePublishedEndpointUrl(publishingUrl, def, endpointInfo.getName());
                    }

                    WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class)
                        .getWSDLFactory().newWSDLWriter();
                    def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
                    doc = wsdlWriter.getDocument(def);
                }
            } else {
                SchemaReference si = smp.get(xsd);
                if (si == null) {
                    String xsd2 = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
                                                       xsd,
                                                       base);
                    if (xsd2 != null) {
                        si = smp.get(xsd2);
                    }
                }
                if (si == null) {
                    throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("SCHEMA_NOT_FOUND",
                                                                                        LOG, wsdl), null);
                }

                String uri = si.getReferencedSchema().getDocumentBaseURI();
                uri = resolveWithCatalogs(OASISCatalogManager.getCatalogManager(bus),
                                          uri,
                                          si.getReferencedSchema().getDocumentBaseURI());
                if (uri == null) {
                    uri = si.getReferencedSchema().getDocumentBaseURI();
                }
                ResourceManagerWSDLLocator rml = new ResourceManagerWSDLLocator(uri,
                                                                                bus);

                InputSource src = rml.getBaseInputSource();
                doc = XMLUtils.getParser().parse(src);
            }

            updateDoc(doc, base, mp, smp, endpointInfo);
            String enc = null;
            try {
                enc = doc.getXmlEncoding();
            } catch (Exception ex) {
                //ignore - not dom level 3
            }
            if (enc == null) {
                enc = "utf-8";
            }

            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os,
                                                                     enc);
            StaxUtils.writeNode(doc, writer, true);
            writer.flush();
        } catch (WSDLQueryException wex) {
            throw wex;
        } catch (Exception wex) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                     LOG,
                                                     baseUri), wex);
        }
    }


    protected Definition getDefinition(EndpointInfo endpointInfo) throws WSDLException
    {
        return new ServiceWSDLBuilder(bus, endpointInfo.getService()).build();
    }

    protected void updateDoc(Document doc, String base,
                           Map<String, Definition> mp,
                           Map<String, SchemaReference> smp,
                           EndpointInfo ei) {
        List<Element> elementList = null;


        try {
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                           "http://www.w3.org/2001/XMLSchema",
                                                                           "import");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                if (smp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("schemaLocation", rewriteSchemaLocation(base, sl));
                }
            }

            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema",
                                                              "include");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                if (smp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("schemaLocation", rewriteSchemaLocation(base, sl));
                }
            }
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://www.w3.org/2001/XMLSchema",
                                                              "redefine");
            for (Element el : elementList) {
                String sl = el.getAttribute("schemaLocation");
                if (smp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("schemaLocation", rewriteSchemaLocation(base, sl));
                }
            }
            elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                              "http://schemas.xmlsoap.org/wsdl/",
                                                              "import");
            for (Element el : elementList) {
                String sl = el.getAttribute("location");
                if (mp.containsKey(URLDecoder.decode(sl, "utf-8"))) {
                    el.setAttribute("location", base + "?wsdl=" + sl.replace(" ", "%20"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                     LOG,
                                                      base), e);
        }

        rewriteOperationAddress(ei, doc, base);

        try {
            doc.setXmlStandalone(true);
        } catch (Exception ex) {
            //likely not DOM level 3
        }
    }

    protected String rewriteSchemaLocation(String base, String schemaLocation)
    {
        return base + "?xsd=" + schemaLocation.replace(" ", "%20");
    }

    protected void rewriteOperationAddress(EndpointInfo ei, Document doc, String base)
    {
        Boolean rewriteSoapAddress = ei.getProperty("autoRewriteSoapAddress", Boolean.class);
        List<Element> elementList = null;

        if (rewriteSoapAddress != null && rewriteSoapAddress.booleanValue()) {
            List<Element> serviceList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                            "http://schemas.xmlsoap.org/wsdl/",
                                                                            "service");
            for (Element serviceEl : serviceList) {
                String serviceName = serviceEl.getAttribute("name");
                if (serviceName.equals(ei.getService().getName().getLocalPart())) {
                    elementList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                      "http://schemas.xmlsoap.org/wsdl/",
                                                                      "port");
                    for (Element el : elementList) {
                        String name = el.getAttribute("name");
                        if (name.equals(ei.getName().getLocalPart())) {
                            Element soapAddress = DOMUtils.findAllElementsByTagNameNS(el,
                                                                                      "http://schemas.xmlsoap.org/wsdl/soap/",
                                                                                      "address")
                                    .iterator().next();
                            soapAddress.setAttribute("location", base);
                        }
                    }
                }
            }
        }
    }

    static String resolveWithCatalogs(OASISCatalogManager catalogs, String start, String base) {
        if (catalogs == null) {
            return null;
        }
        String resolvedSchemaLocation = null;
        try {
            resolvedSchemaLocation = catalogs.resolveSystem(start);
            if (resolvedSchemaLocation == null) {
                resolvedSchemaLocation = catalogs.resolveURI(start);
            }
            if (resolvedSchemaLocation == null) {
                resolvedSchemaLocation = catalogs.resolvePublic(start, base);
            }
        } catch (Exception ex) {
            //ignore
        }
        return resolvedSchemaLocation;
    }

    protected void updateDefinition(Definition def, Map<String, Definition> done,
                                  Map<String, SchemaReference> doneSchemas,
                                  String base, EndpointInfo ei) {
        OASISCatalogManager catalogs = OASISCatalogManager.getCatalogManager(bus);

        Collection<List<?>> imports = CastUtils.cast((Collection<?>)def.getImports().values());
        for (List<?> lst : imports) {
            List<Import> impLst = CastUtils.cast(lst);
            for (Import imp : impLst) {

                String start = imp.getLocationURI();
                String decodedStart = null;
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.
                try {
                    decodedStart = URLDecoder.decode(start, "utf-8");
                } catch (UnsupportedEncodingException e) {
                   throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                     LOG,
                                                     start), e);
                }

                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);

                if (resolvedSchemaLocation == null) {
                    try {
                        //check to see if it's already in a URL format.  If so, leave it.
                        new URL(start);
                    } catch (MalformedURLException e) {
                        if (done.put(decodedStart, imp.getDefinition()) == null) {
                            updateDefinition(imp.getDefinition(), done, doneSchemas, base, ei);
                        }
                    }
                } else {
                    if (done.put(decodedStart, imp.getDefinition()) == null) {
                        done.put(resolvedSchemaLocation, imp.getDefinition());
                        updateDefinition(imp.getDefinition(), done, doneSchemas, base, ei);
                    }
                }
            }
        }


        /* This doesn't actually work.   Setting setSchemaLocationURI on the import
        * for some reason doesn't actually result in the new URI being written
        * */
        Types types = def.getTypes();
        if (types != null) {
            for (ExtensibilityElement el
                : CastUtils.cast(types.getExtensibilityElements(), ExtensibilityElement.class)) {
                if (el instanceof Schema) {
                    Schema see = (Schema)el;
                    updateSchemaImports(see, doneSchemas, base);
                }
            }
        }
    }

    protected void updatePublishedEndpointUrl(String publishingUrl, Definition def, QName name) {
        Collection<Service> services = CastUtils.cast(def.getAllServices().values());
        for (Service service : services) {
            Collection<Port> ports = CastUtils.cast(service.getPorts().values());
            if (ports.isEmpty()) {
                continue;
            }

            if (name == null) {
                setSoapAddressLocationOn(ports.iterator().next(), publishingUrl);
                break; // only update the first port since we don't target any specific port
            } else {
                for (Port port : ports) {
                    if (name.getLocalPart().equals(port.getName())) {
                        setSoapAddressLocationOn(port, publishingUrl);
                    }
                }
            }
        }
    }

    private void setSoapAddressLocationOn(Port port, String url) {
        List<?> extensions = port.getExtensibilityElements();
        for (Object extension : extensions) {
            if (extension instanceof SOAP12Address) {
                ((SOAP12Address)extension).setLocationURI(url);
            } else if (extension instanceof SOAPAddress) {
                ((SOAPAddress)extension).setLocationURI(url);
            }
        }
    }

    protected void updateSchemaImports(Schema schema,
                                           Map<String, SchemaReference> doneSchemas,
                                           String base) {
        OASISCatalogManager catalogs = OASISCatalogManager.getCatalogManager(bus);
        Collection<List<?>>  imports = CastUtils.cast((Collection<?>)schema.getImports().values());
        for (List<?> lst : imports) {
            List<SchemaImport> impLst = CastUtils.cast(lst);
            for (SchemaImport imp : impLst) {
                String start = imp.getSchemaLocationURI();

                if (start != null) {
                    String decodedStart = null;
                    // Always use the URL decoded version to ensure that we have a
                    // canonical representation of the import URL for lookup.
                    try {
                        decodedStart = URLDecoder.decode(start, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                         throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                     LOG,
                                                     start), e);
                    }

                    if (!doneSchemas.containsKey(decodedStart)) {
                        String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                        if (resolvedSchemaLocation == null) {
                            try {
                                checkSchemaUrl(doneSchemas, start, decodedStart, imp);
                            } catch (MalformedURLException e) {
                                if (doneSchemas.put(decodedStart, imp) == null) {
                                    updateSchemaImports(imp.getReferencedSchema(), doneSchemas, base);
                                }
                            }
                        } else {
                            if (doneSchemas.put(decodedStart, imp) == null) {
                                doneSchemas.put(resolvedSchemaLocation, imp);
                                updateSchemaImports(imp.getReferencedSchema(), doneSchemas, base);
                            }
                        }
                    }
                }
            }
        }

        List<SchemaReference> includes = CastUtils.cast(schema.getIncludes());
        for (SchemaReference included : includes) {
            String start = included.getSchemaLocationURI();

            if (start != null) {
                String decodedStart = null;
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.
                try {
                    decodedStart = URLDecoder.decode(start, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    /*throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                     LOG,
                                                     start), e); */
                }

                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                if (resolvedSchemaLocation == null) {
                    if (!doneSchemas.containsKey(decodedStart)) {
                        try {
                            checkSchemaUrl(doneSchemas, start, decodedStart, included);
                        } catch (MalformedURLException e) {
                            if (doneSchemas.put(decodedStart, included) == null) {
                                updateSchemaImports(included.getReferencedSchema(), doneSchemas, base);
                            }
                        }
                    }
                } else if (!doneSchemas.containsKey(decodedStart)
                    || !doneSchemas.containsKey(resolvedSchemaLocation)) {
                    doneSchemas.put(decodedStart, included);
                    doneSchemas.put(resolvedSchemaLocation, included);
                    updateSchemaImports(included.getReferencedSchema(), doneSchemas, base);
                }
            }
        }
        List<SchemaReference> redefines = CastUtils.cast(schema.getRedefines());
        for (SchemaReference included : redefines) {
            String start = included.getSchemaLocationURI();

            if (start != null) {
                String decodedStart = null;
                // Always use the URL decoded version to ensure that we have a
                // canonical representation of the import URL for lookup.
                try {
                    decodedStart = URLDecoder.decode(start, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new WSDLQueryException(new org.apache.cxf.common.i18n.Message("COULD_NOT_PROVIDE_WSDL",
                                                     LOG,
                                                     start), e);
                }

                String resolvedSchemaLocation = resolveWithCatalogs(catalogs, start, base);
                if (resolvedSchemaLocation == null) {
                    if (!doneSchemas.containsKey(decodedStart)) {
                        try {
                            checkSchemaUrl(doneSchemas, start, decodedStart, included);
                        } catch (MalformedURLException e) {
                            if (doneSchemas.put(decodedStart, included) == null) {
                                updateSchemaImports(included.getReferencedSchema(), doneSchemas, base);
                            }
                        }
                    }
                } else if (!doneSchemas.containsKey(decodedStart)
                    || !doneSchemas.containsKey(resolvedSchemaLocation)) {
                    doneSchemas.put(decodedStart, included);
                    doneSchemas.put(resolvedSchemaLocation, included);
                    updateSchemaImports(included.getReferencedSchema(), doneSchemas, base);
                }
            }
        }
    }

    protected void checkSchemaUrl(Map<String, SchemaReference> doneSchemas, String start, String decodedStart, SchemaReference imp) throws MalformedURLException
    {
        //check to see if it's already in a URL format.  If so, leave it.
        new URL(start);
    }

    @Override
    public boolean isRecognizedQuery(String baseUri, String ctx, EndpointInfo endpointInfo) {
        return isRecognizedQuery(baseUri, ctx, endpointInfo, false);
    }

}
