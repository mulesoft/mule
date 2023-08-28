/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.api;

/**
 * Defines the type of artifact classification.
 * <p/>
 * The type defines how to handle the {@link org.eclipse.aether.artifact.Artifact} and in which {@link ClassLoader} should be
 * added.
 *
 * @since 4.0
 */
public enum ArtifactClassificationType {
  /**
   * {@link org.eclipse.aether.artifact.Artifact} should be classified as an application. If the artifact has main code, it is
   * added to the APPLICATION {@link ClassLoader}.
   */
  APPLICATION,

  /**
   * {@link org.eclipse.aether.artifact.Artifact} should be classified as plugin. The main code of the
   * {@link org.eclipse.aether.artifact.Artifact} is added to the PLUGIN {@link ClassLoader}.
   */
  PLUGIN,

  /**
   * {@link org.eclipse.aether.artifact.Artifact} should be classified as module. The main code of the
   * {@link org.eclipse.aether.artifact.Artifact} is added to the CONTAINER {@link ClassLoader}.
   */
  MODULE,

  /**
   * {@link org.eclipse.aether.artifact.Artifact} should be classified as service. The main code of the
   * {@link org.eclipse.aether.artifact.Artifact} is added to the SERVICE {@link ClassLoader}.
   */
  SERVICE
}
