/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classloader;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.function.Function.identity;
import static org.apache.commons.lang3.ClassUtils.getPackageName;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.api.util.StringMessageUtils.DEFAULT_MESSAGE_WIDTH;
import org.mule.runtime.core.api.util.StringMessageUtils;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ClassLoaderFilter} that decorates a {@link ClassLoaderFilter} to allow exporting classes by name that
 * are not exported by the original {@link ClassLoaderFilter}. For resources it delegates to the original
 * {@link ClassLoaderFilter}.
 *
 * @since 4.0
 */
public final class TestArtifactClassLoaderFilter implements ArtifactClassLoaderFilter {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ArtifactClassLoaderFilter classLoaderFilter;
  private final Map<String, Object> exportedClasses;
  private final Set<String> exportedPackages;

  /**
   * Creates an extended {@link ClassLoaderFilter} to exporte classes that are not exported as packages in the original filter.
   *
   * @param classLoaderFilter the original filter. Not null.
   * @param exportedClasses a {@link List} of {@link Class}es to export in addition to the original filter. Not null.
   */
  public TestArtifactClassLoaderFilter(final ArtifactClassLoaderFilter classLoaderFilter, final List<Class> exportedClasses) {
    checkNotNull(classLoaderFilter, "classLoaderFilter cannot be null");
    checkNotNull(exportedClasses, "exportedClasses cannot be null");

    this.classLoaderFilter = classLoaderFilter;
    this.exportedClasses = exportedClasses.stream().collect(Collectors.toMap(Class::getName, identity()));
    exportedPackages = new HashSet<>(classLoaderFilter.getExportedClassPackages());
    exportedPackages.addAll(exportedClasses.stream().map(clazz -> getPackageName(clazz.getName())).collect(Collectors.toList()));
  }

  /**
   * It delegates to the original {@link ClassLoaderFilter} if it is not exported it will check againts the list of exported
   * classes.
   *
   * @param name class name to check. Non empty.
   * @return true if the resource is exported, false otherwise
   */
  @Override
  public boolean exportsClass(final String name) {
    checkArgument(!isEmpty(name), "Class name cannot be empty");

    boolean exported = classLoaderFilter.exportsClass(name);
    if (!exported) {
      exported = exportedClasses.get(name) != null;
      if (exported) {
        logger.warn(StringMessageUtils
            .getBoilerPlate(newArrayList("WARNING:", " ",
                                         "Class: '" + name + "' is NOT exposed by the plugin but it will be visible "
                                             + "due to it was manually forced to be exported for testing purposes.",
                                         " ",
                                         "Check if this is really necessary, this class won't be visible in standalone mode."),
                            '*', DEFAULT_MESSAGE_WIDTH));
      }
    }
    return exported;
  }

  @Override
  public boolean exportsResource(final String name) {
    return classLoaderFilter.exportsResource(name);
  }

  @Override
  public Set<String> getExportedClassPackages() {
    return exportedPackages;
  }

  @Override
  public Set<String> getExportedResources() {
    return classLoaderFilter.getExportedResources();
  }
}
