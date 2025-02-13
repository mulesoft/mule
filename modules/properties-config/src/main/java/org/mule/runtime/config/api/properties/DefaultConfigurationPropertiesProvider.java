/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.properties;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.compile;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.internal.model.dsl.properties.YamlFactory;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.ResourceProvider;
import org.mule.runtime.properties.internal.ConfigurationPropertiesException;
import org.mule.runtime.properties.internal.DefaultConfigurationProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import org.vibur.objectpool.ConcurrentPool;
import org.vibur.objectpool.PoolService;
import org.vibur.objectpool.util.ConcurrentLinkedQueueCollection;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

/**
 * Artifact attributes configuration. This class represents a single configuration-attributes element from the configuration.
 *
 * @since 1.0
 */
public class DefaultConfigurationPropertiesProvider extends AbstractComponent
    implements ConfigurationPropertiesProvider, Initialisable {

  private static final Pattern SPACE_AFTER_COLON_PATTERN = compile("[^:]*: ");
  private static final String PROPERTIES_EXTENSION = ".properties";
  private static final String YAML_EXTENSION = ".yaml";
  protected static final String UNKNOWN = "unknown";

  private static final PoolService<Yaml> YAML_POOL =
      new ConcurrentPool<>(new ConcurrentLinkedQueueCollection<>(), new YamlFactory(), 1,
                           max(1, getRuntime().availableProcessors() / 2), false);

  protected final Map<String, ConfigurationProperty> configurationAttributes = new HashMap<>();
  protected final String fileLocation;
  private final String encoding;
  private final ResourceProvider resourceProvider;

  public DefaultConfigurationPropertiesProvider(String fileLocation, String encoding, ResourceProvider resourceProvider) {
    this.fileLocation = fileLocation;
    this.resourceProvider = resourceProvider;
    this.encoding = encoding;
  }

  public DefaultConfigurationPropertiesProvider(String fileLocation, ResourceProvider resourceProvider) {
    this(fileLocation, null, resourceProvider);
  }

  @Override
  public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
    return ofNullable(configurationAttributes.get(configurationAttributeKey));
  }

  @Override
  public String getDescription() {
    ComponentLocation location = (ComponentLocation) getAnnotation(LOCATION_KEY);
    return format("<%s file=\"%s\"> - file: %s, line number: %s",
                  getIdentifier().toString(),
                  fileLocation,
                  location.getFileName().orElse(UNKNOWN),
                  location.getLine().orElse(-1));
  }

  protected InputStream getResourceInputStream(String file) throws IOException {
    return resourceProvider.getResourceAsStream(file);
  }

  private InputStreamReader getResourceInputStreamReader(String file) throws IOException {
    InputStream in = getResourceInputStream(file);
    return encoding != null ? new InputStreamReader(in, encoding) : new InputStreamReader(in);
  }

  @Override
  public void initialise() throws InitialisationException {
    if (!fileLocation.endsWith(PROPERTIES_EXTENSION) && !fileLocation.endsWith(YAML_EXTENSION)) {
      throw new ConfigurationPropertiesException(createStaticMessage(format("Configuration properties file %s must end with yaml or properties extension",
                                                                            fileLocation)),
                                                 this);
    }

    try (Reader is = new BufferedReader(getResourceInputStreamReader(fileLocation))) {
      readAttributesFromFile(is);
    } catch (ConfigurationPropertiesException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigurationPropertiesException(createStaticMessage("Couldn't read from file "
          + fileLocation + ": " + e.getMessage()), this, e);
    }
  }

  private void readAttributesFromFile(Reader is) throws IOException {
    if (fileLocation.endsWith(PROPERTIES_EXTENSION)) {
      Properties properties = new Properties();
      properties.load(is);
      properties.keySet().stream()
          .map(key -> new DefaultConfigurationProperty(of(this), (String) key,
                                                       createValue((String) key, (String) properties.get(key))))
          .forEach(configurationAttribute -> configurationAttributes.put(configurationAttribute.getKey(),
                                                                         configurationAttribute));
    } else {
      final Yaml yaml = YAML_POOL.take();
      try {
        Iterable<Object> yamlObjects = yaml.loadAll(is);
        try {
          yamlObjects.forEach(yamlObject -> createAttributesFromYamlObject(null, null, yamlObject));
        } catch (ParserException e) {
          throw new ConfigurationPropertiesException(createStaticMessage("Error while parsing YAML configuration file. Check that all quotes are correctly closed."),
                                                     this, e);
        }
      } finally {
        YAML_POOL.restore(yaml);
      }
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
        configurationAttributes.put(parentPath, new DefaultConfigurationProperty(this, parentPath, value));
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
      if (parentPath == null) {
        if (!SPACE_AFTER_COLON_PATTERN.matcher((String) yamlObject).matches()) {
          throw new ConfigurationPropertiesException(createStaticMessage(format("YAML configuration properties must have space after ':' character. Offending line is: %s",
                                                                                yamlObject)),
                                                     this);
        } else {
          throw new ConfigurationPropertiesException(createStaticMessage(format("YAML configuration property key must not be null. Offending line is %s",
                                                                                yamlObject)),
                                                     this);
        }
      }
      String resultObject = createValue(parentPath, (String) yamlObject);
      configurationAttributes.put(parentPath, new DefaultConfigurationProperty(this, parentPath, resultObject));
    }
  }

  private String createKey(String parentKey, String key) {
    if (parentKey == null) {
      return key;
    }
    return parentKey + "." + key;
  }

  protected String createValue(String key, String value) {
    return value;
  }

  @Override
  public String toString() {
    return getDescription();
  }
}
