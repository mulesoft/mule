/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.DOMAIN;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.launcher.application.ArtifactPlugin;
import org.mule.runtime.module.launcher.application.DefaultArtifactPlugin;
import org.mule.runtime.module.launcher.artifact.ArtifactMuleContextBuilder;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;
import org.mule.runtime.module.tooling.api.artifact.TemporaryArtifact;
import org.mule.runtime.module.tooling.api.artifact.TemporaryArtifactBuilderFactory;
import org.mule.runtime.module.tooling.api.artifact.TemporaryArtifactBuilder;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingStrategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@code ToolingArtifactBuilderFactory} that allows to create a builder use to create a temporary
 * artifact for tooling services.
 *
 * @since 4.0
 */
public class DefaultTemporaryArtifactBuilderFactory implements TemporaryArtifactBuilderFactory {

  private final MuleArtifactResourcesRegistry muleArtifactResourcesRegistry;

  /**
   * Creates a {@code TemporaryToolingArtifactBuilderFactory}
   *
   * @param muleArtifactResourcesRegistry registry of mule artifact resources
   */
  public DefaultTemporaryArtifactBuilderFactory(MuleArtifactResourcesRegistry muleArtifactResourcesRegistry) {
    this.muleArtifactResourcesRegistry = muleArtifactResourcesRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TemporaryArtifactBuilder newBuilder() {
    return new TemporaryArtifactBuilder() {

      private Logger logger = LoggerFactory.getLogger(TemporaryArtifactBuilder.class);

      private ArtifactMuleContextBuilder artifactMuleContextBuilder;
      private MuleDeployableArtifactClassLoader temporaryContextClassLoader;
      private ArtifactConfiguration artifactConfiguration;
      private List<File> artifactPluginFiles = new ArrayList<>();
      private List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();
      private List<Class<? extends ConnectivityTestingStrategy>> connectivityTestingStrategyTypes = new ArrayList<>();

      @Override
      public TemporaryArtifactBuilder addConnectivityTestingStrategyType(Class<? extends ConnectivityTestingStrategy> connectionTestingServiceType) {
        connectivityTestingStrategyTypes.add(connectionTestingServiceType);
        return this;
      }

      @Override
      public TemporaryArtifactBuilder addArtifactPluginFile(File artifactPluginFile) {
        artifactPluginFiles.add(artifactPluginFile);
        return this;
      }

      private MuleContextListener createMuleContextListener() {
        return new MuleContextListener() {

          @Override
          public void onCreation(MuleContext context) {
            for (Class<? extends ConnectivityTestingStrategy> connectivityTestingStrategyType : connectivityTestingStrategyTypes) {
              context.getCustomizationService().registerCustomServiceClass(connectivityTestingStrategyType.getName(),
                                                                           connectivityTestingStrategyType);
            }
          }

          @Override
          public void onInitialization(MuleContext context) {}

          @Override
          public void onConfiguration(MuleContext context) {}
        };
      }

      @Override
      public TemporaryArtifact build() {
        try {
          String artifactId = "tooling-" + UUID.getUUID();
          File toolingTempFolder = new File(MuleContainerBootstrapUtils.getMuleTmpDir(), "tooling");

          File artifactRootFolder = new File(toolingTempFolder, artifactId);
          File tempPluginsFolder = new File(new File(artifactRootFolder, "plugins"), "lib");

          artifactPluginDescriptors.addAll(this.artifactPluginFiles.stream().map(file -> {
            try {
              return muleArtifactResourcesRegistry.getArtifactPluginDescriptorLoader().load(file, tempPluginsFolder);
            } catch (IOException e) {
              throw new MuleRuntimeException(e);
            }
          }).collect(toList()));


          temporaryContextClassLoader = muleArtifactResourcesRegistry.getTemporaryArtifactClassLoaderBuilderFactory()
              .createArtifactClassLoaderBuilder().setParentClassLoader(muleArtifactResourcesRegistry.getContainerClassLoader())
              .addArtifactPluginDescriptors(this.artifactPluginDescriptors.toArray(new ArtifactPluginDescriptor[0]))
              .setArtifactId(artifactId).build();

          List<ArtifactPlugin> artifactPlugins = temporaryContextClassLoader.getArtifactPluginClassLoaders()
              .stream().map(artifactClassLoader -> new DefaultArtifactPlugin(
                                                                             this.artifactPluginDescriptors.stream()
                                                                                 .filter(artifactDescriptor -> artifactDescriptor
                                                                                     .getName()
                                                                                     .equals(artifactDescriptor.getName()))
                                                                                 .findFirst().get(),
                                                                             artifactClassLoader))
              .collect(toList());

          artifactMuleContextBuilder = new ArtifactMuleContextBuilder().setArtifactType(DOMAIN)
              .setArtifactPlugins(artifactPlugins).setExecutionClassloader(temporaryContextClassLoader)
              .setArtifactConfiguration(artifactConfiguration).setMuleContextListener(createMuleContextListener());

          return new TemporaryArtifact() {

            public MuleContext muleContext;

            @Override
            public void start() throws MuleException {
              this.muleContext = artifactMuleContextBuilder.build();
              muleContext.start();
            }

            @Override
            public boolean isStarted() {
              if (muleContext == null) {
                return false;
              }
              return muleContext.isStarted();
            }

            @Override
            public MuleContext getMuleContext() {
              return muleContext;
            }

            @Override
            public void dispose() {
              try {
                muleContext.dispose();
              } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                  logger.debug("Failure disposing mule context", e);
                }
              }
              try {
                temporaryContextClassLoader.dispose();
              } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                  logger.debug("Failure disposing temporary context class loader", e);
                }
              }
            }
          };
        } catch (Exception e) {
          throw new MuleRuntimeException(e);
        }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public TemporaryArtifactBuilder setArtifactConfiguration(ArtifactConfiguration artifactConfiguration) {
        this.artifactConfiguration = artifactConfiguration;
        return this;
      }
    };
  }
}
