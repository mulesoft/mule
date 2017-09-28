/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.api.component.AbstractComponent;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

/**
 * Artifact attributes configuration. This class represents a single configuration-attributes element from the configuration.
 *
 * @since 4.0
 */
public class ConfigurationPropertiesComponent extends AbstractComponent
    implements ConfigurationPropertiesProvider, Initialisable {

  private static final String PROPERTIES_EXTENSION = ".properties";
  private static final String YAML_EXTENSION = ".yaml";
  private static final String UNKNOWN = "unknown";

  private final Map<String, ConfigurationProperty> configurationAttributes = new HashMap<>();
  private String fileLocation;
  private ResourceProvider resourceProvider;

  public ConfigurationPropertiesComponent(String fileLocation, ResourceProvider resourceProvider)
      throws ConfigurationPropertiesException {
    this.fileLocation = fileLocation;
    this.resourceProvider = resourceProvider;
  }

  @Override
  public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
    return Optional.ofNullable(configurationAttributes.get(configurationAttributeKey));
  }

  @Override
  public String getDescription() {
    ComponentLocation location = (ComponentLocation) getAnnotation(LOCATION_KEY);
    return format("<configuration-attributes file=\"%s\"> - file: %s, line number: %s", fileLocation,
                  location.getFileName().orElse(UNKNOWN),
                  location.getLineInFile().map(String::valueOf).orElse("unknown"));

  }

  @Override
  public void initialise() throws InitialisationException {
    if (!fileLocation.endsWith(PROPERTIES_EXTENSION) && !fileLocation.endsWith(YAML_EXTENSION)) {
      throw new ConfigurationPropertiesException(createStaticMessage(format("Configuration properties file %s must end with yaml or properties extension",
                                                                            fileLocation)),
                                                 this);
    }

    try (InputStream is = resourceProvider.getResourceAsStream(fileLocation)) {
      if (is == null) {
        throw new ConfigurationPropertiesException(createStaticMessage(format("Couldn't find configuration properties file %s neither on classpath or in file system",
                                                                              fileLocation)),
                                                   this);
      }

      if (fileLocation.endsWith(PROPERTIES_EXTENSION)) {
        Properties properties = new Properties();
        properties.load(is);
        properties.keySet().stream().map(key -> {
          Object rawValue = properties.get(key);
          return new ConfigurationProperty(of(this), (String) key, (String) rawValue);
        }).forEach(configurationAttribute -> {
          configurationAttributes.put(configurationAttribute.getKey(), configurationAttribute);
        });
      } else {
        Yaml yaml = new Yaml();
        Iterable<Object> yamlObjects = yaml.loadAll(is);
        yamlObjects.forEach(yamlObject -> {
          createAttributesFromYamlObject(null, null, yamlObject);
        });
      }
    } catch (ConfigurationPropertiesException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigurationPropertiesException(createStaticMessage("Couldn't read from file "
          + fileLocation), this, e);
    }
  }

  private void createAttributesFromYamlObject(String parentPath, Object parentYamlObject, Object yamlObject) {
    if (yamlObject instanceof List) {
      List list = (List) yamlObject;
      if (list.get(0) instanceof Map) {
        list.forEach(value -> createAttributesFromYamlObject(parentPath, yamlObject, value));
      } else {
        if (!(list.get(0) instanceof String)) {
          throw new ConfigurationPropertiesException(createStaticMessage("List of complex objects are not supported as property values. Offending key is "
              + parentPath),
                                                     this);
        }
        String[] values = new String[list.size()];
        list.toArray(values);
        String value = join(",", list);
        configurationAttributes.put(parentPath, new ConfigurationProperty(this, parentPath, value));
      }
    } else if (yamlObject instanceof Map) {
      if (parentYamlObject instanceof List) {
        throw new ConfigurationPropertiesException(createStaticMessage("Configuration properties does not support type a list of complex types. Complex type keys are: "
            + join(",", ((Map) yamlObject).keySet())),
                                                   this);
      }
      Map<String, Object> map = (Map) yamlObject;
      map.entrySet().stream()
          .forEach(entry -> createAttributesFromYamlObject(createKey(parentPath, entry.getKey()), yamlObject, entry.getValue()));
    } else {
      if (!(yamlObject instanceof String)) {
        throw new ConfigurationPropertiesException(createStaticMessage(format("YAML configuration properties only supports string values, make sure to wrap the value with \" so you force the value to be an string. Offending property is %s with value %s",
                                                                              parentPath, yamlObject)),
                                                   this);
      }
      configurationAttributes.put(parentPath, new ConfigurationProperty(this, parentPath, yamlObject));
    }
  }

  private String createKey(String parentKey, String key) {
    if (parentKey == null) {
      return key;
    }
    return parentKey + "." + key;
  }
}
