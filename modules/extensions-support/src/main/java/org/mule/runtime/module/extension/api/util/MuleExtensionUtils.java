/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.api.util;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.util.UUID.getUUID;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.internal.management.stats.DefaultFlowConstructStatistics;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Provides utilities to work with extensions
 *
 * @since 4.0
 */
public class MuleExtensionUtils {

  /**
   * Loads a extension model
   *
   * @param clazz fully qualified name of the class to load.
   * @return an {@link ExtensionModel} that represents the class being loaded
   * @throws IllegalArgumentException if there are missing entries in {@code attributes} or the type of any of them does not apply
   *         to the expected one.
   */
  public static ExtensionModel loadExtension(Class<?> clazz) {
    return loadExtension(clazz, new HashMap<>());
  }

  /**
   * Loads a extension model
   *
   * @param clazz fully qualified name of the class to load.
   * @param params a set of attributes to work with in each concrete implementation of {@link ExtensionModelLoader}, which will be
   *        responsible of extracting the mandatory parameters (while casting, if needed).
   * @return an {@link ExtensionModel} that represents the class being loaded
   * @throws IllegalArgumentException if there are missing entries in {@code attributes} or the type of any of them does not apply
   *         to the expected one.
   */
  public static ExtensionModel loadExtension(Class<?> clazz, Map<String, Object> params) {
    params.put(TYPE_PROPERTY_NAME, clazz.getName());
    params.put(VERSION, getProductVersion());
    // TODO MULE-11797: as this utils is consumed from
    // org.mule.runtime.module.extension.internal.capability.xml.schema.AbstractXmlResourceFactory.generateResource(org.mule.runtime.api.meta.model.ExtensionModel),
    // this util should get dropped once the ticket gets implemented.
    final DslResolvingContext dslResolvingContext = getDefault(emptySet());
    return new DefaultJavaExtensionModelLoader().loadExtensionModel(clazz.getClassLoader(), dslResolvingContext, params);
  }

  /**
   * Creates an empty event for extension initialization purposes
   *
   * @return a new {@link CoreEvent}
   */
  public static CoreEvent getInitialiserEvent() {
    return getInitialiserEvent(null);
  }

  /**
   * Creates an empty event for extension initialization pusposes
   *
   * @param muleContext context on which the event will be associated.
   * @return a new {@link CoreEvent}
   */
  public static CoreEvent getInitialiserEvent(MuleContext muleContext) {
    FlowConstruct flowConstruct = new FlowConstruct() {

      @Override
      public Object getAnnotation(QName name) {
        return null;
      }

      @Override
      public Map<QName, Object> getAnnotations() {
        return emptyMap();
      }

      @Override
      public void setAnnotations(Map<QName, Object> annotations) {}

      @Override
      public ComponentLocation getLocation() {
        return null;
      }

      @Override
      public String getRootContainerName() {
        return null;
      }
      // TODO MULE-9076: This is only needed because the muleContext is get from the given flow.

      @Override
      public MuleContext getMuleContext() {
        return muleContext;
      }

      @Override
      public String getServerId() {
        return "InitialiserServer";
      }

      @Override
      public String getUniqueIdString() {
        return getUUID();
      }

      @Override
      public String getName() {
        return "InitialiserEventFlow";
      }

      @Override
      public LifecycleState getLifecycleState() {
        return null;
      }

      @Override
      public FlowExceptionHandler getExceptionListener() {
        return null;
      }

      @Override
      public DefaultFlowConstructStatistics getStatistics() {
        return null;
      }
    };
    return InternalEvent.builder(create(flowConstruct, fromSingleComponent("InitializerEvent"))).message(of(null))
        .build();
  }

  /**
   * Creates a default {@link ExtensionManager}
   *
   * @return a non null {@link ExtensionManager}
   */
  public static ExtensionManager createDefaultExtensionManager() {
    return new DefaultExtensionManager();
  }

}
