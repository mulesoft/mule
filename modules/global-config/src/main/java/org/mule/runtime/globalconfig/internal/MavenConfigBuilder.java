/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.globalconfig.internal;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import org.mule.maven.client.api.model.Authentication;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.client.api.model.RemoteRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.globalconfig.api.exception.RuntimeGlobalConfigException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;

/**
 * Configuration builder for {@link MavenConfiguration} instances.
 */
public class MavenConfigBuilder {

  private static final String POSITION = "position";

  /**
   * @param mavenConfig the maven configuration set by the user
   * @return a {@link MavenConfiguration} created by using the user configuration and default values set by mule.
   */
  public static MavenConfiguration buildMavenConfig(Config mavenConfig) {
    try {
      String globalSettingsLocation =
          mavenConfig.hasPath("globalSettingsLocation") ? mavenConfig.getString("globalSettingsLocation") : null;
      String userSettingsLocation =
          mavenConfig.hasPath("userSettingsLocation") ? mavenConfig.getString("userSettingsLocation") : null;
      String repositoryLocation =
          mavenConfig.hasPath("repositoryLocation") ? mavenConfig.getString("repositoryLocation") : null;

      File globalSettingsFile = findResource(globalSettingsLocation);
      File userSettingsFile = findResource(userSettingsLocation);

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
          MavenConfiguration.newMavenConfigurationBuilder().localMavenRepositoryLocation(repositoryFolder);
      if (globalSettingsFile != null) {
        mavenConfigurationBuilder.globalSettingsLocation(globalSettingsFile);
      }
      if (userSettingsFile != null) {
        mavenConfigurationBuilder.userSettingsLocation(userSettingsFile);
      }

      ConfigObject repositories =
          mavenConfig.hasPath("repositories") ? mavenConfig.getObject("repositories") : null;
      if (repositories != null) {
        Map<String, Object> repositoriesAsMap = repositories.unwrapped();
        repositoriesAsMap.entrySet().stream().sorted(remoteRepositoriesComparator()).forEach((repoEntry) -> {
          String repositoryId = repoEntry.getKey();
          Map<String, String> repositoryConfig = (Map<String, String>) repoEntry.getValue();
          String url = repositoryConfig.get("url");
          String username = repositoryConfig.get("username");
          String password = repositoryConfig.get("password");
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

  private static File findResource(String resourceLocation) {
    File resourceFile = null;
    if (resourceLocation != null) {
      URL resource = MavenConfigBuilder.class.getResource(resourceLocation);
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

  private static Comparator<Map.Entry<String, Object>> remoteRepositoriesComparator() {
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
  private static File getRuntimeRepositoryFolder() {
    return new File(MuleFoldersUtil.getMuleBaseFolder(), "repository");
  }

  /**
   * @return creates a {@link MavenConfiguration} instance when no maven settings are defined.
   */
  public static MavenConfiguration buildNullMavenConfig() {
    return MavenConfiguration.newMavenConfigurationBuilder().localMavenRepositoryLocation(getRuntimeRepositoryFolder())
        .build();
  }
}
