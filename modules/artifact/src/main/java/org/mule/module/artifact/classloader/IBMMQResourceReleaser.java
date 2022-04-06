/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static org.mule.runtime.core.api.util.ClassUtils.getField;
import static org.mule.runtime.core.api.util.ClassUtils.getStaticFieldValue;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.api.util.ClassUtils.setStaticFieldValue;
import static java.beans.Introspector.flushCaches;
import static java.lang.Boolean.getBoolean;
import static java.lang.Thread.getAllStackTraces;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Utility class for releasing all the known references that may lead to a ClassLoader leak.
 */
public class IBMMQResourceReleaser implements ResourceReleaser {

  private static final Logger LOGGER = LoggerFactory.getLogger(IBMMQResourceReleaser.class);

  private static final String AVOID_IBM_MQ_CLEANUP_PROPERTY_NAME = "avoid.ibm.mq.cleanup";
  private static final String AVOID_IBM_MQ_CLEANUP_MBEANS_PROPERTY_NAME = "avoid.ibm.mq.cleanup.mbeans";

  private final boolean IBM_MQ_RESOURCE_RELEASER_AVOID_CLEANUP = getBoolean(AVOID_IBM_MQ_CLEANUP_PROPERTY_NAME);
  private final boolean IBM_MQ_RESOURCE_RELEASER_AVOID_CLEANUP_MBEANS =
      getBoolean(AVOID_IBM_MQ_CLEANUP_MBEANS_PROPERTY_NAME);

  private final static String THREADLOCALS_FIELD = "threadLocals";
  private final static String INHERITABLE_THREADLOCALS_FIELD = "inheritableThreadLocals";
  private final static String THREADLOCAL_MAP_TABLE_CLASS = "java.lang.ThreadLocal$ThreadLocalMap";
  private final static String JUL_KNOWN_LEVEL_CLASS = "java.util.logging.Level$KnownLevel";
  private final static String IBM_MQ_MBEAN_DOMAIN = "IBM MQ";
  private final static String IBM_MQ_COMMON_SERVICES_CLASS = "com.ibm.mq.internal.MQCommonServices";
  private final static String IBM_MQ_ENVIRONMENT_CLASS = "com.ibm.mq.MQEnvironment";
  private final static String IBM_MQ_JMS_TLS_CLASS = "com.ibm.msg.client.jms.internal.JmsTls";
  private final static String IBM_MQ_TRACE_CLASS = "com.ibm.msg.client.commonservices.trace.Trace";
  private final ClassLoader driverClassLoader;


  @Override
  public void release() {
    if (IBM_MQ_RESOURCE_RELEASER_AVOID_CLEANUP) {
      LOGGER.debug("Avoiding IBM MQ resources cleanup.");
      return;
    }
    LOGGER.debug("Releasing IBM MQ resources");

    if (!IBM_MQ_RESOURCE_RELEASER_AVOID_CLEANUP_MBEANS) {
      LOGGER.debug("Releasing IBM MQ resources - Removal of registered mBeans is called.");
      removeMBeans();
    }

    LOGGER.debug("Releasing IBM MQ resources - Removal of JUL Custom Logging Levels.");
    cleanJULKnownLevels();

    LOGGER.debug("Releasing IBM MQ resources - Removes references held by MQCommonServices Class.");
    cleanPrivateStaticFieldForClass(IBM_MQ_COMMON_SERVICES_CLASS, "jmqiEnv");

    LOGGER.debug("Releasing IBM MQ resources - Removes the static references held by the MQEnvironment Class.");
    cleanPrivateStaticFieldForClass(IBM_MQ_ENVIRONMENT_CLASS, "defaultMQCxManager");

    LOGGER.debug("Releasing IBM MQ resources - Removes the static references held by the JmsTls Class.");
    cleanPrivateStaticFieldForClass(IBM_MQ_JMS_TLS_CLASS, "myInstance");

    LOGGER.debug("Releasing IBM MQ resources - Removes the static references held by the TraceController Class.");
    cleanPrivateStaticFieldForClass(IBM_MQ_TRACE_CLASS, "traceController");

    LOGGER
        .debug("Releasing IBM MQ resources - Removes the thread local references to instances of classes loaded by the driver classloader.");
    removeThreadLocals();

  }

  /**
   * Creates a new Instance of IBMMQResourceReleaser
   * 
   * @param classLoader ClassLoader which loaded the IBM MQ Driver.
   */
  public IBMMQResourceReleaser(ClassLoader classLoader) {
    this.driverClassLoader = classLoader;
  }

  /**
   * The IBM MQ Driver registers two MBeans for management. When disposing the application / domain, this beans keep references to
   * classes loaded by this ClassLoader. When the application is removed, the ClassLoader is leaked due this references.
   *
   * The two known mbeans are * TraceControl * PropertyStoreControl
   *
   */
  // TODO MULE-19714 Promote IBM ResourceReleaser features as Generic Features
  public void removeMBeans() {

    LOGGER.debug("Removing registered MBeans of the IBM MQ Driver (if present)");

    MBeanServer mBeanServer = getPlatformMBeanServer();
    Set<ObjectInstance> instances = mBeanServer.queryMBeans(null, null);
    if (LOGGER.isDebugEnabled()) {
      instances
          .forEach(x -> LOGGER.debug("MBean Found: Class Name: {} // Object Name: {}", x.getClassName(), x.getObjectName()));
    }

    // Object Name::type=CommonServices,name=TraceControl
    // IBM WebSphere MQ:type=CommonServices,name=PropertyStoreControl
    final Hashtable<String, String> keys = new Hashtable<>();
    keys.put("type", "CommonServices");
    keys.put("name", "*");
    try {
      for (ObjectInstance object : mBeanServer.queryMBeans(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys), null)) {
        mBeanServer.unregisterMBean(object.getObjectName());
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Unregistered {}", object.getObjectName());
        }
      }
    } catch (javax.management.InstanceNotFoundException ex) {
      LOGGER.debug("No instance of CommonServices/TraceControl MBean was found.");
    } catch (MalformedObjectNameException | MBeanRegistrationException e) {
      LOGGER.warn("Caught exception unregistering the IBM MQ TraceControl MBean: {}", e.getMessage(), e);
    }
    flushCaches();
  }

  /**
   * The IBM MQ Driver registers custom Java Util Logging (JUL) Levels. the JUL Classes are loaded by system Classloader. So there
   * are references left from outside the application context. So, it retains instances and leaks the application ClassLoader.
   * This method removes the knownLevels registered by the Classes Loaded by the driver ClassLoader. This only applies to JDK8. *
   * https://bugs.openjdk.java.net/browse/JDK-6543126 *
   * https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/logging/Level.java#L534 *
   * https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/src/java.logging/share/classes/java/util/logging/Level.java#L563
   */
  // TODO MULE-19714 Promote IBM ResourceReleaser features as Generic Features
  public void cleanJULKnownLevels() {

    LOGGER.debug("Cleaning Java Util Logging references");
    Class<?> knownLevelClass;
    Field levelObjectField;
    try {
      knownLevelClass = loadClass(JUL_KNOWN_LEVEL_CLASS, driverClassLoader);
    } catch (ClassNotFoundException e) {
      LOGGER.warn("The {} class was not found. This may be caused by a JVM or driver upgrade.", JUL_KNOWN_LEVEL_CLASS);
      return;
    }

    try {
      levelObjectField = getField(knownLevelClass, "levelObject", false);
      levelObjectField.setAccessible(true);
    } catch (NoSuchFieldException ex) {
      LOGGER.warn("The level field was not found for the {} class. This may be caused by a JVM or driver upgrade ",
                  JUL_KNOWN_LEVEL_CLASS);
      return;
    }

    synchronized (knownLevelClass) {
      Map<?, List> nameToLevels = null;
      Map<?, List> intToLevels = null;

      try {
        nameToLevels = getStaticFieldValue(knownLevelClass, "nameToLevels", false);
      } catch (NoSuchFieldException | IllegalAccessException ex) {
        LOGGER.warn("Caught exception when accessing the nameToLevels field for {} class: {}", knownLevelClass, ex.getMessage(),
                    ex);
      }

      try {
        intToLevels = getStaticFieldValue(knownLevelClass, "intToLevels", false);
      } catch (NoSuchFieldException | IllegalAccessException ex) {
        LOGGER.warn("Caught exception when accessing the intToLevels field for {} class: {}", knownLevelClass, ex.getMessage(),
                    ex);
      }

      if (nameToLevels != null) {
        final Set removed = processJULKnownLevels(levelObjectField, nameToLevels);
        if (intToLevels != null) {
          for (List knownLevels : intToLevels.values()) {
            knownLevels.removeAll(removed);
          }
        }
      } else if (intToLevels != null) {
        processJULKnownLevels(levelObjectField, intToLevels);
      }
    }
  }

  /**
   * Processes and removes the Java Util Logging KnownLevels references
   * 
   * @param levelObjectField Field to access internal property.
   * @param levelsMaps       Map which contains references
   * @return Removed KnownLevels
   * @throws Exception
   */
  private Set processJULKnownLevels(
                                    Field levelObjectField, Map<?, List> levelsMaps) {
    Set output = new HashSet<>();
    for (List knownLevels : levelsMaps.values()) {
      for (Iterator iter = knownLevels.listIterator(); iter.hasNext();) {
        Object knownLevel = iter.next();
        try {
          Level levelObject = (Level) levelObjectField.get(knownLevel);
          if (driverClassLoader == levelObject.getClass().getClassLoader()) {
            iter.remove();
            output.add(knownLevel);
          }
        } catch (IllegalAccessException ex) {
          LOGGER.warn("Caught IllegalAccessException when removing the JUL KnownLevels: {}", ex.getMessage(), ex);
        }
      }
    }
    return output;
  }


  /**
   * Sets the the private static field of class to null.
   * 
   * @param className the target class name.
   * @param fieldName the target field name.
   */
  private void cleanPrivateStaticFieldForClass(String className, String fieldName) {
    Class targetClass;
    try {
      targetClass = loadClass(className, driverClassLoader);
    } catch (ClassNotFoundException ex) {
      LOGGER.warn("Could not load the {} class. The provided driver version may have removed it", className, ex);
      return;
    }
    try {
      setStaticFieldValue(targetClass, fieldName, null, true);
    } catch (NoSuchFieldException | IllegalAccessException ex) {
      LOGGER.warn("Could not clear the field {} for class {}. The provided river version may have removed it.", fieldName,
                  className, ex);
    }
  }

  /**
   * While analyzing the HeapDumps looking for the ClassLoader Leak causes, there were ThreadLocal references to driver classes in
   * threads that do not belong to the application being disposed. This references leads to a ClassLoader leak. This method
   * removes the thread local references to instances of classes loaded by the driver classloader.
   */
  // TODO MULE-19714 Promote IBM ResourceReleaser features as Generic Features
  public void removeThreadLocals() {

    LOGGER.debug("Removing ThreadLocals");
    Field threadLocalsField = null;
    Field inheritableThreadLocalsField = null;
    Field threadLocalMapTableField = null;

    try {
      threadLocalsField = getField(Thread.class, THREADLOCALS_FIELD, false);
    } catch (NoSuchFieldException ex) {
      LOGGER.warn("Could not get the {} field for Thread.class. This may be related to a change in the JVM.", THREADLOCALS_FIELD,
                  ex);
    }

    try {
      inheritableThreadLocalsField = getField(Thread.class, INHERITABLE_THREADLOCALS_FIELD, false);
    } catch (NoSuchFieldException ex) {
      LOGGER.warn("Could not get the {} field for Thread.class. This may be related to a change in the JVM",
                  INHERITABLE_THREADLOCALS_FIELD, ex);
    }

    if (threadLocalsField != null) {
      threadLocalsField.setAccessible(true);
    }
    if (inheritableThreadLocalsField != null) {
      inheritableThreadLocalsField.setAccessible(true);
    }

    try {
      Class<?> threadLocalMapTableClass = loadClass(THREADLOCAL_MAP_TABLE_CLASS, driverClassLoader);
      threadLocalMapTableField = getField(threadLocalMapTableClass, "table", false);
      threadLocalMapTableField.setAccessible(true);
    } catch (ClassNotFoundException ex) {
      LOGGER.warn("Could not find the {} class. This may be related to a change in the JVM", THREADLOCAL_MAP_TABLE_CLASS, ex);
      return;
    } catch (NoSuchFieldException ex) {
      LOGGER.warn("Could not find the table field for {} class. This may be related to a change in the JVM",
                  THREADLOCAL_MAP_TABLE_CLASS, ex);
      return;
    }

    for (Thread thread : getAllStackTraces().keySet()) {
      if (LOGGER.isDebugEnabled() && thread.getThreadGroup() != null) {
        LOGGER.debug("Processing Thread: {} / {}", thread.getThreadGroup().getName(), thread.getName());
      }

      if (threadLocalsField != null) {
        try {
          processThreadLocalMap(threadLocalMapTableField, threadLocalsField.get(thread));
        } catch (IllegalAccessException ex) {
          LOGGER.warn("Caught exception getting ThreadLocals Field of {} thread: {}", thread.getName(), ex.getMessage(), ex);
        }
      }
      if (inheritableThreadLocalsField != null) {
        try {
          processThreadLocalMap(threadLocalMapTableField, inheritableThreadLocalsField.get(thread));
        } catch (IllegalAccessException ex) {
          LOGGER.warn("Caught exception getting InheritableThreadLocals Field of {} thread: {}", thread.getName(),
                      ex.getMessage(), ex);
        }
      }
    }

  }

  /**
   * Removes the threadLocal variables related to classes Loaded by this classloader.
   * 
   * @param threadLocalMap Map containing ThreadLocal values
   */
  private void processThreadLocalMap(Field threadLocalMapTableField, Object threadLocalMap) {

    if (threadLocalMap == null) {
      return;
    }
    Object[] threadLocalMapTable = {};

    try {
      threadLocalMapTable = (Object[]) threadLocalMapTableField.get(threadLocalMap);
    } catch (IllegalAccessException e) {
      LOGGER.warn("Could not get threadLocalMapTablefield: {}", e.getMessage(), e);
      return;
    }

    for (Object entry : threadLocalMapTable) {

      if (entry != null) {

        Reference<?> reference = (Reference<?>) entry;
        ThreadLocal<?> threadLocal = (ThreadLocal<?>) reference.get();

        if (threadLocal != null) {

          Object x = threadLocal.get();
          if (x != null) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("ThreadLocal ClassLoader: {}", x.getClass().getClassLoader());
              LOGGER.debug("ThreadLocal Class: {}", x.getClass().getCanonicalName());
            }
            if (driverClassLoader == x.getClass().getClassLoader()) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removing instance of {}", x.getClass().getCanonicalName());
              }
              threadLocal.remove();
              threadLocal.set(null);
              reference.clear();
              try {
                Field threadLocalMapEntryValueField = getField(entry.getClass(), "value", false);
                threadLocalMapEntryValueField.setAccessible(true);
                threadLocalMapEntryValueField.set(entry, null);
                ((Reference<?>) entry).clear();
              } catch (NoSuchFieldException ex) {
                LOGGER.warn("Could not get field value for class {}: {}", entry.getClass(), ex.getMessage(), ex);
              } catch (IllegalAccessException ex) {
                LOGGER.warn("Could not clear the thread local's entry map reference for class{}: {}", entry.getClass(),
                            ex.getMessage(), ex);
              }
            }
          }
        }
      }
    }
  }
}
