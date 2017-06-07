/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.legacy.exception.ExceptionReader;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.registry.ServiceType;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.api.util.PropertiesUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ExceptionHelper</code> provides a number of helper functions that can be useful for dealing with Mule exceptions. This
 * class has 3 core functions -
 * <p/>
 * 1. ErrorCode lookup. A corresponding Mule error code can be found using for a given Mule exception 2. Additional Error
 * information such as Java doc url for a given exception can be resolved using this class 3. Error code mappings can be looked up
 * by providing the the protocol to map to and the Mule exception.
 */

public final class ExceptionHelper extends org.mule.runtime.api.exception.ExceptionHelper {

  public static final String RESOURCE_ROOT = "META-INF/";

  /**
   * This is the property to set the error code to no the message it is the property name the Transport provider uses set the set
   * the error code on the underlying message
   */
  private static final String ERROR_CODE_PROPERTY = "error.code.property";

  /**
   * logger used by this class
   */
  private static final Logger logger = LoggerFactory.getLogger(ExceptionHelper.class);

  private static String J2SE_VERSION = "";

  /**
   * todo How do you get the j2ee version??
   */
  private static final String J2EE_VERSION = "1.3ee";

  private static Properties errorDocs = new Properties();
  private static Properties errorCodes = new Properties();
  private static Map<String, Properties> errorMappings = new HashMap<>();
  private static Map<String, Boolean> disposeListenerRegistered = new HashMap<>();

  private static boolean initialised = false;

  static {
    initialise();
  }

  /**
   * Do not instanciate.
   */
  private ExceptionHelper() {
    super();
  }

  private static void initialise() {
    try {
      if (initialised) {
        return;
      }

      J2SE_VERSION = System.getProperty("java.specification.version");

      String name = RESOURCE_ROOT + ServiceType.EXCEPTION.getPath() + "/mule-exception-codes.properties";
      InputStream in = ExceptionHelper.class.getClassLoader().getResourceAsStream(name);
      if (in == null) {
        throw new IllegalArgumentException("Failed to load resource: " + name);
      }
      errorCodes.load(in);
      in.close();

      name = RESOURCE_ROOT + ServiceType.EXCEPTION.getPath() + "/mule-exception-config.properties";
      in = ExceptionHelper.class.getClassLoader().getResourceAsStream(name);
      if (in == null) {
        throw new IllegalArgumentException("Failed to load resource: " + name);
      }
      errorDocs.load(in);
      in.close();

      initialised = true;
    } catch (Exception e) {
      throw new MuleRuntimeException(CoreMessages.failedToLoad("Exception resources"), e);
    }
  }

  public static int getErrorCode(Class exception) {
    // TODO MULE-10834 - We won't be using error code for now.
    return -1;
  }

  private static Properties getErrorMappings(String protocol, final MuleContext muleContext) {
    Properties m = errorMappings.get(getErrorMappingCacheKey(protocol, muleContext));
    if (m != null) {
      return m;
    } else {
      String name = RESOURCE_ROOT + ServiceType.EXCEPTION.getPath() + "/" + protocol + "-exception-mappings.properties";
      Properties p = PropertiesUtils.loadAllProperties(name, muleContext.getExecutionClassLoader());
      errorMappings.put(getErrorMappingCacheKey(protocol, muleContext), p);
      registerAppDisposeListener(muleContext);
      return p;
    }
  }

  private static void registerAppDisposeListener(MuleContext muleContext) {
    if (!disposeListenerRegistered.containsKey(muleContext.getConfiguration().getId())) {
      try {
        muleContext.registerListener(createClearCacheListenerOnContextDispose(muleContext));
        disposeListenerRegistered.put(muleContext.getConfiguration().getId(), true);
      } catch (NotificationException e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

  private static MuleContextNotificationListener<MuleContextNotification> createClearCacheListenerOnContextDispose(final MuleContext muleContext) {
    return new MuleContextNotificationListener<MuleContextNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(MuleContextNotification notification) {
        if (notification.getAction() == MuleContextNotification.CONTEXT_DISPOSED) {
          clearCacheFor(muleContext);
          disposeListenerRegistered.remove(notification.getMuleContext().getConfiguration().getId());
        }
      }
    };
  }

  private static String getErrorMappingCacheKey(String protocol, MuleContext muleContext) {
    return protocol + "-" + muleContext.getConfiguration().getId();
  }

  public static String getErrorCodePropertyName(String protocol, MuleContext muleContext) {
    protocol = protocol.toLowerCase();
    Properties mappings = getErrorMappings(protocol, muleContext);
    if (mappings == null) {
      return null;
    }
    return mappings.getProperty(ERROR_CODE_PROPERTY);
  }

  /**
   * Maps an exception thrown for a certain protocol to an error. When there's no specific error for such transport it will return
   * a generic error. Most likely the returned error is an integer code.
   *
   * @param protocol scheme for the transport
   * @param exception exception mapped to error
   * @param muleContext the application context
   * @return the error for exception for the specific protocol
   */
  public static String getErrorMapping(String protocol, Class exception, MuleContext muleContext) {
    String code = getTransportErrorMapping(protocol, exception, muleContext);
    if (code != null) {
      return code;
    }
    code = String.valueOf(getErrorCode(exception));
    // Finally lookup mapping based on error code and return the Mule error
    // code if a match is not found
    return getErrorMappings(protocol, muleContext).getProperty(code, code);
  }

  /**
   *
   * Maps an exception thrown for a certain protocol to an error. Most likely the returned error is an integer code.
   *
   * @param protocol scheme for the transport
   * @param exception exception mapped to error
   * @param muleContext the application context
   * @return the error for exception for the specific protocol
   */
  public static String getTransportErrorMapping(String protocol, Class exception, MuleContext muleContext) {
    protocol = protocol.toLowerCase();
    Properties mappings = getErrorMappings(protocol, muleContext);
    if (mappings == null) {
      logger.info("No mappings found for protocol: " + protocol);
      return String.valueOf(getErrorCode(exception));
    }

    Class clazz = exception;
    String code = null;
    while (!clazz.equals(Object.class)) {
      code = mappings.getProperty(clazz.getName());
      if (code == null) {
        clazz = clazz.getSuperclass();
      } else {
        return code;
      }
    }
    return null;
  }

  /**
   * @deprecated since 3.8.0
   */
  @Deprecated
  public static String getJavaDocUrl(Class<?> exception) {
    return getDocUrl("javadoc.", exception.getName());
  }

  /**
   * @deprecated since 3.8.0
   */
  @Deprecated
  public static String getDocUrl(Class<?> exception) {
    return getDocUrl("doc.", exception.getName());
  }

  private static String getDocUrl(String prefix, String packageName) {
    String key = prefix;
    if (packageName.startsWith("java.") || packageName.startsWith("javax.")) {
      key += J2SE_VERSION;
    }
    String url = getUrl(key, packageName);
    if (url == null && (packageName.startsWith("java.") || packageName.startsWith("javax."))) {
      key = prefix + J2EE_VERSION;
      url = getUrl(key, packageName);
    }
    if (url != null) {
      if (!url.endsWith("/")) {
        url += "/";
      }
      String s = packageName.replaceAll("[.]", "/");
      s += ".html";
      url += s;
    }
    return url;
  }

  private static String getUrl(String key, String packageName) {
    String url = null;
    if (!key.endsWith(".")) {
      key += ".";
    }
    while (packageName.length() > 0) {
      url = errorDocs.getProperty(key + packageName, null);
      if (url == null) {
        int i = packageName.lastIndexOf(".");
        if (i == -1) {
          packageName = "";
        } else {
          packageName = packageName.substring(0, i);
        }
      } else {
        break;
      }
    }
    return url;
  }

  public static Throwable sanitizeIfNeeded(Throwable t) {
    return fullStackTraces ? t : sanitize(t);
  }

  public static Throwable getRootParentException(Throwable t) {
    Throwable cause = t;
    Throwable parent = t;
    while (cause != null) {
      if (cause.getCause() == null) {
        return parent;
      }
      parent = cause;
      cause = getExceptionReader(cause).getCause(cause);
      // address some misbehaving exceptions, avoid endless loop
      if (t == cause) {
        break;
      }
    }
    return t;
  }

  public static <T> T traverseCauseHierarchy(Throwable e, ExceptionEvaluator<T> evaluator) {
    LinkedList<Throwable> exceptions = new LinkedList<>();
    exceptions.add(e);
    while (e.getCause() != null && !e.getCause().equals(e)) {
      exceptions.addFirst(e.getCause());
      e = e.getCause();
    }
    for (Throwable exception : exceptions) {
      T value = evaluator.evaluate(exception);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  public static String writeException(Throwable t) {
    ExceptionReader er = getExceptionReader(t);
    StringBuilder msg = new StringBuilder();
    msg.append(er.getMessage(t)).append(". Type: ").append(t.getClass());
    return msg.toString();
  }

  public static interface ExceptionEvaluator<T> {

    T evaluate(Throwable e);
  }

  private static void clearCacheFor(MuleContext muleContext) {
    List<String> entriesToRemove = new ArrayList<>();
    for (String key : errorMappings.keySet()) {
      if (key.endsWith(muleContext.getConfiguration().getId())) {
        entriesToRemove.add(key);

      }
    }
    for (String key : entriesToRemove) {
      errorMappings.remove(key);
    }
  }
}
