/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.extensions.schema.Schema;
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

public class WSDLUtils
{
    private static final String XML_NS_PREFIX = "xmlns:";

    /**
     * Returns all the XML schemas from a WSDL definition.
     * @throws TransformerException If unable to transform a Schema into String.
     */
    public static List<String> getSchemas(Definition wsdlDefinition) throws TransformerException
    {

        Map<String, String> wsdlNamespaces = wsdlDefinition.getNamespaces();

        List<String> schemas = new ArrayList<String>();
        Types types = wsdlDefinition.getTypes();
        if (types != null)
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
