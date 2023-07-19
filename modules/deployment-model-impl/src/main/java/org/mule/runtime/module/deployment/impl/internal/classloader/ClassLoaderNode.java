/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.classloader;

import java.util.List;
import java.util.Optional;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

/**
 * Simplified representation of the classloading hierarchy mainly used for diagnostics purposes.
 * <p/>
 * Contains a representation of the hierarchy and has utility methods to provide detail information to the user for
 * troubleshooting purposes.
 *
 * @since 4.2
 */
public interface ClassLoaderNode {

  /**
   * @return a unique ID for the classloader but meaningful enough for the user to understand which classloader is referring to.
   */
  String getId();

  /**
   * @return a list of {@link ClassLoaderNode} this classloader node will delegate to for finding resources or classes.
   */
  List<ClassLoaderNode> getDelegateNodes();

  /**
   * @return a reference to the parent classloader node in the hierarchy.
   */
  Optional<ClassLoaderNode> getParent();

  /**
   * @return the {@link ArtifactClassLoader} this node refers to.
   */
  ArtifactClassLoader getArtifactClassLoader();

  /**
   * @return indicates if the classloader node is pointing to a mule plugin classloader.
   */
  boolean isMulePlugin();

  /**
   * Find the {@link ClassLoaderNode} within this node hierarchy only within the reachable class loaders.
   * <p/>
   * This method assumes that the requested classloader exists in the hierarchy.
   *
   * @param artifactClassLoader the classloader owned by the {@link ClassLoaderNode} to search.
   * @return the found {@link ClassLoaderNode}
   */
  ClassLoaderNode findClassLoaderNode(ArtifactClassLoader artifactClassLoader);

  /**
   * Find the list of {@link ClassLoaderNode} that owns a private resource with name {@code resourceName}.
   *
   * @param resourceName the name of the resource to find
   * @return the list of {@link ClassLoaderNode} owning the resource.
   */
  List<ClassLoaderNode> findPossibleResourceOwners(String resourceName);

  /**
   * Find the list of {@link ClassLoaderNode} that owns a private class with name {@code className}.
   *
   * @param className the name of the class lo find
   * @return the list of {@link ClassLoaderNode} owning the class.
   */
  List<ClassLoaderNode> findPossibleClassOwners(String className);
}
