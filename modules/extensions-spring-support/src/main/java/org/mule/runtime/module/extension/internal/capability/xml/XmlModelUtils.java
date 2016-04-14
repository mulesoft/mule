/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.CURRENT_VERSION;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.DEFAULT_SCHEMA_LOCATION_MASK;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.XSD_EXTENSION;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.annotation.capability.Xml;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import org.mule.runtime.module.extension.internal.util.NameUtils;

import java.util.function.Supplier;

/**
 * Utils class for parsing and generation of Xml related values of an {@link ExtensionModel extension}
 *
 * @since 4.0
 */
public final class XmlModelUtils
{
    public static XmlModelProperty createXmlModelProperty(Xml xml, String extensionName, String extensionVersion)
    {

        String namespace = calculateValue(xml, () -> xml.namespace(), () -> buildDefaultNamespace(extensionName));
        String namespaceLocation = calculateValue(xml, () -> xml.namespaceLocation(), () -> buildDefaultLocation(namespace));
        String xsdFileName = buildDefaultXsdFileName(namespace);
        String schemaLocation = buildDefaultSchemaLocation(namespaceLocation, xsdFileName);

        return new XmlModelProperty(extensionVersion, namespace, namespaceLocation, xsdFileName, schemaLocation);
    }

    private static String calculateValue(Xml xml, Supplier<String> value, Supplier<String> fallback)
    {
        if (xml != null)
        {
            String result = value.get();
            if (StringUtils.isNotBlank(result))
            {
                return result;
            }
        }
        return fallback.get();
    }

    private static String buildDefaultLocation(String namespace)
    {
        return String.format(DEFAULT_SCHEMA_LOCATION_MASK, namespace);
    }

    private static String buildDefaultNamespace(String declarationName)
    {
        String namespace = StringUtils.deleteWhitespace(declarationName);
        namespace = removeFromName(namespace, "extension");
        namespace = removeFromName(namespace, "connector");
        namespace = removeFromName(namespace, "module");
        namespace = StringUtils.isBlank(namespace) ? declarationName : namespace;
        return NameUtils.hyphenize(namespace);

    }

    private static String buildDefaultXsdFileName(String namespace)
    {
        return String.format("mule-%s%s", namespace, XSD_EXTENSION);
    }

    private static String buildDefaultSchemaLocation(String namespaceLocation, String xsdFileName)
    {
        return String.format("%s/%s/%s", namespaceLocation, CURRENT_VERSION, xsdFileName);
    }

    private static String removeFromName(String name, String word)
    {
        return StringUtils.removeEndIgnoreCase(name, word);
    }

}
