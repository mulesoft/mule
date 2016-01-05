/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import org.mule.extension.annotation.api.capability.Xml;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.property.XmlModelProperty;
import org.mule.module.extension.internal.model.AbstractAnnotatedModelEnricher;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.module.extension.internal.util.NameUtils;
import org.mule.util.StringUtils;

import java.util.function.Supplier;

/**
 * Verifies if the extension is annotated with {@link Xml} and if so, enriches the {@link DeclarationDescriptor}
 * with a model property which key is {@value XmlModelProperty#KEY} and the value an instance of {@link XmlModelProperty}.
 * <p/>
 * To get a hold of the {@link Class} on which the {@link Xml} annotation is expected to be, the
 * {@link DescribingContext} will be queried for a model property of key {@link ImplementingTypeModelProperty#KEY}
 * and type {@link ImplementingTypeModelProperty}. If such property is not found, then this enricher will return
 * without any side effects
 *
 * @since 4.0
 */
public final class XmlModelEnricher extends AbstractAnnotatedModelEnricher
{

    private static final String DEFAULT_SCHEMA_LOCATION_MASK = "http://www.mulesoft.org/schema/mule/%s";

    @Override
    public void enrich(DescribingContext describingContext)
    {
        Xml xml = extractAnnotation(describingContext.getDeclarationDescriptor().getDeclaration(), Xml.class);
        DeclarationDescriptor descriptor = describingContext.getDeclarationDescriptor();
        descriptor.withModelProperty(XmlModelProperty.KEY, createXmlModelProperty(xml, descriptor));
    }

    private XmlModelProperty createXmlModelProperty(Xml xml, DeclarationDescriptor descriptor)
    {
        Declaration declaration = descriptor.getDeclaration();
        String schemaVersion = calculateValue(xml, () -> xml.schemaVersion(), declaration::getVersion);
        String namespace = calculateValue(xml, () -> xml.namespace(), () -> buildDefaultNamespace(declaration.getName()));
        String schemaLocation = calculateValue(xml, () -> xml.schemaLocation(), () -> buildDefaultLocation(namespace));
        return new ImmutableXmlModelProperty(schemaVersion, namespace, schemaLocation);
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
