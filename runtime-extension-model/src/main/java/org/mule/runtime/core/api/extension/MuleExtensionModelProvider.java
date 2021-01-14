/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension;

import static java.lang.String.format;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

import java.io.IOException;
import java.util.Properties;

/**
 * Utility class to access the {@link ExtensionModel} definition for Mule's Runtime
 *
 * @since 4.0
 */
public final class MuleExtensionModelProvider {

  public static final String MULE_NAME = CORE_PREFIX;
  public static final String MULE_VERSION;
  public static final String MULESOFT_VENDOR = "MuleSoft, Inc.";

  public static final String MULE_TLS_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "tls");
  public static final String MULE_TLS_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/tls/current/mule-tls.xsd";

  static {
    try {
      final Properties buildProperties = new Properties();
      buildProperties.load(MuleExtensionModelDeclarer.class.getResourceAsStream("/build.properties"));
      MULE_VERSION = buildProperties.getProperty("muleVersion", "4.0.0");
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private static final LazyValue<ExtensionModel> EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(new DefaultExtensionLoadingContext(new MuleExtensionModelDeclarer().createExtensionModel(),
                                                 MuleExtensionModelProvider.class.getClassLoader(),
                                                 new NullDslResolvingContext())));

  private static final LazyValue<ExtensionModel> TLS_EXTENSION_MODEL = new LazyValue<>(() -> new ExtensionModelFactory()
      .create(new DefaultExtensionLoadingContext(new TlsExtensionModelDeclarer().createExtensionModel(),
                                                 MuleExtensionModelProvider.class.getClassLoader(),
                                                 new NullDslResolvingContext())));

  /**
   * @return the {@link ExtensionModel} definition for Mule's Runtime
   */
  public static ExtensionModel getExtensionModel() {
    return EXTENSION_MODEL.get();
  }

  /**
   * @return the {@link ExtensionModel} definition containing the namespace declaration for the tls module.
   */
  public static ExtensionModel getTlsExtensionModel() {
    return TLS_EXTENSION_MODEL.get();
  }
}
