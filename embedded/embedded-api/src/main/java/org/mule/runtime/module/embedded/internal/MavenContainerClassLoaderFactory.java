/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.internal;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.toFile;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.api.MavenClient;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates a {@link ClassLoader} with the {@link URL}'s for the Container.
 *
 * @since 4.0
 */
// TODO MULE-11925 Improve MavenContainerClassLoaderFactory so it can work in terms of Dependency instead of URLs
public class MavenContainerClassLoaderFactory {

  private static final String CONTAINER_BOM_GROUP_ID = "org.mule.runtime";
  private static final String CONTAINER_BOM_ARTIFACT_ID = "mule-runtime-impl-bom";

  private final MavenClient mavenClient;

  public MavenContainerClassLoaderFactory(MavenClient mavenClient) {
    this.mavenClient = mavenClient;
  }

  /**
   * Creates the {@link ClassLoader} Container for a given version.
   *
   * @param version Maven version. Not null.
   * @param containerBaseFolder
   * @return a {@link ClassLoader} Container.
   */
  public ClassLoader create(String version, ClassLoader parentClassLoader, URL containerBaseFolder) {
    try {
      BundleDescriptor containerBomBundleDescriptor = getContainerBomBundleDescriptor(version);
      List<BundleDependency> bundleDependencies =
          mavenClient.resolveBundleDescriptorDependencies(false, containerBomBundleDescriptor);

      List<URL> urls = bundleDependencies.stream()
          .filter(bundleDependency -> !bundleDependency.getDescriptor().getGroupId().equals("org.mule.services"))
          .map(BundleDependency::getBundleUrl)
          .collect(Collectors.toList());
      urls = new ArrayList<>(urls);
      File containerFolderFile = toFile(containerBaseFolder);
      // the URL has to be constructed this way since File.toURI().toURL() gets rid of the final slash
      urls.add(new URL(new File(containerFolderFile, "conf").toURI().toString() + "/"));
      return new URLClassLoader(urls.toArray(new URL[urls.size()]), parentClassLoader);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<URL> getServices(String version) {
    BundleDescriptor containerBomBundleDescriptor = getContainerBomBundleDescriptor(version);
    List<BundleDependency> containerDependencies =
        mavenClient.resolveBundleDescriptorDependencies(false, containerBomBundleDescriptor);
    List<URL> urls = containerDependencies.stream()
        .map(BundleDependency::getBundleUrl)
        .collect(Collectors.toList());
    return urls.stream().filter(u -> u.getFile().toLowerCase().endsWith(".zip")).collect(toList());
  }

  private BundleDescriptor getContainerBomBundleDescriptor(String version) {
    return new BundleDescriptor.Builder()
        .setGroupId(CONTAINER_BOM_GROUP_ID)
        .setArtifactId(CONTAINER_BOM_ARTIFACT_ID)
        .setVersion(version)
        .setType("pom").build();
  }


}
