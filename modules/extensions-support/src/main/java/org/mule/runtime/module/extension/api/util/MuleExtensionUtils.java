/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.api.util;

import static java.util.Collections.singleton;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.module.extension.api.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DISABLE_COMPONENT_IGNORE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.internal.event.NullEventFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;

import java.util.Map;

/**
 * Provides utilities to work with extensions
 *
 * @since 4.0
 */
public class MuleExtensionUtils {

  private MuleExtensionUtils() {}

  /**
   * Loads a extension model
   *
   * @param clazz fully qualified name of the class to load.
   * @return an {@link ExtensionModel} that represents the class being loaded
   * @throws IllegalArgumentException if there are missing entries in {@code attributes} or the type of any of them does not apply
   *                                  to the expected one.
   */
  public static ExtensionModel loadExtension(Class<?> clazz) {
    return loadExtension(clazz, new SmallMap<>());
  }

  /**
   * Loads a extension model
   *
   * @param clazz  fully qualified name of the class to load.
   * @param params a set of attributes to work with in each concrete implementation of {@link ExtensionModelLoader}, which will be
   *               responsible of extracting the mandatory parameters (while casting, if needed).
   * @return an {@link ExtensionModel} that represents the class being loaded
   * @throws IllegalArgumentException if there are missing entries in {@code attributes} or the type of any of them does not apply
   *                                  to the expected one.
   */
  public static ExtensionModel loadExtension(Class<?> clazz, Map<String, Object> params) {
    params.put(TYPE_PROPERTY_NAME, clazz.getName());
    params.put(VERSION, getProductVersion());
    // TODO MULE-11797: as this utils is consumed from
    // org.mule.runtime.module.extension.internal.capability.xml.schema.AbstractXmlResourceFactory.generateResource(org.mule.runtime.api.meta.model.ExtensionModel),
    // this util should get dropped once the ticket gets implemented.
    final DslResolvingContext dslResolvingContext = getDefault(singleton(MuleExtensionModelProvider.getExtensionModel()));
    return new DefaultJavaExtensionModelLoader().loadExtensionModel(clazz.getClassLoader(), dslResolvingContext, params);
  }

  /**
   * Creates an empty event for extension initialization purposes
   *
   * @return a new {@link CoreEvent}
   * @deprecated since 4.3.0. Use {@link NullEventFactory#getNullEvent()} instead
   */
  @Deprecated
  public static CoreEvent getInitialiserEvent() {
    return getNullEvent();
  }

  /**
   * Creates an empty event for extension initialization purposes
   *
   * @param muleContext context on which the event will be associated.
   * @return a new {@link CoreEvent}
   * @deprecated since 4.3.0. Use {@link NullEventFactory#getNullEvent(MuleContext)} instead
   */
  @Deprecated
  public static CoreEvent getInitialiserEvent(MuleContext muleContext) {
    return getNullEvent(muleContext);
  }

  /**
   * Creates a default {@link ExtensionManager}
   *
   * @return a non null {@link ExtensionManager}
   */
  public static ExtensionManager createDefaultExtensionManager() {
    return new DefaultExtensionManager();
  }

  public static boolean isIgnoreDisabled(ExtensionLoadingContext loadingContext) {
    return loadingContext.getParameter(DISABLE_COMPONENT_IGNORE)
        .map(v -> v instanceof Boolean ? (Boolean) v : false)
        .orElse(false);
  }

  /**
   * Determines if the {@code maxItemsPerPoll} parameter must be added to polling sources.
   *
   * @param loadingContext the {@link ExtensionLoadingContext}
   * @return whether the parameter should be added or not.
   * @since 4.4.0
   */
  public static boolean isPollingSourceLimitEnabled(ExtensionLoadingContext loadingContext) {
    return loadingContext.getParameter(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER)
        .map(v -> v instanceof Boolean ? (Boolean) v : false)
        .orElse(false);
  }

}
