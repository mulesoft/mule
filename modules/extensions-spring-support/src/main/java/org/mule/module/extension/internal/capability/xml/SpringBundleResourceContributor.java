/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.property.XmlModelProperty;
import org.mule.extension.api.resources.ResourcesGenerator;
import org.mule.extension.api.resources.spi.GenerableResourceContributor;
import org.mule.module.extension.internal.capability.xml.schema.SchemaGenerator;
import org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants;
import org.mule.module.extension.internal.config.ExtensionNamespaceHandler;

import java.util.Properties;

/**
 * Implementation of {@link org.mule.extension.api.resources.spi.GenerableResourceContributor}
 * Spring bundles
 *
 * @since 3.7.0
 */
public class SpringBundleResourceContributor implements GenerableResourceContributor
{

    @Override
    public void contribute(ExtensionModel extensionModel, ResourcesGenerator resourcesGenerator)
    {
        XmlModelProperty xmlProperty = extensionModel.getModelProperty(XmlModelProperty.KEY);

        generateSchema(extensionModel, xmlProperty, resourcesGenerator);
        generateSpringBundle(extensionModel, xmlProperty, resourcesGenerator);
    }

    private void generateSchema(ExtensionModel extensionModel, XmlModelProperty xmlProperty, ResourcesGenerator resourcesGenerator)
    {
        String schema = new SchemaGenerator().generate(extensionModel, xmlProperty);
        resourcesGenerator.get(getXsdFileName(extensionModel)).getContentBuilder().append(schema);
    }

    private void generateSpringBundle(ExtensionModel extensionModel, XmlModelProperty xmlProperty, ResourcesGenerator resourcesGenerator)
    {
        writeSpringHandlerBundle(xmlProperty, resourcesGenerator);
        writeSpringSchemaBundle(extensionModel, xmlProperty, resourcesGenerator);
    }

    private void writeSpringHandlerBundle(XmlModelProperty capability, ResourcesGenerator resourcesGenerator)
    {
        String content = String.format("%s=%s", capability.getSchemaLocation(), ExtensionNamespaceHandler.class.getName());
        resourcesGenerator.get("spring.handlers").getContentBuilder().append(springBundleScape(content));
    }

    private void writeSpringSchemaBundle(ExtensionModel extensionModel, XmlModelProperty xmlProperty, ResourcesGenerator resourcesGenerator)
    {
        StringBuilder builder = resourcesGenerator.get("spring.schemas").getContentBuilder();
        builder.append(getSpringSchemaBundle(extensionModel, xmlProperty, xmlProperty.getSchemaVersion()));
        builder.append(getSpringSchemaBundle(extensionModel, xmlProperty, "current"));
    }

    private String getSpringSchemaBundle(ExtensionModel extensionModel, XmlModelProperty xmlProperty, String version)
    {
        String filename = getXsdFileName(extensionModel);
        return springBundleScape(String.format("%s/%s/%s=META-INF/%s\n", xmlProperty.getSchemaLocation(), version, filename, filename));
    }

    private String getXsdFileName(ExtensionModel extensionModel)
    {
        return String.format("mule-%s%s", extensionModel.getName(), SchemaConstants.XSD_EXTENSION);
    }


    /**
     * Colon is a special character for the {@link Properties} class
     * that spring uses to parse the bundle. Thus, such character needs to be escaped
     * with a backslash
     *
     * @param content the content to be escaped
     * @return the escaped content
     */
    private String springBundleScape(String content)
    {
        return content.replaceAll(":", "\\\\:");
    }
}
