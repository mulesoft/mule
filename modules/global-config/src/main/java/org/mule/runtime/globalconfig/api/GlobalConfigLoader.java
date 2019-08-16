/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.globalconfig.api;

import static com.typesafe.config.ConfigFactory.invalidateCaches;
import static com.typesafe.config.ConfigSyntax.JSON;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.StringUtils.EMPTY;
import static org.mule.runtime.globalconfig.internal.ClusterConfigBuilder.defaultClusterConfig;
import static org.mule.runtime.globalconfig.internal.MavenConfigBuilder.defaultMavenConfig;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.globalconfig.api.cluster.ClusterConfig;
import org.mule.runtime.globalconfig.api.exception.RuntimeGlobalConfigException;
import org.mule.runtime.globalconfig.internal.ClusterConfigBuilder;
import org.mule.runtime.globalconfig.internal.MavenConfigBuilder;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigResolveOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import java.util.function.Supplier;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mule Runtime global configuration loader.
 *
 * @since 4.0
 */
public class GlobalConfigLoader {

  private static final String CONFIG_ROOT_ELEMENT_NAME = "muleRuntimeConfig";
  private static final String DEFAULT_MULE_CONFIG_FILE_NAME = "mule-config";
  public static final String MULE_CONFIG_FILE_NAME_PROPERTY = "mule.configFile";
  private static final String CLUSTER_PROPERTY = "cluster";
  private static final String MAVEN_PROPERTY = "maven";
  private static final String JSON_EXTENSION = ".json";
  private static Logger LOGGER = LoggerFactory.getLogger(GlobalConfigLoader.class);
  private static MavenConfiguration mavenConfig;
  private static ClusterConfig clusterConfig;

  private static StampedLock lock = new StampedLock();

  private static final String MULE_SCHEMA_JSON_LOCATION = "mule-schema.json";

  private GlobalConfigLoader() {

  }

  /**
   * Initialise the global config if not yet initialised.
   * <p>
   * Validates the provided configuration against a JSON schema
   */
  private static void initialiseGlobalConfig() {
    String configFileName =
        getProperty(MULE_CONFIG_FILE_NAME_PROPERTY, DEFAULT_MULE_CONFIG_FILE_NAME).replace(JSON_EXTENSION, EMPTY);
    Config config =
        ConfigFactory.load(GlobalConfigLoader.class.getClassLoader(), configFileName,
                           ConfigParseOptions.defaults().setSyntax(JSON), ConfigResolveOptions.defaults());
    Config muleRuntimeConfig = config.hasPath(CONFIG_ROOT_ELEMENT_NAME) ? config.getConfig(CONFIG_ROOT_ELEMENT_NAME) : null;
    if (muleRuntimeConfig == null) {
      mavenConfig = defaultMavenConfig();
      clusterConfig = defaultClusterConfig();
    } else {
      String effectiveConfigAsJson =
          muleRuntimeConfig.root().render(ConfigRenderOptions.concise().setJson(true).setComments(false));
      String prettyPrintConfig = muleRuntimeConfig.root()
          .render(ConfigRenderOptions.defaults().setComments(true).setJson(true).setFormatted(true));
      try (
          InputStream schemaStream = GlobalConfigLoader.class.getClassLoader().getResourceAsStream(MULE_SCHEMA_JSON_LOCATION)) {
        JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));
        Schema schema = SchemaLoader.load(rawSchema);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Using effective mule-config.json configuration: \n"
              + prettyPrintConfig);
        }
        schema.validate(new JSONObject(effectiveConfigAsJson));
        parseMavenConfig(muleRuntimeConfig);
        parseClusterConfig(muleRuntimeConfig);
      } catch (ValidationException e) {
        LOGGER
            .info(format("Mule global config exception. Effective configuration is (config is a merge of MULE_HOME/conf/%s.json and system properties): \n %s",
                         configFileName, prettyPrintConfig));
        throw new RuntimeGlobalConfigException(e);
      } catch (IOException e) {
        throw new RuntimeGlobalConfigException(
                                               createStaticMessage(format("resources %s missing from the runtime classpath",
                                                                          MULE_SCHEMA_JSON_LOCATION)),
                                               e);
      }
    }
  }

  private static <T> T parseConfig(Config muleRuntimeConfig, String configProperty, Supplier<T> noConfigCallback,
                                   Function<Config, T> parseConfigCallback) {
    Config config = muleRuntimeConfig.hasPath(configProperty) ? muleRuntimeConfig.getConfig(configProperty) : null;
    if (config == null) {
      return noConfigCallback.get();
    } else {
      return parseConfigCallback.apply(config);
    }
  }

  private static void parseClusterConfig(Config muleRuntimeConfig) {
    clusterConfig = parseConfig(muleRuntimeConfig, CLUSTER_PROPERTY, ClusterConfigBuilder::defaultClusterConfig,
                                ClusterConfigBuilder::parseClusterConfig);
  }

  private static void parseMavenConfig(Config muleRuntimeConfig) {
    mavenConfig = parseConfig(muleRuntimeConfig, MAVEN_PROPERTY, MavenConfigBuilder::defaultMavenConfig,
                              MavenConfigBuilder::buildMavenConfig);
  }

  /**
   * Resets the maven configuration. If new system properties were added, those will be taken into account after reloading the
   * config.
   */
  public static void reset() {
    long stamp = lock.writeLock();
    try {
      mavenConfig = null;
      clusterConfig = null;
      invalidateCaches();
      initialiseGlobalConfig();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  /**
   * @return the maven configuration to use for the runtime.
   */
  public static MavenConfiguration getMavenConfig() {
    return safetelyGetConfig(() -> mavenConfig);
  }

  /**
   * @return the cluster configuration to use for the runtime.
   */
  public static ClusterConfig getClusterConfig() {
    return safetelyGetConfig(() -> clusterConfig);
  }

  private static <T> T safetelyGetConfig(Supplier<T> configSupplier) {
    long stamp = lock.readLock();
    try {
      if (configSupplier.get() == null) {
        long writeStamp = lock.tryConvertToWriteLock(stamp);
        if (writeStamp == 0L) {
          lock.unlockRead(stamp);
          stamp = lock.writeLock();
        } else {
          stamp = writeStamp;
        }
        if (configSupplier.get() == null) {
          initialiseGlobalConfig();
        }
      }
      return configSupplier.get();
    } finally {
      lock.unlock(stamp);
    }
  }

}
