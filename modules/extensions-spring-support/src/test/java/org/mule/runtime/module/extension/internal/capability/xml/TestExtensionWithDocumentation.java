/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.capability.Xml;

@Extension(name = "documentation")
@Operations({TestDocumentedExtensionOperations.class})
@Xml(namespaceLocation = "namespaceLocation", namespace = "documentation")
public class TestExtensionWithDocumentation
{

    /**
     * Config parameter
     */
    @Parameter
    private String configParameter;

    @ParameterGroup
    private TestDocumentedParameterGroup group;
}
