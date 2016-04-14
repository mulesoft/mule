/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import org.mule.runtime.extension.api.annotation.capability.Xml;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import static org.mule.runtime.module.extension.internal.capability.xml.XmlModelUtils.createXmlModelProperty;
import org.mule.runtime.module.extension.internal.introspection.enricher.AbstractAnnotatedModelEnricher;

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

    @Override
    public void enrich(DescribingContext describingContext)
    {
        Xml xml = extractAnnotation(describingContext.getExtensionDeclarer().getExtensionDeclaration(), Xml.class);
        ExtensionDeclarer descriptor = describingContext.getExtensionDeclarer();
        ExtensionDeclaration extensionDeclaration = descriptor.getExtensionDeclaration();
        descriptor.withModelProperty(createXmlModelProperty(xml, extensionDeclaration.getName(), extensionDeclaration.getVersion()));
    }

}
