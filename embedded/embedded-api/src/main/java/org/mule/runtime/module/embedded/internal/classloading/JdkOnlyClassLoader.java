/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.internal.classloading;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

// TODO MULE-11882 - Consolidate classloading isolation
public class JdkOnlyClassLoader extends FilteringClassLoader {

  public static final Set<String> BOOT_PACKAGES =
      ImmutableSet.of("java", "javax.smartcardio",
                      // Java EE
                      "javax.resource", "javax.servlet", "javax.ws", "javax.mail", "javax.inject", "org.apache.xerces",
                      "org.apache.logging.log4j", "org.slf4j", "org.apache.commons.logging", "org.apache.log4j", "org.dom4j",
                      "com.sun", "sun", "org.mule.mvel2",
                      "org.codehaus.groovy",
                      "org.aopalliance.aop",
                      "com.yourkit");

  /**
   * Creates a new filtering classLoader that only loads jdk specific classes
   */
  public JdkOnlyClassLoader() {
    super(new ClassLoaderFilter(ImmutableSet.<String>builder().addAll(BOOT_PACKAGES)
        .addAll(new JreUrlsDiscoverer().loadJrePackages()).build()));
  }
}
