/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

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
   * {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   * <p/>
   *
   * @return Maven artifacts to be excluded {@code provided} scope direct dependencies of the rootArtifact. In format
   *         {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   */
  String[] providedExclusions() default {};

  /**
   * Runtime libraries in the format of {@code [groupId]:[artifactId]} or {@code [groupId]:[artifactId]:[classifier]} to be added
   * as libraries on the test application's {@link ArtifactClassLoader}. These artifacts have to be declared as {@code test} scope
   * dependencies for the rootArtifact.
   * <p/>
   * Be aware that only the artifact would be added as shared libraries, it will not include its dependencies.
   *
   * @return array of {@link String} to define runtime libraries in order to be added as shared libraries.
   */

  String[] applicationRuntimeLibs() default {};

  /**
   * Runtime libraries in the format of {@code [groupId]:[artifactId]} or {@code [groupId]:[artifactId]:[classifier]} to be 
   * exported on the test runner's {@link ArtifactClassLoader}. These artifacts have to be declared as {@code test} scope 
   * dependencies for the rootArtifact.
   * <p/>
   * Be aware that only the artifact would be added as shared libraries, it will not include its dependencies.
   *
   * @return array of {@link String} to define runtime libraries in order to be added as shared libraries.
   */
  String[] testRunnerExportedRuntimeLibs() default {};

  /**
   * Runtime libraries in the format of {@code [groupId]:[artifactId]} or {@code [groupId]:[artifactId]:[classifier]} to be added 
   * as shared libraries to {@link ArtifactClassLoader}. These artifacts have to be declared as {@code test} scope dependencies 
   * for the rootArtifact.
   * <p/>
   * Be aware that only the artifact would be added as shared libraries, it will not include its dependencies.
   *
   * @return array of {@link String} to define runtime libraries in order to be added as shared libraries.
   */
  String[] applicationSharedRuntimeLibs() default {};

  /**
   * <b>WARNING: do not use this if you want to have a pure isolated test case.</b>
   * <p/>
   * Allows to declare an array of {@link String} fully qualified {@link Class}es that the test would need to get access to and
   * they are not exposed by the plugin. Meaning that the isolation of plugins would be broken due to these {@link Class}es would
   * be exposed no matter if the plugin doesn't expose them.
   * <p/>
   * {@link Class}es defined here will be also visible for all the tests in the module due to the {@link ClassLoader} is created
   * one per module when running tests.
   * <p/>
   * Only {@link Class}es from the rootArtifact when it is a plugin would be exposed, it is not possible to export a {@link Class}
   * that belongs to other plugins rather than rootArtifact.
   *
   * @return array of {@link Class} for those classes that has to be exposed for the test. By default is empty.
   */
  Class[] exportPluginClasses() default {};

  /**
   * Maven artifacts to be excluded from the {@code test} scope direct dependencies of the rootArtifact. In format
   * {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   *
   * @return Maven artifacts to be excluded from the {@code test} scope direct dependencies of the rootArtifact. In format
   *         {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   */
  String[] testExclusions() default {};


  /**
   * Maven artifacts to be included from the {@code test} scope direct dependencies of the rootArtifact. In format
   * {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   *
   * @return Maven artifacts to be included from the {@code test} scope direct dependencies of the rootArtifact. In format
   *         {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}.
   */
  String[] testInclusions() default {};

  /**
   * Artifacts to be included as privileged artifacts for the all the modules. In format {@code [groupId]:[artifactId]}
   * or {@code [groupId]:[artifactId]:[classifier]}.
   * <p/>
   * This allows to add test plugins as privileged artifacts without having to modify the mule-module.properties of each module.
   *
   * @return Artifacts to be included as privileged artifacts. In format {@code [groupId]:[artifactId].
   */
  String[] extraPrivilegedArtifacts() default {};

}
