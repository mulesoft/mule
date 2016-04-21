/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.property.ClassLoaderModelProperty;

/**
 * Adds a {@link ClassLoaderModelProperty} at the {@link ExtensionModel} level, which
 * points to the current {@link Thread}'s context {@link ClassLoader}
 *
 * @since 4.0
 */
public class ClassLoaderModelEnricher implements ModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        describingContext.getExtensionDeclarer().withModelProperty(new ClassLoaderModelProperty(Thread.currentThread().getContextClassLoader()));
    }
}
