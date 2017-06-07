/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Loads a standard $MULE_HOME/lib/* hierarchy.
 */
public class MuleContainerSystemClassLoader extends URLClassLoader {

  static {
    registerAsParallelCapable();
  }

  // protected transient Log logger = LogFactory.getLog(getClass());

  public MuleContainerSystemClassLoader(DefaultMuleClassPathConfig classPath) {
    super(new URL[0]);

    try {
      final List<URL> urlsList = classPath.getURLs();
      for (URL url : urlsList) {
        // if (logger.isDebugEnabled())
        // {
        // logger.debug("adding URL " + url);
        // }

        addURL(url);
      }
    } catch (Exception e) {
      // if (logger.isDebugEnabled())
      // {
      // logger.debug(e);
      // }
    }
  }
}
