/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.slf4j.Logger;

/**
 * A utility class to release all resources associated to Groovy Dependency on un-deployment to prevent classloader leaks
 */
public class GroovyResourceReleaser implements ResourceReleaser {

  private final ClassLoader classLoader;
  private static final Logger LOGGER = getLogger(GroovyResourceReleaser.class);
  private static final String GROOVY_CLASS_INFO = "org.codehaus.groovy.reflection.ClassInfo";
  private static final String GROOVY_INVOKER_HELPER = "org.codehaus.groovy.runtime.InvokerHelper";
  private static final String GROOVY_SCRIPT_ENGINE_FACTORY = "org.codehaus.groovy.jsr223.GroovyScriptEngineFactory";
  private static final String LOGGER_ABSTRACT_MANAGER = "org.apache.logging.log4j.core.appender.AbstractManager";
  private static final String LOGGER_ROLLING_FILE_MANAGER = "org.apache.logging.log4j.core.appender.rolling.RollingFileManager";
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

  private void unregisterInvokerHelper() {
    try {
      Class classInfoClass = this.classLoader.loadClass(GROOVY_CLASS_INFO);
      Method getAllClassInfoMethod = classInfoClass.getMethod("getAllClassInfo");
      Method getTheClassMethod = classInfoClass.getMethod("getTheClass");
      Class invokerHelperClass = this.classLoader.loadClass(GROOVY_INVOKER_HELPER);
      Method removeClassMethod = invokerHelperClass.getMethod("removeClass", Class.class);
      Object classes = getAllClassInfoMethod.invoke(null);
      if (classes != null && classes instanceof Collection) {
        for (Object classInfo : ((Collection) classes)) {
          String className = "";
          try {
            Object clazz = getTheClassMethod.invoke(classInfo);
            Method getNameMethod = clazz.getClass().getMethod("getName");
            className = (String) getNameMethod.invoke(clazz);
            removeClassMethod.invoke(null, clazz);
          } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Could not remove the {} class from the Groovy's InvokerHelper", className, e);
          }
        }
      }
    } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
      LOGGER.warn("Error trying to remove the Groovy's InvokerHelper classes", e);
    }
  }

  private void cleanSpisEngines() {
    try {
      Class<?> abstractManager = loadClass(LOGGER_ABSTRACT_MANAGER, this.classLoader);
      HashMap hashMap = getStaticFieldValue(abstractManager, "MAP", true);
      Iterator it = hashMap.values().iterator();
      Class<?> rollingFileManagerClass = loadClass(LOGGER_ROLLING_FILE_MANAGER, this.classLoader);
      Object rfmInstance = null;
      while (it.hasNext()) {
        Object o = it.next();
        if (rollingFileManagerClass.isInstance(o)) {
          rfmInstance = o;
          Object layout = getFieldValue(rfmInstance, "layout", true);
          Object configuration = getFieldValue(layout, "configuration", true);

          Class<?> configurationClass = loadClass(LOGGER_CONFIGURATION, this.classLoader);
          Method getScriptManagerMethod = configurationClass.getMethod("getScriptManager");
          Object scriptManager = getScriptManagerMethod.invoke(configuration);

          Object manager = getFieldValue(scriptManager, "manager", true);
          HashSet engineSpis = (HashSet) getFieldValue(manager, "engineSpis", true);
          Class groovy = loadClass(GROOVY_SCRIPT_ENGINE_FACTORY, this.classLoader);
          Iterator engineSpisIterator = engineSpis.iterator();
          while (engineSpisIterator.hasNext()) {
            Object i = engineSpisIterator.next();
            if (groovy.isInstance(i) && i.getClass().getClassLoader().equals(groovy.getClassLoader())) {
              engineSpis.remove(i);
            }
          }
        }
      }
    } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException
        | IllegalAccessException e) {
      LOGGER.warn("Error trying to unregister the Groovy's Scripting Engine", e);
    }
  }
}
