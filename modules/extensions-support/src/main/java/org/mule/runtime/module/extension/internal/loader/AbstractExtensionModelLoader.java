/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_SDK_IGNORE_COMPONENT;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_SDK_POLLING_SOURCE_LIMIT;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ADD_ANNOTATIONS_TO_CONFIG_CLASS;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DISABLE_COMPONENT_IGNORE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import org.mule.runtime.extension.api.loader.AbstractParserBasedExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;

import java.util.Optional;

/**
 * Base implementation for an {@link ExtensionModelLoader}
 *
 * @since 4.5.0
 */
public abstract class AbstractExtensionModelLoader extends AbstractParserBasedExtensionModelLoader {

  private static boolean IGNORE_DISABLED = getProperty(DISABLE_SDK_IGNORE_COMPONENT) != null;
  private static final boolean ENABLE_POLLING_SOURCE_LIMIT = getProperty(ENABLE_SDK_POLLING_SOURCE_LIMIT) != null;

  /**
   * As part of our effort to remove the powermock dependency for the Java 17 upgrade("W-13116494"), we have temporarily
   * introduced this setter for test use only. We will need to refactor the class so that the field does not have to be static.
   *
   * @param value // TODO: W-13510775 Refactor AbstractExtensionModelLoader to make IGNORE_DISABLED not static
   */
  public static void setIgnoreDisabled(boolean value) {
    IGNORE_DISABLED = value;
  }

  public static boolean getIgnoreDisabled() {
    return IGNORE_DISABLED;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    Optional<Object> disableComponentIgnore = checkBoolean(context, DISABLE_COMPONENT_IGNORE);
    if (IGNORE_DISABLED && !disableComponentIgnore.isPresent()) {
      context.addParameter(DISABLE_COMPONENT_IGNORE, true);
    }

    if (ENABLE_POLLING_SOURCE_LIMIT) {
      context.addParameter(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
    }

    Optional<Object> addAnnotationstoConfigClass = checkBoolean(context, ADD_ANNOTATIONS_TO_CONFIG_CLASS);
    if (addAnnotationstoConfigClass.isPresent()) {
      context.addParameter(ADD_ANNOTATIONS_TO_CONFIG_CLASS, addAnnotationstoConfigClass.get());
    }
  }

  private Optional<Object> checkBoolean(ExtensionLoadingContext context, String paramMame) {
    Optional<Object> addAnnotationstoConfigClass = context.getParameter(paramMame);
    addAnnotationstoConfigClass
        .ifPresent(value -> checkState(value instanceof Boolean,
                                       format("Property value for %s expected to be boolean", paramMame)));
    return addAnnotationstoConfigClass;
  }

  protected ModelLoaderDelegate getModelLoaderDelegate(ExtensionLoadingContext context, String version) {
    // We can skip the SPI lookup of the parent class because we have direct access to the impl we need.
    return new DefaultExtensionModelLoaderDelegate(version, getId());
  }
}
