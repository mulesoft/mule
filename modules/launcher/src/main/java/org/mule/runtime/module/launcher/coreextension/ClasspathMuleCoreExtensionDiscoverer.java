/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.PropertiesUtils.loadProperties;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers {@link MuleCoreExtension} classes that are defined in the classpath using core-extensions.properties files.
 */
public class ClasspathMuleCoreExtensionDiscoverer implements MuleCoreExtensionDiscoverer {

  public static final String CORE_EXTENSION_RESOURCE_NAME =
      "META-INF/org/mule/runtime/core/config/core-extensions.properties";

  private static Logger logger = LoggerFactory.getLogger(ClasspathMuleCoreExtensionDiscoverer.class);
  private final ArtifactClassLoader containerClassLoader;

  /**
   * Creates a new extension discoverer
   *
   * @param containerClassLoader classloader where the discovering process will be executed. Non null.
   */
  public ClasspathMuleCoreExtensionDiscoverer(ArtifactClassLoader containerClassLoader) {
    checkArgument(containerClassLoader != null, "Container classLoader cannot be null");
    this.containerClassLoader = containerClassLoader;
  }

  @Override
  public List<MuleCoreExtension> discover() throws MuleException {
    List<MuleCoreExtension> result = new LinkedList<>();

    Enumeration<?> e = ClassUtils.getResources(CORE_EXTENSION_RESOURCE_NAME, getClass().getClassLoader());
    List<Properties> extensions = new LinkedList<Properties>();

    // load ALL of the extension files first
    while (e.hasMoreElements()) {
      try {
        URL url = (URL) e.nextElement();
        if (logger.isDebugEnabled()) {
          logger.debug("Reading extension file: " + url.toString());
        }
        extensions.add(loadProperties(url.openStream()));
      } catch (Exception ex) {
        throw new DefaultMuleException("Error loading Mule core extensions", ex);
      }
    }

    for (Properties extProps : extensions) {
      for (Map.Entry entry : extProps.entrySet()) {
        String extName = (String) entry.getKey();
        String extClass = (String) entry.getValue();
        try {
          MuleCoreExtension extension = (MuleCoreExtension) ClassUtils.instantiateClass(extClass);
          extension.setContainerClassLoader(containerClassLoader);
          result.add(extension);
        } catch (Throwable t) {
          throw new DefaultMuleException(format("Error starting Mule core extension '%s'. Extension class is %s", extName,
                                                extClass),
                                         t);
        }
      }
    }

    return result;
  }
}
