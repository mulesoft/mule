/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Abstract implementation of {@link ClassLoaderConfigurationLoader} that resolves the dependencies for all the mule artifacts and
 * create the {@link ClassLoaderConfiguration}. It lets the implementations of this class to add artifact's specific class loader
 * URLs
 *
 * @since 4.0
 *
 * @deprecated
 */
// TODO - W-11098291: remove
@Deprecated
public abstract class AbstractMavenClassLoaderModelLoader extends AbstractMavenClassLoaderConfigurationLoader {

  public AbstractMavenClassLoaderModelLoader(Optional<MavenClient> mavenClient) {
    super(mavenClient);
  }

  public AbstractMavenClassLoaderModelLoader(Optional<MavenClient> mavenClient, Supplier<JarExplorer> jarExplorerFactory) {
    super(mavenClient, jarExplorerFactory);
  }

}
