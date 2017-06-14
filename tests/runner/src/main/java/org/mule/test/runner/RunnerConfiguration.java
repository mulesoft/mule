/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.mule.test.runner.utils.AnnotationUtils.getAnnotationAttributeFromHierarchy;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Configuration for {@link ArtifactClassLoaderRunner}.
 *
 * @since 4.0
 */
public class RunnerConfiguration {

  private static final String PROVIDED_EXCLUSIONS = "providedExclusions";
  private static final String PROVIDED_INCLUSIONS = "providedInclusions";
  private static final String TEST_EXCLUSIONS = "testExclusions";
  private static final String TEST_INCLUSIONS = "testInclusions";
  private static final String EXPORT_PLUGIN_CLASSES = "exportPluginClasses";
  private static final String SHARED_RUNTIME_LIBS = "sharedRuntimeLibs";

  private List<String> providedExclusions;
  private List<String> testExclusions;
  private List<String> testInclusions;
  private List<Class> exportPluginClasses;
  private List<String> sharedRuntimeLibs;

  private String loadedFromTestClass;

  private RunnerConfiguration(Class loadedFromTestClass) {
    this.loadedFromTestClass = loadedFromTestClass.getName();
  }

  public List<Class> getExportPluginClasses() {
    return exportPluginClasses;
  }

  public List<String> getProvidedExclusions() {
    return providedExclusions;
  }

  public List<String> getSharedRuntimeLibs() {
    return sharedRuntimeLibs;
  }

  public List<String> getTestExclusions() {
    return testExclusions;
  }

  public List<String> getTestInclusions() {
    return testInclusions;
  }

  /**
   * Creates an instance of the the configuration by reading the class annotated with {@link ArtifactClassLoaderRunnerConfig}.
   *
   * @param klass Test {@link Class} annotated
   * @return a {@link RunnerConfiguration}
   */
  public static RunnerConfiguration readConfiguration(Class klass) {
    RunnerConfiguration runnerConfiguration = new RunnerConfiguration(klass);
    Class testClass = klass;

    runnerConfiguration.providedExclusions = readAttribute(PROVIDED_EXCLUSIONS, testClass);
    runnerConfiguration.testExclusions = readAttribute(TEST_EXCLUSIONS, testClass);
    runnerConfiguration.testInclusions = readAttribute(TEST_INCLUSIONS, testClass);

    runnerConfiguration.exportPluginClasses = readAttribute(EXPORT_PLUGIN_CLASSES, testClass);

    runnerConfiguration.sharedRuntimeLibs = readAttribute(SHARED_RUNTIME_LIBS, testClass);

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
  private static <E> List<E> readAttribute(String name, Class<?> klass) {
    List<E[]> valuesList =
        getAnnotationAttributeFromHierarchy(klass, ArtifactClassLoaderRunnerConfig.class,
                                            name);
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
    return sharedRuntimeLibs.equals(that.sharedRuntimeLibs);
  }

  @Override
  public int hashCode() {
    int result = providedExclusions.hashCode();
    result = 31 * result + testExclusions.hashCode();
    result = 31 * result + testInclusions.hashCode();
    result = 31 * result + exportPluginClasses.hashCode();
    result = 31 * result + sharedRuntimeLibs.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
  }
}
