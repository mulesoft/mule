/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.internal.classloading;

import static org.codehaus.plexus.util.PropertyUtils.loadProperties;
import static org.mule.runtime.module.embedded.internal.classloading.JdkVersionUtils.JAVA_VERSION_PROPERTY;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Discovers the set of URLs corresponding to the JRE.
 *
 * @since 4.0
 */
// TODO MULE-11882 - Consolidate classloading isolation
public class JreUrlsDiscoverer {

  protected static final String JRE_PACKAGES_PROPERTIES = "jre-packages.properties";
  public static final String UNABLE_TO_DETERMINE_JRE_PACKAGES_ERROR = "Unable to determine packages exported by the JRE";

  public Set<String> loadJrePackages() {
    final Properties properties = loadProperties(getClass().getClassLoader().getResource(JRE_PACKAGES_PROPERTIES));

    final String jreVersionProperty = getJreVersionProperty();
    if (!properties.keySet().contains(jreVersionProperty)) {
      throw new IllegalStateException(UNABLE_TO_DETERMINE_JRE_PACKAGES_ERROR);
    }
    final String packages = (String) properties.get(jreVersionProperty);
    final HashSet<String> result = new HashSet<>();
    for (String jrePackage : packages.split(",")) {
      jrePackage = jrePackage.trim();
      if (!(jrePackage == null || jrePackage.trim().equals(""))) {
        result.add(jrePackage);
      }
    }
    return result;
  }

  private String getJreVersionProperty() {
    final JdkVersionUtils.JdkVersion jdkVersion = getJdkVersion();
    return "jre-" + jdkVersion.getMajor() + "." + jdkVersion.getMinor();
  }

  public static JdkVersionUtils.JdkVersion getJdkVersion() {
    return new JdkVersionUtils.JdkVersion(System.getProperty(JAVA_VERSION_PROPERTY));
  }
}
