/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import static org.apache.commons.lang.StringUtils.isBlank;
import org.mule.extension.annotations.capability.Xml;
import org.mule.extension.introspection.declaration.DescribingContext;
import org.mule.extension.introspection.declaration.fluent.Declaration;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.introspection.property.XmlModelProperty;
import org.mule.module.extension.internal.model.AbstractAnnotatedModelEnricher;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;

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

    public static final String DEFAULT_SCHEMA_LOCATION_MASK = "http://www.mulesoft.org/schema/mule/extension/%s";

    @Override
    public void enrich(DescribingContext describingContext)
    {
        Xml xml = extractAnnotation(describingContext.getDeclarationDescriptor().getDeclaration(), Xml.class);
        if (xml != null)
        {
            DeclarationDescriptor descriptor = describingContext.getDeclarationDescriptor();
            descriptor.withModelProperty(XmlModelProperty.KEY, createXmlModelProperty(xml, descriptor));
        }
    }

    private XmlModelProperty createXmlModelProperty(Xml xml, DeclarationDescriptor descriptor)
    {
        Declaration declaration = descriptor.getDeclaration();
        String schemaVersion = isBlank(xml.schemaVersion()) ? declaration.getVersion() : xml.schemaVersion();
        String schemaLocation = isBlank(xml.schemaLocation()) ? buildDefaultLocation(declaration) : xml.schemaLocation();

        return new ImmutableXmlModelProperty(schemaVersion, xml.namespace(), schemaLocation);
    }

    private String buildDefaultLocation(Declaration declaration)
    {
        return String.format(DEFAULT_SCHEMA_LOCATION_MASK, declaration.getName());
    }
}
