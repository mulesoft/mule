/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml.schema;

import static org.mule.util.Preconditions.checkArgument;
import static org.mule.util.Preconditions.checkState;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.property.XmlModelProperty;
import org.mule.module.extension.internal.capability.xml.schema.model.NamespaceFilter;
import org.mule.module.extension.internal.capability.xml.schema.model.Schema;
import org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.StringUtils;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * Generator class that takes a {@link ExtensionModel}
 * with a {@link XmlModelProperty} and returns
 * a XSD schema as a String
 *
 * @since 3.7.0
 */
public class SchemaGenerator
{

    private void validate(ExtensionModel extensionModel, XmlModelProperty xmlModelProperty)
    {
        checkArgument(extensionModel != null, "extension cannot be null");
        checkArgument(xmlModelProperty != null, "xml model property cannot be null");
        checkState(!StringUtils.isBlank(xmlModelProperty.getNamespace()), "xml model property cannot provide a blank namespace");
    }

    public String generate(ExtensionModel extensionModel, XmlModelProperty xmlModelProperty)
    {
        validate(extensionModel, xmlModelProperty);
        SchemaBuilder schemaBuilder = SchemaBuilder.newSchema(xmlModelProperty.getSchemaLocation());

        extensionModel.getConfigurationModels().forEach(schemaBuilder::registerConfigElement);
        extensionModel.getOperationModels().forEach(schemaBuilder::registerOperation);
        extensionModel.getConnectionProviders().forEach(schemaBuilder::registerConnectionProviderElement);

        schemaBuilder.registerEnums();

        return renderSchema(schemaBuilder.build());
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