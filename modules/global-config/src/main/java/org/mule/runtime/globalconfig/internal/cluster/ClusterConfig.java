/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.globalconfig.internal.cluster;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.api.annotation.NoImplement;
import org.mule.maven.client.api.model.Authentication;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.client.api.model.RemoteRepository;
import org.mule.maven.client.api.model.RepositoryPolicy;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.globalconfig.api.EnableableConfig;
import org.mule.runtime.globalconfig.api.exception.RuntimeGlobalConfigException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration builder for {@link MavenConfiguration} instances.
 */
public class ClusterConfig {

  static final String POSITION = "position";
  EnableableConfig objectStoreConfig;
  EnableableConfig lockFactoryConfig;
  EnableableConfig timeSupplierConfig;

  public EnableableConfig getObjectStoreConfig() {
    return objectStoreConfig;
  }

  public EnableableConfig getLockFactoryConfig() {
    return lockFactoryConfig;
  }

  public EnableableConfig getTimeSupplierConfig() {
    return timeSupplierConfig;
  }

  /**
   * @param config the maven configuration set by the user
   * @return a {@link MavenConfiguration} created by using the user configuration and default values set by mule.
   */
  public static MavenConfiguration buildMavenConfig(Config config) {
    try {
      String globalSettingsLocation =
          config.hasPath("globalSettingsLocation") ? config.getString("globalSettingsLocation") : null;
      String userSettingsLocation =
          config.hasPath("userSettingsLocation") ? config.getString("userSettingsLocation") : null;
      String settingsSecurityLocation =
          config.hasPath("settingsSecurityLocation") ? config.getString("settingsSecurityLocation") : null;
      String repositoryLocation =
          config.hasPath("repositoryLocation") ? config.getString("repositoryLocation") : null;
      boolean ignoreArtifactDescriptorRepositories =
          config.hasPath("ignoreArtifactDescriptorRepositories")
              ? config.getBoolean("ignoreArtifactDescriptorRepositories")
              : true;
      boolean forcePolicyUpdateNever =
          config.hasPath("forcePolicyUpdateNever") && config.getBoolean("forcePolicyUpdateNever");

      boolean forcePolicyUpdateAlways = !forcePolicyUpdateNever
          && config.hasPath("forcePolicyUpdateAlways") && config.getBoolean("forcePolicyUpdateAlways");

      boolean offLineMode = config.hasPath("offLineMode") && config.getBoolean("offLineMode");

      File globalSettingsFile = findResource(globalSettingsLocation);
      File userSettingsFile = findResource(userSettingsLocation);
      File settingsSecurityFile = findResource(settingsSecurityLocation);

      File repositoryFolder = getRuntimeRepositoryFolder();
      if (repositoryLocation != null) {
        repositoryFolder = new File(repositoryLocation);
        if (!repositoryFolder.exists()) {
          throw new RuntimeGlobalConfigException(I18nMessageFactory
              .createStaticMessage(format("Repository folder %s configured for the mule runtime does not exists",
                                          repositoryLocation)));
        }
      }
      MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder =
          MavenConfiguration.newMavenConfigurationBuilder()
              .localMavenRepositoryLocation(repositoryFolder)
              .ignoreArtifactDescriptorRepositories(ignoreArtifactDescriptorRepositories)
              .forcePolicyUpdateNever(forcePolicyUpdateNever)
              .forcePolicyUpdateAlways(forcePolicyUpdateAlways)
              .offlineMode(offLineMode);
      if (globalSettingsFile != null) {
        mavenConfigurationBuilder.globalSettingsLocation(globalSettingsFile);
      }
      if (userSettingsFile != null) {
        mavenConfigurationBuilder.userSettingsLocation(userSettingsFile);
      }
      if (settingsSecurityFile != null) {
        mavenConfigurationBuilder.settingsSecurityLocation(settingsSecurityFile);
      }

      ConfigObject repositories =
          config.hasPath("repositories") ? config.getObject("repositories") : null;
      if (repositories != null) {
        Map<String, Object> repositoriesAsMap = repositories.unwrapped();
        repositoriesAsMap.entrySet().stream().sorted(remoteRepositoriesComparator()).forEach((repoEntry) -> {
          String repositoryId = repoEntry.getKey();
          Map<String, Object> repositoryConfig = (Map<String, Object>) repoEntry.getValue();
          String url = (String) repositoryConfig.get("url");
          String username = (String) repositoryConfig.get("username");
          String password = (String) repositoryConfig.get("password");
          try {
            RemoteRepository.RemoteRepositoryBuilder remoteRepositoryBuilder = RemoteRepository.newRemoteRepositoryBuilder()
                .id(repositoryId).url(new URL(url));
            if (username != null || password != null) {
              Authentication.AuthenticationBuilder authenticationBuilder = Authentication.newAuthenticationBuilder();
              if (username != null) {
                authenticationBuilder.username(username);
              }
              if (password != null) {
                authenticationBuilder.password(password);
              }
              getRepositoryPolicy(repositoryConfig, "snapshotPolicy")
                  .ifPresent(snapshotPolicy -> remoteRepositoryBuilder.snapshotPolicy(snapshotPolicy));
              getRepositoryPolicy(repositoryConfig, "releasePolicy")
                  .ifPresent(releasePolicy -> remoteRepositoryBuilder.releasePolicy(releasePolicy));
              remoteRepositoryBuilder.authentication(authenticationBuilder.build());
            }
            mavenConfigurationBuilder.remoteRepository(remoteRepositoryBuilder.build());
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
        });
      }
      return mavenConfigurationBuilder.build();
    } catch (Exception e) {
      if (e instanceof RuntimeGlobalConfigException) {
        throw e;
      }
      throw new RuntimeGlobalConfigException(e);
    }
  }

  static Optional<RepositoryPolicy> getRepositoryPolicy(Map<String, Object> repositoryConfig, String policy) {
    if (repositoryConfig.containsKey(policy)) {
      RepositoryPolicy.RepositoryPolicyBuilder repositoryPolicyBuilder = RepositoryPolicy.newRepositoryPolicyBuilder();
      Map<String, String> snapshotPolicy = (Map<String, String>) repositoryConfig.get(policy);
      String enabled = snapshotPolicy.getOrDefault("enabled", null);
      String updatePolicy = snapshotPolicy.getOrDefault("updatePolicy", null);
      String checksumPolicy = snapshotPolicy.getOrDefault("checksumPolicy", null);
      if (enabled != null) {
        repositoryPolicyBuilder.enabled(Boolean.valueOf(enabled));
      }
      if (updatePolicy != null) {
        repositoryPolicyBuilder.updatePolicy(updatePolicy);
      }
      if (checksumPolicy != null) {
        repositoryPolicyBuilder.checksumPolicy(checksumPolicy);
      }
      return of(repositoryPolicyBuilder.build());
    }
    return empty();
  }

  static File findResource(String resourceLocation) {
    File resourceFile = null;
    if (resourceLocation != null) {
      URL resource = ClusterConfig.class.getResource(resourceLocation);
      if (resource == null) {
        resourceFile = new File(resourceLocation);
        if (!resourceFile.exists()) {
          throw new RuntimeGlobalConfigException(I18nMessageFactory.createStaticMessage(
                                                                                        format("Couldn't find file %s nor in the classpath or as absolute path",
                                                                                               resourceLocation)));
        }
      }
    }
    return resourceFile;
  }

  static Comparator<Map.Entry<String, Object>> remoteRepositoriesComparator() {
    return (firstEntry, secondEntry) -> {
      Integer firstPosition =
          valueOf(((Map<String, String>) firstEntry.getValue()).getOrDefault(POSITION, String.valueOf(MAX_VALUE)));
      Integer secondPosition = valueOf(((Map<String, String>) secondEntry.getValue())
          .getOrDefault(POSITION, String.valueOf(MAX_VALUE)));
      return firstPosition.compareTo(secondPosition);
    };
  }

  /**
   * @return the runtime repository folder for maven artifacts.
   */
  static File getRuntimeRepositoryFolder() {
    return new File(MuleFoldersUtil.getMuleBaseFolder(), "repository");
  }

  /**
   * @return creates a {@link MavenConfiguration} instance when no maven settings are defined.
   */
  public static MavenConfiguration buildNullClusterConfig() {
    return MavenConfiguration.newMavenConfigurationBuilder().localMavenRepositoryLocation(getRuntimeRepositoryFolder())
        .build();
  }

}
