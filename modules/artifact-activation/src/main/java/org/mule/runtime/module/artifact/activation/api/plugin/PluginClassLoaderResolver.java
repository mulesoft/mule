/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.plugin;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Optionally generates a {@link Supplier} for a plugin class loader given the plugin's {@link ArtifactPluginDescriptor
 * descriptor} and owner artifact's class loader.
 *
 * @since 4.5
 */
public interface PluginClassLoaderResolver
    extends BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, Optional<Supplier<ArtifactClassLoader>>> {

}
