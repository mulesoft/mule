/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.util.CollectionUtils.singletonList;
import static org.mule.runtime.core.util.JdkVersionUtils.getJdkVersion;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import org.mule.runtime.core.util.JdkVersionUtils.JdkVersion;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/**
 * Discovers the module corresponding to the JRE by creating a new {@link MuleModule} from the packages listed on the
 * {@value JRE_PACKAGES_PROPERTIES} file.
 *
 * @since 4.0
 */
public class JreModuleDiscoverer implements ModuleDiscoverer {

  protected static final String JRE_PACKAGES_PROPERTIES = "jre-packages.properties";
  protected static final String JRE_MODULE_NAME = "jre";
  public static final String UNABLE_TO_DETERMINE_JRE_PACKAGES_ERROR = "Unable to determine packages exported by the JRE";

  @Override
  public List<MuleModule> discover() {
    return singletonList(new MuleModule(JRE_MODULE_NAME, loadJrePackages(), emptySet()));
  }

  private HashSet<String> loadJrePackages() {
    try {
      final Properties properties = loadProperties(getClass().getClassLoader().getResource(JRE_PACKAGES_PROPERTIES));

      final String jreVersionProperty = getJreVersionProperty();
      if (!properties.keySet().contains(jreVersionProperty)) {
        throw new IllegalStateException(UNABLE_TO_DETERMINE_JRE_PACKAGES_ERROR);
      }
      final String packages = (String) properties.get(jreVersionProperty);
      final HashSet<String> result = new HashSet<>();
      for (String jrePackage : packages.split(",")) {
        jrePackage = jrePackage.trim();
        if (!isEmpty(jrePackage)) {
          result.add(jrePackage);
        }
      }

      return result;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to determine JRE provided packages", e);
    }
  }

  private String getJreVersionProperty() {
    final JdkVersion jdkVersion = getJdkVersion();
    return "jre-" + jdkVersion.getMajor() + "." + jdkVersion.getMinor();
  }
}
