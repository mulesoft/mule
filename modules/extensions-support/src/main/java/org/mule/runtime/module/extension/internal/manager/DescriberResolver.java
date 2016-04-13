/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.DESCRIBER_ID;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class used to obtain the {@link Describer} instance which should be
 * used to obtain the {@link ExtensionDeclarer} of a given {@link ExtensionManifest}.
 * <p>
 * This class is thread-safe and reusable.
 *
 * @since 4.0
 */
final class DescriberResolver
{

    private final Map<String, DescriberResolverDelegate> delegates;

    DescriberResolver()
    {
        delegates = ImmutableMap.<String, DescriberResolverDelegate>builder()
                .put(DESCRIBER_ID, createAnnotationDescriberDelegate())
                .build();
    }

    /**
     * Returns the describer that corresponds to the given {@code manifest}
     *
     * @param manifest    a {@link ExtensionManifest}
     * @param classLoader a {@link ClassLoader} with visibility to the extension's classes
     * @return a {@link Describer} capable of describing the {@link ExtensionModel} that the {@code manifest} references
     */
    Describer resolve(ExtensionManifest manifest, ClassLoader classLoader)
    {
        DescriberResolverDelegate delegate = delegates.get(manifest.getDescriberManifest().getId());
        if (delegate == null)
        {
            throw new IllegalArgumentException(String.format("Manifest for extension '%s' references describer '%s' which is not supported", manifest.getName(), manifest.getDescriberManifest().getId()));
        }

        return delegate.resolve(manifest, classLoader);
    }

    private DescriberResolverDelegate createAnnotationDescriberDelegate()
    {
        return (manifest, classLoader) -> {

            String type = manifest.getDescriberManifest().getProperties().get(AnnotationsBasedDescriber.TYPE_PROPERTY_NAME);
            if (StringUtils.isBlank(type))
            {
                throw new IllegalArgumentException();
            }

            Class<?> extensionType;
            try
            {
                extensionType = ClassUtils.loadClass(type, classLoader);
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException();
            }

            return new AnnotationsBasedDescriber(extensionType, new StaticVersionResolver(manifest.getVersion()));
        };
    }

    @FunctionalInterface
    private interface DescriberResolverDelegate
    {

        Describer resolve(ExtensionManifest manifest, ClassLoader classLoader);
    }
}
