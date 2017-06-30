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
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.globalconfig.internal.MavenConfigBuilder.buildMavenConfig;
import static org.mule.runtime.globalconfig.internal.MavenConfigBuilder.buildNullMavenConfig;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.globalconfig.api.exception.RuntimeGlobalConfigException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigResolveOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.StampedLock;

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
  private static Logger LOGGER = LoggerFactory.getLogger(GlobalConfigLoader.class);
  private static MavenConfiguration mavenConfig;

  private static StampedLock lock = new StampedLock();

  private static final String MULE_SCHEMA_JSON_LOCATION = "mule-schema.json";

  /**
   * Initialise the global config if not yet initialised.
   * <p>
   * Validates the provided configuration against a JSON schema
   */
  private static void initialiseGlobalConfig() {
    Config config =
        ConfigFactory.load(GlobalConfigLoader.class.getClassLoader(), "mule-config",
                           ConfigParseOptions.defaults().setSyntax(JSON), ConfigResolveOptions.defaults());
    Config muleRuntimeConfig = config.hasPath(CONFIG_ROOT_ELEMENT_NAME) ? config.getConfig(CONFIG_ROOT_ELEMENT_NAME) : null;
    if (muleRuntimeConfig == null) {
      mavenConfig = buildNullMavenConfig();
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
        Config mavenConfig = muleRuntimeConfig.getConfig("maven");
        if (mavenConfig != null) {
          GlobalConfigLoader.mavenConfig = buildMavenConfig(mavenConfig);
        } else {
          GlobalConfigLoader.mavenConfig = buildNullMavenConfig();
        }
      } catch (ValidationException e) {
        LOGGER
            .info("Mule global config exception. Effective configuration is (config is a merge of MULE_HOME/conf/mule-config.json and system properties): \n "
                + prettyPrintConfig);
        throw new RuntimeGlobalConfigException(e);
      } catch (IOException e) {
        throw new RuntimeGlobalConfigException(
                                               createStaticMessage(format("resources %s missing from the runtime classpath",
                                                                          MULE_SCHEMA_JSON_LOCATION)),
                                               e);
      }
    }
  }

  /**
   * Resets the maven configuration. If new system properties were added, those will be taken into account after reloading the
   * config.
   */
  public static void reset() {
    long stamp = lock.writeLock();
    try {
      mavenConfig = null;
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
    long stamp = lock.readLock();
    try {
      if (mavenConfig == null) {
        long writeStamp = lock.tryConvertToWriteLock(stamp);
        if (writeStamp == 0L) {
          lock.unlockRead(stamp);
          stamp = lock.writeLock();
        } else {
          stamp = writeStamp;
        }
        if (mavenConfig == null) {
          initialiseGlobalConfig();
        }
      }
      return mavenConfig;
    } finally {
      lock.unlock(stamp);
    }
  }

}
