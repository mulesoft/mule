/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.mule.test.runner.utils.AnnotationUtils.findConfiguredClass;
import static org.mule.test.runner.utils.AnnotationUtils.getAnnotationAttributeFrom;
import static org.mule.test.runner.utils.AnnotationUtils.getAnnotationAttributeFromHierarchy;

import org.mule.test.runner.utils.AnnotationUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Configuration for {@link ArtifactClassLoaderRunner}.
 *
 * @since 4.0
 */
public class RunnerConfiguration {

  private static final String PROVIDED_EXCLUSIONS = "providedExclusions";
  private static final String TEST_EXCLUSIONS = "testExclusions";
  private static final String TEST_INCLUSIONS = "testInclusions";
  private static final String EXPORT_PLUGIN_CLASSES = "exportPluginClasses";
  private static final String SHARED_RUNTIME_LIBS = "sharedRuntimeLibs";
  private static final String EXTRA_PRIVILEGED_ARTIFACTS = "extraPrivilegedArtifacts";

  private Set<String> providedExclusions;
  private Set<String> testExclusions;
  private Set<String> testInclusions;
  private Set<Class> exportPluginClasses;
  private Set<String> sharedRuntimeLibs;
  private Set<String> extraPrivilegedArtifacts;

  private String loadedFromTestClass;

  private RunnerConfiguration(Class loadedFromTestClass) {
    this.loadedFromTestClass = loadedFromTestClass.getName();
  }

  public Set<Class> getExportPluginClasses() {
    return exportPluginClasses;
  }

  public Set<String> getProvidedExclusions() {
    return providedExclusions;
  }

  public Set<String> getSharedRuntimeLibs() {
    return sharedRuntimeLibs;
  }

  public Set<String> getTestExclusions() {
    return testExclusions;
  }

  public Set<String> getTestInclusions() {
    return testInclusions;
  }

  public Set<String> getExtraPrivilegedArtifacts() {
    return extraPrivilegedArtifacts;
  }

  /**
   * Creates an instance of the the configuration by reading the class annotated with {@link ArtifactClassLoaderRunnerConfig}.
   * <p/>
   * Configuration is created by searching in the class hierarchy for {@code testClass} which classes are annotated with {@link ArtifactClassLoaderRunnerConfig}
   * and creating a new configuration for the test. Some configuration attributes, as {@value PROVIDED_EXCLUSIONS}, {@value TEST_EXCLUSIONS} and {@value TEST_INCLUSIONS}, are
   * the result of collecting the same attribute from all the configured classes/interfaces in the test class's hierarchy.
   * <p/>
   * The rest of the attributes, {@value EXPORT_PLUGIN_CLASSES}, {@value SHARED_RUNTIME_LIBS} and {@value EXTRA_PRIVILEGED_ARTIFACTS},
   * are taken from the first class in the hierarchy that is configured.
   *
   * @param testClass Test {@link Class} annotated
   * @return a {@link RunnerConfiguration}
   * @see AnnotationUtils#findConfiguredClass(java.lang.Class)
   */
  public static RunnerConfiguration readConfiguration(Class testClass) {
    RunnerConfiguration runnerConfiguration = new RunnerConfiguration(testClass);

    runnerConfiguration.providedExclusions = new HashSet<>(readAttributeFromHierarchy(PROVIDED_EXCLUSIONS, testClass));
    runnerConfiguration.testExclusions = new HashSet<>(readAttributeFromHierarchy(TEST_EXCLUSIONS, testClass));
    runnerConfiguration.testInclusions = new HashSet<>(readAttributeFromHierarchy(TEST_INCLUSIONS, testClass));

    Class configuredClass = findConfiguredClass(testClass);
    runnerConfiguration.exportPluginClasses = new HashSet<>(readAttributeFromClass(EXPORT_PLUGIN_CLASSES, configuredClass));
    runnerConfiguration.sharedRuntimeLibs = new HashSet<>(readAttributeFromClass(SHARED_RUNTIME_LIBS, configuredClass));
    runnerConfiguration.extraPrivilegedArtifacts =
        new HashSet<>(readAttributeFromClass(EXTRA_PRIVILEGED_ARTIFACTS, configuredClass));

    return runnerConfiguration;
  }

  /**
   * Reads the attribute from the klass annotated and does a flatMap with the list of values.
   *
   * @param name attribute/method name of the annotation {@link ArtifactClassLoaderRunnerConfig} to be obtained
   * @param klass {@link Class} from where the annotated attribute will be read
   * @param <E> generic type
   * @return {@link List} of values
   */
  private static <E> List<E> readAttributeFromHierarchy(String name, Class<?> klass) {
    List<E[]> valuesList =
        getAnnotationAttributeFromHierarchy(klass, ArtifactClassLoaderRunnerConfig.class,
                                            name);
    return valuesList.stream().flatMap(Arrays::stream).distinct().collect(toList());
  }

  private static <E> List<E> readAttributeFromClass(String name, Class<?> klass) {
    List<E[]> valuesList = singletonList(getAnnotationAttributeFrom(klass, ArtifactClassLoaderRunnerConfig.class, name));
    return valuesList.stream().flatMap(Arrays::stream).distinct().collect(toList());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RunnerConfiguration that = (RunnerConfiguration) o;

    if (!providedExclusions.equals(that.providedExclusions)) {
      return false;
    }
    if (!testExclusions.equals(that.testExclusions)) {
      return false;
    }
    if (!testInclusions.equals(that.testInclusions)) {
      return false;
    }
    if (!exportPluginClasses.equals(that.exportPluginClasses)) {
      return false;
    }
    if (!extraPrivilegedArtifacts.equals(that.extraPrivilegedArtifacts)) {
      return false;
    }
    return sharedRuntimeLibs.equals(that.sharedRuntimeLibs);
  }

  @Override
  public int hashCode() {
    int result = providedExclusions.hashCode();
    result = 31 * result + testExclusions.hashCode();
    result = 31 * result + testInclusions.hashCode();
    result = 31 * result + exportPluginClasses.hashCode();
    result = 31 * result + sharedRuntimeLibs.hashCode();
    result = 31 * result + extraPrivilegedArtifacts.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
  }
}
