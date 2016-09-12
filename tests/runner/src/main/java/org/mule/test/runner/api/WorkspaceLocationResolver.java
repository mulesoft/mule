/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import java.io.File;

import org.eclipse.aether.artifact.Artifact;

/**
 * Resolves locations of {@link Artifact}s from a IDE workspace, a Maven build session or a similar ad-hoc collection of
 * artifacts.
 *
 * @since 4.0
 */
public interface WorkspaceLocationResolver {

  /**
   * Resolves the {@link File} to file system for the given {@link Artifact}.
   *
   * @param artifactId to resolve its {@link File} from the workspace, cannot be {@code null}.
   * @return {@link File} for the given artifactId location or null if it is not present in workspace.
   */
  File resolvePath(String artifactId);

}
