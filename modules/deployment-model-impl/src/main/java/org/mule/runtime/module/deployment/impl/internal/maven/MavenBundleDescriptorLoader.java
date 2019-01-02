/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getMavenConfig;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;

import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

import org.apache.maven.model.Model;

/**
 * Loads a {@link BundleDescriptor} using Maven to extract the relevant information from a Mule artifact's
 * {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file.
 */
public class MavenBundleDescriptorLoader implements BundleDescriptorLoader {

  private static final String JAR = "jar";

  private BundleDescriptorLoader bundleDescriptorLoader;

  public MavenBundleDescriptorLoader() {
    bundleDescriptorLoader = new MuleContainerBundleDescriptorLoader();
  }

  public MavenBundleDescriptorLoader(MavenClient mavenClient) {
    bundleDescriptorLoader = new FixedMavenBundleDescriptorLoader(mavenClient);
  }

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  /**
   * Looks for the POM file within the current {@code artifactFile} jar or folder structure (under
   * {@link org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor#MULE_ARTIFACT_FOLDER} folder)
   * to retrieve the artifact locator.
   *
   * @param artifactFile {@link File} with the content of the artifact to work with. Non null
   * @param attributes collection of attributes describing the loader. Non null.
   * @param artifactType the type of the artifact of the descriptor to be loaded.
   * @return a locator of the coordinates of the current artifact
   * @throws ArtifactDescriptorCreateException if the bundle descriptor cannot be loaded.
   */
  @Override
  public BundleDescriptor load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    if (isHeavyPackage(artifactFile)) {
      File classLoaderModelDescriptor = getClassLoaderModelDescriptor(artifactFile);

      org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel = deserialize(classLoaderModelDescriptor);

      return new BundleDescriptor.Builder()
          .setArtifactId(packagerClassLoaderModel.getArtifactCoordinates().getArtifactId())
          .setGroupId(packagerClassLoaderModel.getArtifactCoordinates().getGroupId())
          .setVersion(packagerClassLoaderModel.getArtifactCoordinates().getVersion())
          .setType(packagerClassLoaderModel.getArtifactCoordinates().getType())
          .setClassifier(packagerClassLoaderModel.getArtifactCoordinates().getClassifier())
          .build();
    } else {
      return getBundleDescriptor(artifactFile, artifactType);
    }
  }

  public BundleDescriptor getBundleDescriptor(File artifactFile, ArtifactType artifactType) {
    return bundleDescriptorLoader.getBundleDescriptor(artifactFile, artifactType);
  }

  private boolean isHeavyPackage(File artifactFile) {
    return getClassLoaderModelDescriptor(artifactFile).exists();
  }

  protected File getClassLoaderModelDescriptor(File artifactFile) {
    if (artifactFile.isDirectory()) {
      return new File(artifactFile, CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION);
    } else {
      return new File(artifactFile.getParent(), CLASSLOADER_MODEL_JSON_DESCRIPTOR);
    }
  }

  interface BundleDescriptorLoader {

    BundleDescriptor getBundleDescriptor(File artifactFile, ArtifactType artifactType);

  }


  private class MuleContainerBundleDescriptorLoader implements BundleDescriptorLoader {

    private MavenClientProvider mavenClientProvider;
    private MavenConfiguration mavenRuntimeConfig;

    private FixedMavenBundleDescriptorLoader fixedMavenBundleDescriptorLoader;

    private StampedLock lock = new StampedLock();

    MuleContainerBundleDescriptorLoader() {
      mavenClientProvider = discoverProvider(MavenClientProvider.class.getClassLoader());
      mavenRuntimeConfig = getMavenConfig();

      createFixedMavenBundleDescriptorLoader();
    }

    private void createFixedMavenBundleDescriptorLoader() {
      fixedMavenBundleDescriptorLoader =
          new FixedMavenBundleDescriptorLoader(mavenClientProvider.createMavenClient(mavenRuntimeConfig));
    }

    @Override
    public BundleDescriptor getBundleDescriptor(File artifactFile, ArtifactType artifactType) {
      long stamp = lock.readLock();
      try {
        MavenConfiguration updatedMavenConfiguration = getMavenConfig();
        if (!mavenRuntimeConfig.equals(updatedMavenConfiguration)) {
          long writeStamp = lock.tryConvertToWriteLock(stamp);
          if (writeStamp == 0L) {
            lock.unlockRead(stamp);
            stamp = lock.writeLock();
          } else {
            stamp = writeStamp;
          }
          if (!mavenRuntimeConfig.equals(updatedMavenConfiguration)) {
            mavenRuntimeConfig = updatedMavenConfiguration;
            createFixedMavenBundleDescriptorLoader();
          }
        }

        return fixedMavenBundleDescriptorLoader.getBundleDescriptor(artifactFile, artifactType);
      } finally {
        lock.unlock(stamp);
      }
    }
  }

  private class FixedMavenBundleDescriptorLoader implements BundleDescriptorLoader {

    private MavenClient mavenClient;
    private File temporaryFolder;

    public FixedMavenBundleDescriptorLoader(MavenClient mavenClient) {
      this.mavenClient = mavenClient;

      File localMavenRepositoryLocation = mavenClient.getMavenConfiguration().getLocalMavenRepositoryLocation();
      temporaryFolder = new File(localMavenRepositoryLocation, ".mule");
      if (!temporaryFolder.exists() && !temporaryFolder.mkdirs()) {
        throw new MuleRuntimeException(createStaticMessage("Cannot create a temporary folder for loading bundle descriptor at %s",
                                                           temporaryFolder.getAbsolutePath()));
      }
    }

    @Override
    public BundleDescriptor getBundleDescriptor(File artifactFile, ArtifactType artifactType) {
      Model model;
      if (artifactFile.isDirectory()) {
        model = mavenClient.getEffectiveModel(artifactFile, empty());
      } else {
        model = mavenClient.getEffectiveModel(artifactFile, of(temporaryFolder));
      }

      return new BundleDescriptor.Builder()
          .setArtifactId(model.getArtifactId())
          .setGroupId(model.getGroupId())
          .setVersion(model.getVersion())
          .setType(JAR)
          // Handle manually the packaging for mule plugin as the mule plugin maven plugin defines the packaging as mule-extension
          .setClassifier(artifactType.equals(ArtifactType.PLUGIN) ? MULE_PLUGIN_CLASSIFIER : model.getPackaging())
          .build();
    }
  }
}
