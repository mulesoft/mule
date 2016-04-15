/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import org.mule.extension.api.annotation.capability.Xml;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.extension.api.introspection.property.XmlModelProperty;
import org.mule.module.extension.internal.introspection.enricher.AbstractAnnotatedModelEnricher;
import org.mule.module.extension.internal.util.NameUtils;
import org.mule.runtime.core.util.StringUtils;

import java.util.function.Supplier;

/**
 * Verifies if the extension is annotated with {@link Xml} and if so, enriches the {@link ExtensionDeclarer}
 * with a {@link XmlModelProperty}.
 * <p>
 * To get a hold of the {@link Class} on which the {@link Xml} annotation is expected to be, the
 * {@link DescribingContext} will be queried for such a model property. If such property is not present,
 * then this enricher will return without any side effects
 *
 * @since 4.0
 */
public final class XmlModelEnricher extends AbstractAnnotatedModelEnricher
{

    private static final String DEFAULT_SCHEMA_LOCATION_MASK = "http://www.mulesoft.org/schema/mule/%s";

    @Override
    public void enrich(DescribingContext describingContext)
    {
        Xml xml = extractAnnotation(describingContext.getExtensionDeclarer().getExtensionDeclaration(), Xml.class);
        ExtensionDeclarer descriptor = describingContext.getExtensionDeclarer();
        descriptor.withModelProperty(createXmlModelProperty(xml, descriptor));
    }

    private XmlModelProperty createXmlModelProperty(Xml xml, ExtensionDeclarer descriptor)
    {
        ExtensionDeclaration extensionDeclaration = descriptor.getExtensionDeclaration();
        String schemaVersion = calculateValue(xml, () -> xml.schemaVersion(), extensionDeclaration::getVersion);
        String namespace = calculateValue(xml, () -> xml.namespace(), () -> buildDefaultNamespace(extensionDeclaration.getName()));
        String schemaLocation = calculateValue(xml, () -> xml.schemaLocation(), () -> buildDefaultLocation(namespace));
        return new XmlModelProperty(schemaVersion, namespace, schemaLocation);
    }

    private String calculateValue(Xml xml, Supplier<String> value, Supplier<String> fallback)
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

    private String buildDefaultLocation(String namespace)
    {
        return String.format(DEFAULT_SCHEMA_LOCATION_MASK, namespace);
    }

    private String buildDefaultNamespace(String declarationName)
    {
        String namespace = StringUtils.deleteWhitespace(declarationName);
        namespace = removeFromName(namespace, "extension");
        namespace = removeFromName(namespace, "connector");
        namespace = removeFromName(namespace, "module");
        namespace = StringUtils.isBlank(namespace) ? declarationName : namespace;
        return NameUtils.hyphenize(namespace);

    }

    private String removeFromName(String name, String word)
    {
        return StringUtils.removeEndIgnoreCase(name, word);
    }

}
