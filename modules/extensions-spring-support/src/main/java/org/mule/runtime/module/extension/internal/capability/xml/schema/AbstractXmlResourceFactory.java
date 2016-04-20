/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.XSD_EXTENSION;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;

import java.util.Optional;
import java.util.Properties;

/**
 * Base class for {@link GeneratedResourceFactory} implementations which
 * generate resources needed to support configuring extensions through
 * XML
 *
 * @since 4.0
 */
abstract class AbstractXmlResourceFactory implements GeneratedResourceFactory
{

    /**
     * Tests the given {@code extensionModel} to be enriched with the {@link XmlModelProperty}.
     * If the property is present, then it delegates into {@link #generateXmlResource(ExtensionModel, XmlModelProperty)}.
     * <p>
     * Otherwise, it returns {@link Optional#empty()}
     *
     * @param extensionModel the {@link ExtensionModel} that requires the resource
     * @return an {@link Optional} {@link GeneratedResource}
     */
    @Override
    public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel)
    {
        XmlModelProperty xmlProperty = extensionModel.getModelProperty(XmlModelProperty.class).orElse(null);

        return xmlProperty == null
               ? empty()
               : of(generateXmlResource(extensionModel, xmlProperty));
    }

    /**
     * Delegate method which should contain the actual logic to generate the resource
     *
     * @param extensionModel   the {@link ExtensionModel} that requires the resource
     * @param xmlModelProperty the extension's {@link XmlModelProperty}
     * @return a {@link GeneratedResource}
     */
    protected abstract GeneratedResource generateXmlResource(ExtensionModel extensionModel, XmlModelProperty xmlModelProperty);

    /**
     * Escapes special characters for the {@link Properties} class that Spring uses to parse the bundle.
     *
     * @param content the content to be escaped
     * @return the escaped content
     */
    protected String escape(String content)
    {
        return content.replaceAll(":", "\\\\:");
    }

}
