/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static org.mule.runtime.core.api.util.ClassUtils.getFieldValue;
import static org.mule.runtime.core.api.util.ClassUtils.getStaticFieldValue;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

/**
 * A utility class to release all resources associated to Groovy Dependency on un-deployment to prevent classloader leaks.
 *
 * @since 4.4.0
 * @deprecated Since 4.5.0, this releaser has been deprecated in favor of an {@link ArtifactLifecycleListener} in the extensions
 *             that are using the Groovy scripting engine. We still keep it to support legacy extensions.
 */
@Deprecated
public class GroovyResourceReleaser implements ResourceReleaser {

  private final ClassLoader classLoader;
  private static final Logger LOGGER = getLogger(GroovyResourceReleaser.class);
  private static final String GROOVY_CLASS_INFO = "org.codehaus.groovy.reflection.ClassInfo";
  private static final String GROOVY_INVOKER_HELPER = "org.codehaus.groovy.runtime.InvokerHelper";

  private static final String GROOVY_SCRIPT_ENGINE_FACTORY = "org.codehaus.groovy.jsr223.GroovyScriptEngineFactory";
  private static final String LOGGER_ABSTRACT_MANAGER = "org.apache.logging.log4j.core.appender.AbstractManager";
  private static final String LOGGER_STREAM_MANAGER = "org.apache.logging.log4j.core.appender.OutputStreamManager";
  private static final String LOGGER_CONFIGURATION = "org.apache.logging.log4j.core.config.Configuration";

  /**
   * Creates a new Instance of GroovyResourceReleaser
   *
   * @param classLoader ClassLoader which loaded the Groovy Dependency.
   */
  public GroovyResourceReleaser(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void release() {
    unregisterInvokerHelper();
    cleanSpisEngines();
  }

  private void unregisterAllClassesFromInvokerHelper() {
    try {
      Class<?> classInfoClass = this.classLoader.loadClass(GROOVY_CLASS_INFO);
      Method getAllClassInfoMethod = classInfoClass.getMethod("getAllClassInfo");
      Method getTheClassMethod = classInfoClass.getMethod("getTheClass");
      Class<?> invokerHelperClass = this.classLoader.loadClass(GROOVY_INVOKER_HELPER);
      Method removeClassMethod = invokerHelperClass.getMethod("removeClass", Class.class);
      Object classInfos = getAllClassInfoMethod.invoke(null);
      if (classInfos instanceof Collection) {
        unregisterClassesFromInvokerHelper(removeClassMethod, getTheClassMethod, (Collection) classInfos);
      }
    } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
      LOGGER.warn("Error trying to remove the Groovy's InvokerHelper classes", e);
    }
  }

  private void unregisterInvokerHelperRemoveClassMethod(Method removeClassMethod, Method getTheClassMethod,
                                                        Collection<?> classes) {
    for (Object classInfo : classes) {
      Object clazz = null;
      try {
        clazz = getTheClassMethod.invoke(classInfo);
        removeClassMethod.invoke(null, clazz);
      } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
        String className = clazz instanceof Class ? ((Class) clazz).getName() : "Unknown";
        LOGGER.warn("Could not remove the {} class from the Groovy's InvokerHelper", className, e);
      }
    }
  }

  private void cleanSpisEngines() {
    try {
      Class<?> abstractManager = loadClass(LOGGER_ABSTRACT_MANAGER, this.classLoader);
      Map<?, ?> hashMap = getStaticFieldValue(abstractManager, "MAP", true);
      Class<?> streamManagerClass = loadClass(LOGGER_STREAM_MANAGER, this.classLoader);
      Object rfmInstance;
      for (Object manager : hashMap.values()) {
        if (streamManagerClass.isInstance(manager)) {
          rfmInstance = manager;
          Object layout = getFieldValue(rfmInstance, "layout", true);
          Object configuration = getFieldValue(layout, "configuration", true);

          Class<?> configurationClass = loadClass(LOGGER_CONFIGURATION, this.classLoader);
          Method getScriptManagerMethod = configurationClass.getMethod("getScriptManager");
          Object scriptManager = getScriptManagerMethod.invoke(configuration);
          if (scriptManager != null) {
            cleanGroovyEngines(scriptManager);
          }
        }
      }
    } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException
        | IllegalAccessException e) {
      LOGGER.warn("Error trying to unregister the Groovy's Scripting Engine", e);
    }
  }

  private void cleanGroovyEngines(Object scriptManager)
      throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
    Object manager = getFieldValue(scriptManager, "manager", true);
    Iterable<?> engineSpis = getFieldValue(manager, "engineSpis", true);
    Class<?> groovy = loadClass(GROOVY_SCRIPT_ENGINE_FACTORY, this.classLoader);
    Iterator<?> engineSpisIterator = engineSpis.iterator();
    while (engineSpisIterator.hasNext()) {
      Object engine = engineSpisIterator.next();
      if (isGroovyScriptEngine(groovy, engine)) {
        engineSpisIterator.remove();
      }
    }
  }

  private boolean isGroovyScriptEngine(Class<?> groovy, Object engine) {
    return groovy.isInstance(engine) && engine.getClass().getClassLoader().equals(groovy.getClassLoader());
  }
}
