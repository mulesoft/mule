/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.descriptor.api;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

import static org.apache.commons.io.FilenameUtils.separatorsToUnix;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoImplement;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains all the information needed to create a {@link ClassLoader} for a Mule artifact.
 *
 * @since 4.9
 */
@NoImplement
public interface ClassLoaderConfiguration {

  /**
   * @return the URLs to use on the {@link ClassLoader}. Non-null.
   */
  URL[] getUrls();

  /**
   * @return the class packages to be exported on the {@link ClassLoader}. Non-null.
   */
  Set<String> getExportedPackages();

  /**
   * @return the resources to be exported on the {@link ClassLoader}. Non-null.
   */
  Set<String> getExportedResources();

  /**
   * @return the Java packages to be loaded from the artifact itself, even if some other artifact in the region exports it. Non
   *         null
   */
  Set<String> getLocalPackages();

  /**
   * @return the resources to be loaded from the artifact itself, even if some other artifact in the region exports it. Non-null.
   */
  Set<String> getLocalResources();

  /**
   * @return the artifact dependencies required to create the {@link ClassLoader}. Non-null.
   */
  Set<? extends BundleDependency> getDependencies();

  /**
   * @return the Java package names to be exported as privileged API on the {@link ClassLoader}. Non-null.
   */
  Set<String> getPrivilegedExportedPackages();

  /**
   * @return the artifact IDs that have access to the privileged API defined on the {@link ClassLoader}. Each artifact is defined
   *         using Maven's groupId:artifactId. Non-null.
   */
  Set<String> getPrivilegedArtifacts();

  /**
   * @return {@code true} if the configuration should include {@code test} scope dependencies when resolving the class loader
   *         {@link URL urls} for the artifact.
   */
  boolean isIncludeTestDependencies();

}

