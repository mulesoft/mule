/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml;

import static org.apache.commons.lang.StringUtils.isBlank;
import org.mule.extensions.annotations.capability.Xml;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.extensions.introspection.declaration.Construct;
import org.mule.extensions.introspection.declaration.Declaration;
import org.mule.extensions.introspection.declaration.DeclarationConstruct;
import org.mule.extensions.introspection.declaration.HasCapabilities;
import org.mule.module.extensions.CapabilityExtractor;

/**
 * Implementation of {@link CapabilityExtractor}
 * that verifies if the extension is annotated with {@link org.mule.extensions.introspection.capability.XmlCapability}
 * and if so, registers into the builder a {@link org.mule.extensions.introspection.capability.XmlCapability
 *
 * @since 3.7.0
 */
public class XmlCapabilityExtractor implements CapabilityExtractor
{

    public static final String DEFAULT_SCHEMA_LOCATION_MASK = "http://www.mulesoft.org/schema/mule/extension/%s";

    @Override
    public Object extractCapability(DeclarationConstruct declaration, Class<?> capableType, HasCapabilities<? extends Construct> capableCallback)
    {
        Xml xml = capableType.getAnnotation(Xml.class);
        if (xml != null)
        {
            XmlCapability capability = processCapability(xml, declaration);
            capableCallback.withCapability(capability);

            return capability;
        }

        return null;
    }

    private XmlCapability processCapability(Xml xml, DeclarationConstruct construct)
    {

        Declaration declaration = construct.getDeclaration();
        String schemaVersion = isBlank(xml.schemaVersion()) ? declaration.getVersion() : xml.schemaVersion();
        String schemaLocation = isBlank(xml.schemaLocation()) ? buildDefaultLocation(declaration) : xml.schemaLocation();

        return new ImmutableXmlCapability(schemaVersion, xml.namespace(), schemaLocation);
    }

    private String buildDefaultLocation(Declaration declaration)
    {
        return String.format(DEFAULT_SCHEMA_LOCATION_MASK, declaration.getName());
    }

}
