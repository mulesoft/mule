/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.base;

import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_SDK_IGNORE_COMPONENT;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_SDK_POLLING_SOURCE_LIMIT;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DISABLE_COMPONENT_IGNORE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.base.validator.ConfigurationModelValidator;
import org.mule.runtime.module.extension.internal.loader.base.validator.ConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.base.validator.DeprecationModelValidator;
import org.mule.runtime.module.extension.internal.loader.base.validator.ParameterPluralNameModelValidator;

import java.util.List;

public class DefaultExtensionModelLoader extends ExtensionModelLoader {

  private static final String DEFAULT_LOADER_ID = "default";

  private static final boolean IGNORE_DISABLED = getProperty(DISABLE_SDK_IGNORE_COMPONENT) != null;
  private static final boolean ENABLE_POLLING_SOURCE_LIMIT = getProperty(ENABLE_SDK_POLLING_SOURCE_LIMIT) != null;

  public static final String VERSION = "version";

  private final List<ExtensionModelValidator> validators = unmodifiableList(asList(
      new ConfigurationModelValidator(),
      new ConnectionProviderModelValidator(),
      new DeprecationModelValidator(),
      new ParameterPluralNameModelValidator()));

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId() {
    return DEFAULT_LOADER_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    context.addCustomValidators(validators);

    if (IGNORE_DISABLED) {
      context.addParameter(DISABLE_COMPONENT_IGNORE, true);
    }
    if (ENABLE_POLLING_SOURCE_LIMIT) {
      context.addParameter(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    ExtensionElement extensionType = getExtensionType(context);
    String version =
        context.<String>getParameter(VERSION).orElseThrow(() -> new IllegalArgumentException("version not specified"));
    factory.getLoader(extensionType, version).declare(context);
  }
}
