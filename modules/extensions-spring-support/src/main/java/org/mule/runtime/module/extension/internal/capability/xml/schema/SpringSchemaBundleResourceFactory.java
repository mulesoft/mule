/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import org.mule.runtime.extension.api.resources.GeneratedResource;

/**
 * Generates a Spring bundle file which links the extension's namespace to its schema file
 *
 * @since 4.0
 */
public class SpringSchemaBundleResourceFactory extends AbstractXmlResourceFactory
{

    static final String GENERATED_FILE_NAME = "spring.schemas";
    static final String BUNDLE_MASK = "%s/%s/%s=META-INF/%s\n";

    /**
     * {@inheritDoc}
     */
    @Override
    protected GeneratedResource generateXmlResource(ExtensionModel extensionModel, XmlModelProperty xmlModelProperty)
    {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(getSpringSchemaBundle(xmlModelProperty, xmlModelProperty.getSchemaVersion()));
        contentBuilder.append(getSpringSchemaBundle(xmlModelProperty, "current"));

        return new GeneratedResource(GENERATED_FILE_NAME, contentBuilder.toString().getBytes());
    }

    private String getSpringSchemaBundle(XmlModelProperty xmlProperty, String version)
    {
        String filename = xmlProperty.getXsdFileName();
        return escape(String.format(BUNDLE_MASK, xmlProperty.getNamespaceUri(), version, filename, filename));
    }
}
