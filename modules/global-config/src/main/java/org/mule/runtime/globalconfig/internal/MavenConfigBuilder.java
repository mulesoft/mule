/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.globalconfig.internal;

import static java.lang.String.format;
import org.mule.maven.client.api.Authentication;
import org.mule.maven.client.api.MavenConfiguration;
import org.mule.maven.client.api.RemoteRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.globalconfig.api.exception.RuntimeGlobalConfigException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Configuration builder for {@link MavenConfiguration} instances.
 */
public class MavenConfigBuilder {

  /**
   * @param mavenConfig the maven configuration set by the user
   * @return a {@link MavenConfiguration} created by using the user configuration and default values set by mule.
   */
  public static MavenConfiguration buildMavenConfig(Config mavenConfig) {
    try {

      String mavenSettingsLocation =
          mavenConfig.hasPath("mavenSettingsLocation") ? mavenConfig.getString("mavenSettingsLocation") : null;
      String repositoryLocation =
          mavenConfig.hasPath("repositoryLocation") ? mavenConfig.getString("repositoryLocation") : null;
      File mavenSettingsFile = null;
      if (mavenSettingsLocation != null) {
        URL resource = MavenConfigBuilder.class.getResource(mavenSettingsLocation);
        if (resource == null) {
          mavenSettingsFile = new File(mavenSettingsLocation);
          if (!mavenSettingsFile.exists()) {
            throw new RuntimeGlobalConfigException(I18nMessageFactory.createStaticMessage(
                                                                                          format("Couldn't find file %s nor in the classpath or as absolute path",
                                                                                                 mavenSettingsLocation)));
          }
        }
      }
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
          MavenConfiguration.newMavenConfigurationBuilder().withLocalMavenRepositoryLocation(repositoryFolder);
      if (mavenSettingsFile != null) {
        mavenConfigurationBuilder.withGlobalSettingsLocation(mavenSettingsFile);
      }

      ConfigObject repositories =
          mavenConfig.hasPath("repositories") ? mavenConfig.getObject("repositories") : null;
      if (repositories != null) {
        Map<String, Object> repositoriesAsMap = repositories.unwrapped();
        repositoriesAsMap.entrySet().forEach((repoEntry) -> {
          String repositoryId = repoEntry.getKey();
          Map<String, String> repositoryConfig = (Map<String, String>) repoEntry.getValue();
          String url = repositoryConfig.get("url");
          String username = repositoryConfig.get("username");
          String password = repositoryConfig.get("password");
          try {
            RemoteRepository.RemoteRepositoryBuilder remoteRepositoryBuilder = RemoteRepository.newRemoteRepositoryBuilder()
                .withId(repositoryId).withUrl(new URL(url));
            if (username != null || password != null) {
              Authentication.AuthenticationBuilder authenticationBuilder = Authentication.newAuthenticationBuilder();
              if (username != null) {
                authenticationBuilder.withUsername(username);
              }
              if (password != null) {
                authenticationBuilder.withPassword(password);
              }
              remoteRepositoryBuilder.withAuthentication(authenticationBuilder.build());
            }
            mavenConfigurationBuilder.withRemoteRepository(remoteRepositoryBuilder.build());
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
    return MavenConfiguration.newMavenConfigurationBuilder().withLocalMavenRepositoryLocation(getRuntimeRepositoryFolder())
        .build();
  }
}
