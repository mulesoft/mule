/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a configuration needed by {@link ArtifactClassLoaderRunner} in order to run the tests in the module.
 * <p/>
 * Be aware that this annotation will be loaded for the whole module being tested, it is not supported to have different annotated
 * values for different test classes due to in order to improve the performance a {@link ClassLoader} is created only the first
 * time and used to run several tests.
 * <p/>
 * A best practice is to have a base abstract class for your module tests that extends
 * {@link org.mule.functional.junit4.ArtifactFunctionalTestCase} or
 * {@link org.mule.functional.junit4.MuleArtifactFunctionalTestCase} for mule internal tests, to define the isolation runner
 * configuration using {@code this} annotation that applies to all the tests that are being executed for the same module.
 * <p/>
 * The concept of module of execution is being backed by a JVM where a JVM is created for running the whole test of the module, in
 * case of maven either IDEs.
 *
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ArtifactClassLoaderRunnerConfig {

  /**
   * Maven artifacts to be excluded from the {@code provided} scope direct dependencies of the rootArtifact. In format
   * {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   * <p/>
   * {@link #plugins()} Maven artifacts if declared will be considered to be excluded from being added as {@code provided} due to
   * they are going to be added to its class loaders.
   *
   * @return Maven artifacts to be excluded {@code provided} scope direct dependencies of the rootArtifact. In format
   *         {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   */
  String[] providedExclusions() default {};

  /**
   * Maven artifacts to be included from the {@code provided} scope direct dependencies of the rootArtifact. In format
   * {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   * <p/>
   * This artifacts have to be declared as {@code provided} scope in rootArtifact direct dependencies and no matter if they were
   * excluded or not from {@link #providedExclusions()} and {@link #plugins()}. Meaning that the same artifact could ended up
   * being added to the container class loader and as plugin.
   *
   * @return Maven artifacts to be explicitly included from the {@code provided} scope direct dependencies of the rootArtifact. In
   *         format {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   */
  String[] providedInclusions() default {};

  /**
   * Plugins in the format of {@code <groupId>:<artifactId>} to be loaded and registered to Mule Container during the execution of
   * the test. {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} will be created for each plugin.
   * <p/>
   * If the current artifact being tested is a plugin it would need to be declared here the groupId and artifactId, its
   * {@code /target/classes/} folder and Maven {@code compile} dependencies will be used to build the
   * {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}.
   * <p/>
   * Otherwise any plugin artifact declared on this list should be declared as Maven dependency with scope {@code provided}, the
   * version of the plugin to be resolved will be the one defined in the Maven dependency.
   * <p/>
   *
   * @return array of {@link String} to define plugins in order to create for each a plugin {@link ClassLoader}
   */
  String[] plugins() default {};

  /**
   * <b>WARNING: do not use this if you want to have a pure isolated test case.</b>
   * <p/>
   * Allows to declare an array of {@link String} fully qualified {@link Class}es that the test would need to get access to and
   * they are not exposed by the plugin. Meaning that the isolation of plugins would be broken due to these {@link Class}es would
   * be exposed no matter if the plugin doesn't expose them.
   * <p/>
   * For each class defined here it will look the source code location of that file in order to get which plugin or extension has
   * to be appended to export the {@link Class} defined.
   * <p/>
   * {@link Class}es defined here will be also visible for all the tests in the module due to the {@link ClassLoader} is created
   * one per module when running tests.
   * <p/>
   * Only {@link Class}es from the plugin code would be exposed, it is not possible to export a {@link Class} that belongs to a
   * third-party library.
   *
   * @return array of {@link Class} for those classes that has to be exposed for the test. By default is empty.
   */
  Class[] exportPluginClasses() default {};

  /**
   * Maven artifacts to be excluded from the {@code test} scope direct dependencies of the rootArtifact. In format
   * {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   *
   * @return Maven artifacts to be excluded from the {@code test} scope direct dependencies of the rootArtifact. In format
   *         {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   */
  String[] testExclusions() default {};


  /**
   * Maven artifacts to be included from the {@code test} scope direct dependencies of the rootArtifact. In format
   * {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   *
   * @return Maven artifacts to be included from the {@code test} scope direct dependencies of the rootArtifact. In format
   *         {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   */
  String[] testInclusions() default {};

}
