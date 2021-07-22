/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static java.beans.Introspector.flushCaches;
import static java.lang.Boolean.getBoolean;
import static java.lang.Thread.getAllStackTraces;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.Objects.nonNull;
import static org.mule.runtime.core.api.util.ClassUtils.getField;
import static org.mule.runtime.core.api.util.ClassUtils.getStaticFieldValue;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.api.util.ClassUtils.setStaticFieldValue;

/**
 * A Utility class for releasing all the known references that may lead to
 * a ClassLoader leak.
 */
public class IBMMQResourceReleaser implements ResourceReleaser {

  private static final Logger LOGGER = LoggerFactory.getLogger(IBMMQResourceReleaser.class);

  private static final String AVOID_IBM_MQ_CLEANUP_PROPERTY_NAME = "avoid.ibm.mq.cleanup";
  private static final boolean IBM_MQ_RESOURCE_RELEASER_AVOID_CLEANUP =
      getBoolean(AVOID_IBM_MQ_CLEANUP_PROPERTY_NAME);

  private static final String JMSCC_THREAD_POOL_MASTER_NAME = "JMSCCThreadPoolMaster";
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

    removeMBeans();
    cleanJULKnownLevels();
    cleanMQCommonServices();
    cleanMQEnvironment();
    cleanJmsTls();
    cleanTraceController();
    removeThreadLocals();

  }

  /**
   * Creates a new Instance of IBMMQResourceReleaser
   */
  public IBMMQResourceReleaser(ClassLoader classLoader) {
    this.driverClassLoader = classLoader;
  }

  /**
   * The IBM MQ Driver registers two MBeans for management.
   * When disposing the application / domain, this beans keep references
   * to classes loaded by this ClassLoader. When the application is removed,
   * the ClassLoader is leaked due this references.
   *
   * The two known mbeans are
   *  * TraceControl
   *  * PropertyStoreControl
   *
   */
  public void removeMBeans() {

    LOGGER.debug("Removing registered MBeans of the IBM MQ Driver (if present)");

    MBeanServer mBeanServer = getPlatformMBeanServer();
    Set<ObjectInstance> instances = mBeanServer.queryMBeans(null, null);
    if (LOGGER.isDebugEnabled()) {
      instances
          .forEach(x -> LOGGER.debug("MBean Found: Class Name: {} // Object Name: {}", x.getClassName(), x.getObjectName()));
    }

    //Object Name::type=CommonServices,name=TraceControl
    final Hashtable<String, String> keys = new Hashtable<>();
    keys.put("type", "CommonServices");
    keys.put("name", "TraceControl");
    try {
      if (driverClassLoader == mBeanServer.getClassLoaderFor(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys))) {
        mBeanServer.unregisterMBean(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys));
      }
      LOGGER.debug("Unregistered IBM MQ TraceControl MBean");

    } catch (javax.management.InstanceNotFoundException | MalformedObjectNameException | MBeanRegistrationException e) {
      LOGGER.warn("Caught exception unregistering the IBM MQ TraceControl MBean: {}", e.getMessage(), e);
    }

    //IBM WebSphere MQ:type=CommonServices,name=PropertyStoreControl
    keys.put("type", "CommonServices");
    keys.put("name", "PropertyStoreControl");
    try {
      if (driverClassLoader == mBeanServer.getClassLoaderFor(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys))) {
        mBeanServer.unregisterMBean(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys));
      }
      LOGGER.debug("Unregistered IBM MQ PropertyStoreControl MBean");
    } catch (javax.management.InstanceNotFoundException | MalformedObjectNameException | MBeanRegistrationException e) {
      LOGGER.warn("Caught exception removing known IBM MQ Mbeans: {}", e.getMessage(), e);
    }
    flushCaches();
  }

  /**
   *  The IBM MQ Driver registers custom Java Util Logging (JUL) Levels.
   *  the JUL Classes are loaded by system Classloader. So there are references left
   *  from outside the application context. So, it retains instances and leaks the application
   *  ClassLoader.
   *  This method removes the knownLevels registered by the Classes Loaded by the driver ClassLoader.
   */
  public void cleanJULKnownLevels() {

    LOGGER.debug("Cleaning Java Util Logging references");

    try {

      Class<?> knownLevelClass = loadClass(JUL_KNOWN_LEVEL_CLASS, driverClassLoader);
      Field levelObjectField = getField(knownLevelClass, "levelObject", false);
      synchronized (knownLevelClass) {
        final Map<?, List> nameToLevels = (Map<?, List>) getStaticFieldValue(knownLevelClass, "nameToLevels", false);
        final Map<?, List> intToLevels = (Map<?, List>) getStaticFieldValue(knownLevelClass, "intToLevels", false);
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
    } catch (Exception e) {
      LOGGER.warn("Caught error when removing JUL References: {}", e.getMessage(), e);
    }
  }

  /**
   * Processes and removes the Java Util Logging KnownLevels references
   * @param levelObjectField Field to access internal property.
   * @param levelsMaps Map wich contains references
   * @return Removed KnownLevels
   * @throws Exception
   */
  private Set processJULKnownLevels(
                                    Field levelObjectField, Map<?, List> levelsMaps)
      throws Exception {
    Set output = new HashSet<>();
    for (List knownLevels : levelsMaps.values()) {
      for (Iterator iter = knownLevels.listIterator(); iter.hasNext();) {
        Object knownLevel = iter.next();
        Level levelObject = (Level) levelObjectField.get(knownLevel);
        if (driverClassLoader == levelObject.getClass().getClassLoader()) {
          iter.remove();
          output.add(knownLevel);
        }
      }
    }
    return output;
  }

  /**
   * The JmsTls class keep several references in a private static final field
   * This references avoid the proper ClassLoader disposal.
   * This method performs the JmsTls Class cleanup.
   */
  private void cleanJmsTls() {
    try {
      Class jmsTlsClass = loadClass(IBM_MQ_JMS_TLS_CLASS, driverClassLoader);
      setStaticFieldValue(jmsTlsClass, "myInstance", null, true);
    } catch (Exception ex) {
      LOGGER.warn("Caught Exception when clearing JmsTls: {}", ex.getMessage(), ex);
    }
  }

  /**
   * The TraceController classes keep several references in private static final field
   * This references avoid the proper ClassLoader disposal.
   * This method performs the TraceController cleanup.
   */
  private void cleanTraceController() {

    try {
      Class traceClass = loadClass(IBM_MQ_TRACE_CLASS, driverClassLoader);
      setStaticFieldValue(traceClass, "traceController", null, true);
    } catch (Exception ex) {
      LOGGER.warn("Caught Exception when clearing TraceController: {}", ex.getMessage(), ex);
    }
  }


  /**
   * While analyzing the HeapDumps looking for the ClassLoader Leak causes,
   * there were ThreadLocal references to driver classes in threads that do not belong to
   * the application being disposed. This references leads to a ClassLoader leak.
   * This method removes the thread local references to instances of classes loaded by the driver classloader.
   */
  public void removeThreadLocals() {

    Field threadLocalsField = null;
    Field inheritableThreadLocalsField = null;
    Field threadLocalMapTableField = null;

    try {

      LOGGER.debug("Removing ThreadLocals");

      threadLocalsField = getField(Thread.class, THREADLOCALS_FIELD, false);
      inheritableThreadLocalsField = getField(Thread.class, INHERITABLE_THREADLOCALS_FIELD, false);
      Class<?> threadLocalMapTableClass = loadClass(THREADLOCAL_MAP_TABLE_CLASS, driverClassLoader);
      threadLocalMapTableField = getField(threadLocalMapTableClass, "table", false);
    } catch (NoSuchFieldException | ClassNotFoundException ex) {
      LOGGER.warn("Caught Exception  while getting required fields: {}", ex.getMessage(), ex);
    }


    for (Thread thread : getAllStackTraces().keySet()) {
      LOGGER.debug("Processing Thread: {} / {}", thread.getThreadGroup().getName(), thread.getName());

      if (nonNull(threadLocalsField)) {
        try {
          processThreadLocalMap(threadLocalMapTableField, threadLocalsField.get(thread));
        } catch (IllegalAccessException ex) {
          LOGGER.warn("Caught exception getting ThreadLocals Field of {} thread: {}", thread.getName(), ex.getMessage(), ex);
        }
      }
      if (nonNull(inheritableThreadLocalsField)) {
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
   * @param threadLocalMap Map containing ThreadLocal values
   */
  private void processThreadLocalMap(Field threadLocalMapTableField, Object threadLocalMap) {

    if (nonNull(threadLocalMap) && nonNull(threadLocalMapTableField)) {

      Object[] threadLocalMapTable = {};

      try {
        threadLocalMapTable = (Object[]) threadLocalMapTableField.get(threadLocalMap);
      } catch (IllegalAccessException e) {
        LOGGER.error("Could not get threadLocalMapTablefield: {}", e.getMessage(), e);
      }

      for (Object entry : threadLocalMapTable) {

        if (entry != null) {
          Reference<?> reference = (Reference<?>) entry;
          ThreadLocal<?> threadLocal = (ThreadLocal<?>) reference.get();
          if (nonNull(threadLocal)) {
            try {

              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ClassLoader: {}", this.getClass().getClassLoader().getClass().getName());
              }
              Object x = threadLocal.get();
              if (nonNull(x)) {
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
                  Field threadLocalMapEntryValueField = getField(entry.getClass(), "value", false);
                  threadLocalMapEntryValueField.set(entry, null);
                  ((Reference<?>) entry).clear();
                }
              }

            } catch (IllegalAccessException | NoSuchFieldException e) {
              LOGGER.warn("Caught Exception while accessing threadLocalMapEntryValueField: {}", e.getMessage(), e);
            }

          }
        }
      }
    }
  }


  /**
   * Removes references held by MQCommonServices.
   */
  public void cleanMQCommonServices() {
    try {
      Class<?> mqCommonServicesClass = loadClass(IBM_MQ_COMMON_SERVICES_CLASS, driverClassLoader);
      setStaticFieldValue(mqCommonServicesClass, "jmqiEnv", null, true);
    } catch (Exception ex) {
      LOGGER.warn("Caught Exception cleaning MQCommonServices: {}", ex.getMessage(), ex);
    }
  }

  /**
   * Removes the static references held by the MQEnvironment Class
   */
  public void cleanMQEnvironment() {
    try {
      Class<?> mqEnvironmentClass = loadClass(IBM_MQ_ENVIRONMENT_CLASS, driverClassLoader);
      setStaticFieldValue(mqEnvironmentClass, "defaultMQCxManager", null, true);
    } catch (Exception ex) {
      LOGGER.warn("Caught Exception cleaning MQEnvironment: {}", ex.getMessage(), ex);
    }
  }

}
