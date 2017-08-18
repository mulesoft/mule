/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.net;

import org.mule.runtime.core.api.util.ClassUtils;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for loading URL protocol handlers. This factory is necessary to make Mule work in cases where the standard approach
 * using system properties does not work, e.g. in application servers or with maven's surefire tests.
 * <p>
 * Client classes can register a subclass of {@link URLStreamHandler} for a given protocol. This implementation first checks its
 * registered handlers before resorting to the default mechanism.
 * <p>
 * 
 * @see java.net.URL#URL(String, String, int, String)
 */
public class MuleUrlStreamHandlerFactory extends Object implements URLStreamHandlerFactory {

  private static final String HANDLER_PKGS_SYSTEM_PROPERTY = "java.protocol.handler.pkgs";
  private static final Logger log = LoggerFactory.getLogger(MuleUrlStreamHandlerFactory.class);

  private static Map registry = Collections.synchronizedMap(new HashMap());

  /**
   * Install an instance of this class as UrlStreamHandlerFactory. This may be done exactly once as {@link URL} will throw an
   * {@link Error} on subsequent invocations.
   * <p>
   * This method takes care that multiple invocations are possible, but the UrlStreamHandlerFactory is installed only once.
   */
  public static synchronized void installUrlStreamHandlerFactory() {
    /*
     * When running under surefire, this class will be loaded by different class loaders and will be running in multiple "main"
     * thread objects. Thus, there is no way for this class to register a globally available variable to store the info whether
     * our custom UrlStreamHandlerFactory was already registered.
     * 
     * The only way to accomplish this is to catch the Error that is thrown by URL when trying to re-register the custom
     * UrlStreamHandlerFactory.
     */
    try {
      URL.setURLStreamHandlerFactory(new MuleUrlStreamHandlerFactory());
    } catch (Error err) {
      if (log.isDebugEnabled()) {
        log.debug("Custom MuleUrlStreamHandlerFactory already registered", err);
      }
    }
  }

  public static void registerHandler(String protocol, URLStreamHandler handler) {
    registry.put(protocol, handler);
  }

  public URLStreamHandler createURLStreamHandler(String protocol) {
    URLStreamHandler handler = (URLStreamHandler) registry.get(protocol);
    if (handler == null) {
      handler = this.defaultHandlerCreateStrategy(protocol);
    }
    return handler;
  }

  private URLStreamHandler defaultHandlerCreateStrategy(String protocol) {
    String packagePrefixList = System.getProperty(HANDLER_PKGS_SYSTEM_PROPERTY, "");

    if (packagePrefixList.endsWith("|") == false) {
      packagePrefixList += "|sun.net.www.protocol";
    }

    StringTokenizer tokenizer = new StringTokenizer(packagePrefixList, "|");

    URLStreamHandler handler = null;
    while (handler == null && tokenizer.hasMoreTokens()) {
      String packagePrefix = tokenizer.nextToken().trim();
      String className = packagePrefix + "." + protocol + ".Handler";
      try {
        handler = (URLStreamHandler) ClassUtils.instantiateClass(className);
      } catch (Exception ex) {
        // not much we can do here
      }
    }

    return handler;
  }
}
