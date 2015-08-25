/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.introspection.capability.XmlCapability;
import org.mule.extension.resources.ResourcesGenerator;
import org.mule.extension.resources.spi.GenerableResourceContributor;
import org.mule.module.extension.internal.capability.xml.schema.SchemaGenerator;
import org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants;
import org.mule.module.extension.internal.config.ExtensionNamespaceHandler;
import org.mule.module.extension.internal.util.CapabilityUtils;

/**
 * Implementation of {@link org.mule.extension.resources.spi.GenerableResourceContributor}
 * Spring bundles
 *
 * @since 3.7.0
 */
public class SpringBundleResourceContributor implements GenerableResourceContributor
{

    @Override
    public void contribute(ExtensionModel extensionModel, ResourcesGenerator resourcesGenerator)
    {
        XmlCapability capability = CapabilityUtils.getSingleCapability(extensionModel, XmlCapability.class);

        generateSchema(extensionModel, capability, resourcesGenerator);
        generateSpringBundle(extensionModel, capability, resourcesGenerator);
    }

    private void generateSchema(ExtensionModel extensionModel, XmlCapability capability, ResourcesGenerator resourcesGenerator)
    {
        String schema = new SchemaGenerator().generate(extensionModel, capability);
        resourcesGenerator.get(getXsdFileName(extensionModel)).getContentBuilder().append(schema);
    }

    private void generateSpringBundle(ExtensionModel extensionModel, XmlCapability capability, ResourcesGenerator resourcesGenerator)
    {
        writeSpringHandlerBundle(capability, resourcesGenerator);
        writeSpringSchemaBundle(extensionModel, capability, resourcesGenerator);
    }

    private void writeSpringHandlerBundle(XmlCapability capability, ResourcesGenerator resourcesGenerator)
    {
        String content = String.format("%s=%s", capability.getSchemaLocation(), ExtensionNamespaceHandler.class.getName());
        resourcesGenerator.get("spring.handlers").getContentBuilder().append(springBundleScape(content));
    }

    private void writeSpringSchemaBundle(ExtensionModel extensionModel, XmlCapability capability, ResourcesGenerator resourcesGenerator)
    {
        StringBuilder builder = resourcesGenerator.get("spring.schemas").getContentBuilder();
        builder.append(getSpringSchemaBundle(extensionModel, capability, capability.getSchemaVersion()));
        builder.append(getSpringSchemaBundle(extensionModel, capability, "current"));
    }

    private String getSpringSchemaBundle(ExtensionModel extensionModel, XmlCapability capability, String version)
    {
        String filename = getXsdFileName(extensionModel);
        return springBundleScape(String.format("%s/%s/%s=META-INF/%s\n", capability.getSchemaLocation(), version, filename, filename));
    }

    private String getXsdFileName(ExtensionModel extensionModel)
    {
        return String.format("mule-%s%s", extensionModel.getName(), SchemaConstants.XSD_EXTENSION);
    }


    /**
     * Colon is a special character for the {@link java.util.Properties} class
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
