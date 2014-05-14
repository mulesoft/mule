/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml.schema;

import static org.mule.util.Preconditions.checkArgument;
import static org.mule.util.Preconditions.checkState;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.Configuration;
import org.mule.extensions.introspection.Operation;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.module.extensions.internal.capability.xml.schema.model.NamespaceFilter;
import org.mule.module.extensions.internal.capability.xml.schema.model.Schema;
import org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.StringUtils;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * Generator class that takes a {@link Extension}
 * with a {@link org.mule.extensions.introspection.capability.XmlCapability} and returns
 * a XSD schema as a String
 *
 * @since 3.7.0
 */
public class SchemaGenerator
{

    private void validate(Extension extension, XmlCapability xmlCapability)
    {
        checkArgument(extension != null, "extension cannot be null");
        checkArgument(xmlCapability != null, "capability cannot be null");
        checkState(!StringUtils.isBlank(xmlCapability.getNamespace()), "capability can't provide a blank namespace");
    }

    public String generate(Extension extension, XmlCapability xmlCapability)
    {
        validate(extension, xmlCapability);
        SchemaBuilder schemaBuilder = SchemaBuilder.newSchema(xmlCapability.getSchemaLocation());

        for (Configuration configuration : extension.getConfigurations())
        {
            schemaBuilder.registerConfigElement(configuration);
        }

        for (Operation operation : extension.getOperations())
        {
            schemaBuilder.registerOperation(operation);
        }

        schemaBuilder.registerEnums();

        return renderSchema(schemaBuilder.getSchema());
    }

    private String renderSchema(Schema schema)
    {
        try
        {
            JAXBContext jaxbContext = JAXBContext.newInstance(Schema.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            NamespaceFilter outFilter = new NamespaceFilter("mule", SchemaConstants.MULE_NAMESPACE, true);
            OutputFormat format = new OutputFormat();
            format.setIndent(true);
            format.setNewlines(true);

            StringWriter sw = new StringWriter();
            XMLWriter writer = new XMLWriter(sw, format);
            outFilter.setContentHandler(writer);
            marshaller.marshal(schema, outFilter);
            return sw.toString();
        }
        catch (JAXBException e)
        {
            throw new RuntimeException(e);
        }

    }
}