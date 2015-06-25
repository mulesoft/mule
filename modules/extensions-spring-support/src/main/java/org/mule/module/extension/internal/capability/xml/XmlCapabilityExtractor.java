/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import static org.apache.commons.lang.StringUtils.isBlank;
import org.mule.extension.annotations.capability.Xml;
import org.mule.extension.introspection.capability.XmlCapability;
import org.mule.extension.introspection.declaration.fluent.Declaration;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.module.extension.CapabilityExtractor;

/**
 * Implementation of {@link CapabilityExtractor}
 * that verifies if the extension is annotated with {@link org.mule.extension.introspection.capability.XmlCapability}
 * and if so, registers into the builder a {@link org.mule.extension.introspection.capability.XmlCapability
 *
 * @since 3.7.0
 */
public class XmlCapabilityExtractor implements CapabilityExtractor
{

    public static final String DEFAULT_SCHEMA_LOCATION_MASK = "http://www.mulesoft.org/schema/mule/extension/%s";

    @Override
    public Object extractCapability(DeclarationDescriptor declarationDescriptor, Class<?> capableType)
    {
        Xml xml = capableType.getAnnotation(Xml.class);
        return xml != null ? processCapability(xml, declarationDescriptor) : null;
    }

    private XmlCapability processCapability(Xml xml, DeclarationDescriptor descriptor)
    {
        Declaration declaration = descriptor.getDeclaration();
        String schemaVersion = isBlank(xml.schemaVersion()) ? declaration.getVersion() : xml.schemaVersion();
        String schemaLocation = isBlank(xml.schemaLocation()) ? buildDefaultLocation(declaration) : xml.schemaLocation();

        return new ImmutableXmlCapability(schemaVersion, xml.namespace(), schemaLocation);
    }

    private String buildDefaultLocation(Declaration declaration)
    {
        return String.format(DEFAULT_SCHEMA_LOCATION_MASK, declaration.getName());
    }

}
