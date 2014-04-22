/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WSDLUtils
{

    private static final String XML_NS_PREFIX = "xmlns:";
    private static final String XML_IMPORT_ELEMENT = "import";
    private static final String XML_SCHEMA_LOCATION_ATTRIBUTE = "schemaLocation";

    /**
     * Returns all the XML schemas from a WSDL definition.
     *
     * @throws TransformerException If unable to transform a Schema into String.
     */
    public static List<String> getSchemas(Definition wsdlDefinition) throws TransformerException
    {
        Map<String, String> wsdlNamespaces = wsdlDefinition.getNamespaces();
        List<String> schemas = new ArrayList<String>();
        List<Types> typesList = new ArrayList<Types>();

        extractWsdlTypes(wsdlDefinition, typesList);

        for (Types types : typesList)
        {
            for (Object o : types.getExtensibilityElements())
            {
                if (o instanceof javax.wsdl.extensions.schema.Schema)
                {
                    Schema schema = (Schema) o;
                    for (Map.Entry<String, String> entry : wsdlNamespaces.entrySet())
                    {
                        if (!schema.getElement().hasAttribute(XML_NS_PREFIX + entry.getKey()))
                        {
                            schema.getElement().setAttribute(XML_NS_PREFIX + entry.getKey(), entry.getValue());
                        }
                    }

                    fixSchemaLocations(schema);

                    schemas.add(schemaToString(schema));

                    for (Object location : schema.getIncludes())
                    {
                        schemas.add(schemaToString(((SchemaReference) location).getReferencedSchema()));
                    }
                }
            }
        }
        return schemas;
    }

    /**
     * Extracts the "Types" definition from a WSDL and recursively from all the imports. The types are
     * added to the typesList argument.
     */
    private static void extractWsdlTypes(Definition wsdlDefinition, List<Types> typesList)
    {
        // Add current types definition if present
        if (wsdlDefinition.getTypes() != null)
        {
            typesList.add(wsdlDefinition.getTypes());
        }

        // Allow importing types from other wsdl
        for (Object wsdlImportList : wsdlDefinition.getImports().values())
        {
            for (Import wsdlImport : (List<Import>) wsdlImportList)
            {
                extractWsdlTypes(wsdlImport.getDefinition(), typesList);
            }
        }
    }

    /**
     * Fixes schemaLocation attributes in a parsed schema, allowing references that do not contain
     * a base path (for example, references to local schemas in the classpath).
     */
    private static void fixSchemaLocations(Schema schema)
    {
        String basePath = getBasePath(schema.getDocumentBaseURI());
        Collection<List<SchemaImport>> schemaImportsCollection = schema.getImports().values();

        if (!schemaImportsCollection.isEmpty())
        {
            // Fix schemaLocation values in POJO
            for (List<SchemaImport> schemaImports : schemaImportsCollection)
            {
                for (SchemaImport schemaImport : schemaImports)
                {
                    String schemaLocationURI = schemaImport.getSchemaLocationURI();
                    if (schemaLocationURI != null && !schemaLocationURI.startsWith(basePath))
                    {
                        schemaImport.setSchemaLocationURI(basePath + schemaLocationURI);
                    }
                }
            }

            // Fix schemaLocation values in DOM
            NodeList children = schema.getElement().getChildNodes();
            for (int i = 0; i < children.getLength(); i++)
            {
                Node item = children.item(i);
                if (XML_IMPORT_ELEMENT.equals(item.getLocalName()))
                {
                    NamedNodeMap attributes = item.getAttributes();
                    Node namedItem = attributes.getNamedItem(XML_SCHEMA_LOCATION_ATTRIBUTE);
                    if (namedItem != null)
                    {
                        String schemaLocation = namedItem.getNodeValue();
                        if (!schemaLocation.startsWith(basePath))
                        {
                            namedItem.setNodeValue(basePath + schemaLocation);
                        }
                    }
                }
            }
        }
    }

    private static String getBasePath(String documentURI)
    {
        File document = new File(documentURI);
        if (document.isDirectory())
        {
            return documentURI;
        }

        String fileName = document.getName();
        int fileNameIndex = documentURI.lastIndexOf(fileName);
        if (fileNameIndex == -1)
        {
            return documentURI;
        }

        return documentURI.substring(0, fileNameIndex);
    }


    /**
     * Converts a schema into a String.
     * @throws TransformerException If unable to transform the schema.
     */
    public static String schemaToString(Schema schema) throws TransformerException
    {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(schema.getElement()), new StreamResult(writer));
        return writer.toString();
    }


    /**
     * Retrieves the list of SOAP body parts of a binding operation, or null if there is no
     * SOAP body defined.
     */
    public static List<String> getSoapBodyParts(BindingOperation bindingOperation)
    {
        List extensions = bindingOperation.getBindingInput().getExtensibilityElements();
        List<String> result = null;
        boolean found = false;

        for (Object extension : extensions)
        {
            if (extension instanceof SOAPBody)
            {
                result = ((SOAPBody) extension).getParts();
                found = true;
                break;
            }
            if (extension instanceof SOAP12Body)
            {
                result = ((SOAP12Body) extension).getParts();
                found = true;
                break;
            }
        }

        if (found && result == null)
        {
            result = Collections.emptyList();
        }

        return result;
    }

    /**
     * Retrieves the SOAP version of a WSDL binding, or null if it is not a SOAP binding.
     */
    public static SoapVersion getSoapVersion(Binding binding)
    {
        List extensions = binding.getExtensibilityElements();
        for (Object extension : extensions)
        {
            if (extension instanceof SOAPBinding)
            {
                return SoapVersion.SOAP_11;
            }
            if (extension instanceof SOAP12Binding)
            {
                return SoapVersion.SOAP_12;
            }
        }
        return null;
    }

}
