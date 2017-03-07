/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.temporary;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.logging.log4j.core.util.FileUtils.getFileExtension;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.DOMAIN;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifact;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifactBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifactBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPlugin;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@code ToolingArtifactBuilderFactory} that allows to create a builder use to create a temporary
 * artifact for tooling services.
 *
 * @since 4.0
 */
public class DefaultTemporaryArtifactBuilderFactory implements TemporaryArtifactBuilderFactory {

  private final ExecutorService deleteService;
  private final MuleArtifactResourcesRegistry muleArtifactResourcesRegistry;
  private ArtifactContext artifactContext;

  /**
   * Creates a {@code TemporaryToolingArtifactBuilderFactory}
   *
   * @param muleArtifactResourcesRegistry registry of mule artifact resources
   */
  public DefaultTemporaryArtifactBuilderFactory(MuleArtifactResourcesRegistry muleArtifactResourcesRegistry) {
    this.muleArtifactResourcesRegistry = muleArtifactResourcesRegistry;
    this.deleteService = newSingleThreadExecutor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TemporaryArtifactBuilder newBuilder() {
    return new TemporaryArtifactBuilder() {

      private static final String JAR_EXTENSION = "jar";

      private Logger logger = LoggerFactory.getLogger(TemporaryArtifactBuilder.class);

      private File artifactRootFolder;
      private ArtifactContextBuilder artifactContextBuilder;
      private MuleDeployableArtifactClassLoader temporaryContextClassLoader;
      private ArtifactDeclaration artifactDeclaration;
      private List<File> artifactPluginFiles = new ArrayList<>();
      private List<File> artifactLibraryFiles = new ArrayList<>();
      private Set<ArtifactPluginDescriptor> artifactPluginDescriptors = new HashSet<>();
      private List<Class<? extends ConnectivityTestingStrategy>> connectivityTestingStrategyTypes = new ArrayList<>();

      @Override
      public TemporaryArtifactBuilder addArtifactLibraryFile(File artifactLibraryFile) {
        checkArgument(getFileExtension(artifactLibraryFile).equalsIgnoreCase(JAR_EXTENSION),
                      "artifactLibraryFile has to be a jar file");
        artifactLibraryFiles.add(artifactLibraryFile);
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

          artifactRootFolder = new File(toolingTempFolder, artifactId);
          artifactPluginDescriptors.addAll(this.artifactPluginFiles.stream().map(file -> {
            try {
              return muleArtifactResourcesRegistry.getArtifactPluginDescriptorLoader().load(file);
            } catch (IOException e) {
              throw new MuleRuntimeException(e);
            }
          }).collect(toList()));

          artifactPluginDescriptors =
              muleArtifactResourcesRegistry.getPluginDependenciesResolver().resolve(artifactPluginDescriptors);

          final TemporaryArtifactClassLoaderBuilder temporaryArtifactClassLoaderBuilder =
              muleArtifactResourcesRegistry.getTemporaryArtifactClassLoaderBuilderFactory()
                  .createArtifactClassLoaderBuilder()
                  .setParentClassLoader(muleArtifactResourcesRegistry.getContainerClassLoader())
                  .addArtifactPluginDescriptors(this.artifactPluginDescriptors.toArray(new ArtifactPluginDescriptor[0]))
                  .setArtifactId(artifactId);

          // Just add the "classes" folder in order to avoid issues when looking for log4j configuration
          final File classes = new File(artifactRootFolder, "classes");
          if (!classes.mkdir()) {
            throw new MuleRuntimeException(createStaticMessage("Couldn't create classes folder for temporary application"));
          }
          temporaryArtifactClassLoaderBuilder.addUrl(classes.toURI().toURL());

          artifactLibraryFiles.stream().forEach(file -> {
            try {
              temporaryArtifactClassLoaderBuilder.addUrl(file.toURI().toURL());
            } catch (MalformedURLException e) {
              throw new MuleRuntimeException(e);
            }
          });
          temporaryContextClassLoader = temporaryArtifactClassLoaderBuilder.build();

          List<ArtifactPlugin> artifactPlugins = temporaryContextClassLoader.getArtifactPluginClassLoaders()
              .stream().map(artifactClassLoader -> new DefaultArtifactPlugin(artifactClassLoader.getArtifactId(),
                                                                             this.artifactPluginDescriptors.stream()
                                                                                 .filter(artifactDescriptor -> artifactDescriptor
                                                                                     .getName()
                                                                                     .equals(artifactDescriptor.getName()))
                                                                                 .findFirst().get(),
                                                                             artifactClassLoader))
              .collect(toList());

          artifactContextBuilder = ArtifactContextBuilder.newBuilder().setArtifactType(DOMAIN)
              .setArtifactPlugins(artifactPlugins).setExecutionClassloader(temporaryContextClassLoader)
              .setServiceRepository(muleArtifactResourcesRegistry.getServiceManager())
              .setArtifactDeclaration(artifactDeclaration).setMuleContextListener(createMuleContextListener())
              .setClassLoaderRepository(muleArtifactResourcesRegistry.getArtifactClassLoaderManager());

          return new TemporaryArtifact() {

            public MuleContext muleContext;

            @Override
            public void start() throws MuleException {
              artifactContext = artifactContextBuilder.build();
              this.muleContext = artifactContext.getMuleContext();
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
            public ConnectivityTestingService getConnectivityTestingService() {
              return artifactContext.getConnectivityTestingService();
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
              deleteService.submit(() -> {
                deleteQuietly(artifactRootFolder);
              });
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
      public TemporaryArtifactBuilder setArtifactDeclaration(ArtifactDeclaration artifactDeclaration) {
        this.artifactDeclaration = artifactDeclaration;
        return this;
      }
    };
  }
}
