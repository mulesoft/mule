/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml;

import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.extensions.resources.ResourcesGenerator;
import org.mule.extensions.resources.spi.GenerableResourceContributor;
import org.mule.module.extensions.internal.capability.xml.schema.SchemaGenerator;
import org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants;
import org.mule.module.extensions.internal.config.ExtensionsNamespaceHandler;

import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation of {@link org.mule.extensions.resources.spi.GenerableResourceContributor}
 * spring bundles
 *
 * @since 3.7.0
 */
public class SpringBundleResourceContributor implements GenerableResourceContributor
{

    @Override
    public void contribute(Extension extension, ResourcesGenerator resourcesGenerator)
    {
        Collection<XmlCapability> capabilities = extension.getCapabilities(XmlCapability.class);
        if (CollectionUtils.isEmpty(capabilities))
        {
            return;
        }

        XmlCapability capability = capabilities.iterator().next();

        generateSchema(extension, capability, resourcesGenerator);
        generateSpringBundle(extension, capability, resourcesGenerator);
    }

    private void generateSchema(Extension extension, XmlCapability capability, ResourcesGenerator resourcesGenerator)
    {
        String schema = new SchemaGenerator().generate(extension, capability);
        resourcesGenerator.getOrCreateResource(getXsdFileName(extension)).getContentBuilder().append(schema);
    }

    private void generateSpringBundle(Extension extension, XmlCapability capability, ResourcesGenerator resourcesGenerator)
    {
        writeSpringHandlerBundle(capability, resourcesGenerator);
        writeSpringSchemaBundle(extension, capability, resourcesGenerator);
    }

    private void writeSpringHandlerBundle(XmlCapability capability, ResourcesGenerator resourcesGenerator)
    {
        String content = String.format("%s=%s", capability.getSchemaLocation(), ExtensionsNamespaceHandler.class.getName());
        resourcesGenerator.getOrCreateResource("spring.handlers").getContentBuilder().append(springBundleScape(content));
    }

    private void writeSpringSchemaBundle(Extension extension, XmlCapability capability, ResourcesGenerator resourcesGenerator)
    {
        StringBuilder builder = resourcesGenerator.getOrCreateResource("spring.schemas").getContentBuilder();
        builder.append(getSpringSchemaBundle(extension, capability, capability.getSchemaVersion()));
        builder.append(getSpringSchemaBundle(extension, capability, "current"));
    }

    private String getSpringSchemaBundle(Extension extension, XmlCapability capability, String version)
    {
        String filename = getXsdFileName(extension);
        return springBundleScape(String.format("%s/%s/%s=META-INF/%s\n", capability.getSchemaLocation(), version, filename, filename));
    }

    private String getXsdFileName(Extension extension)
    {
        return String.format("mule-extension-%s%s", extension.getName(), SchemaConstants.XSD_EXTENSION);
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
