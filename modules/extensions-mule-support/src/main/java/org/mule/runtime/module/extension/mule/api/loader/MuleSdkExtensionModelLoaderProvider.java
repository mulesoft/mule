/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.mule.api.loader;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_MULE_SDK_PROPERTY;

import static java.lang.Boolean.getBoolean;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider;
import org.mule.runtime.module.extension.mule.internal.loader.MuleSdkApplicationExtensionModelLoader;
import org.mule.runtime.module.extension.mule.internal.loader.MuleSdkPluginExtensionModelLoader;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides the {@link ExtensionModelLoader} for the Mule SDK
 *
 * @since 4.5.0
 */
@Experimental
public class MuleSdkExtensionModelLoaderProvider implements ExtensionModelLoaderProvider {

  private final boolean isMuleSdkEnabled = getBoolean(ENABLE_MULE_SDK_PROPERTY);

  @Override
  public Set<ExtensionModelLoader> getExtensionModelLoaders() {
    if (isMuleSdkEnabled) {
      Set<ExtensionModelLoader> loaders = new HashSet<>();
      loaders.add(new MuleSdkPluginExtensionModelLoader());
      loaders.add(new MuleSdkApplicationExtensionModelLoader());
      return unmodifiableSet(loaders);
    } else {
      return emptySet();
    }
  }
}
