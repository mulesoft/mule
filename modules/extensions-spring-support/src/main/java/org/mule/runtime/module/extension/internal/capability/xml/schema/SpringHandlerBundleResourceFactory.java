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
import org.mule.runtime.module.extension.internal.config.ExtensionNamespaceHandler;

/**
 * Generates a Spring bundle file which links the extension's namespace to the {@link ExtensionNamespaceHandler}
 *
 * @since 4.0
 */
public class SpringHandlerBundleResourceFactory extends AbstractXmlResourceFactory
{

    static final String GENERATED_FILE_NAME = "spring.handlers";
    static final String BUNDLE_MASK = "%s=%s\n";

    /**
     * {@inheritDoc}
     */
    @Override
    protected GeneratedResource generateXmlResource(ExtensionModel extensionModel, XmlModelProperty xmlModelProperty)
    {
        String content = String.format(BUNDLE_MASK, xmlModelProperty.getNamespaceUri(), ExtensionNamespaceHandler.class.getName());
        return new GeneratedResource(GENERATED_FILE_NAME, escape(content).getBytes());
    }
}
