/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.loader;

import static java.lang.String.format;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.deployment.model.api.plugin.descriptor.Descriptor;
import org.mule.runtime.deployment.model.api.plugin.loader.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.PluginDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.descriptor.DefaultDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.descriptor.DefaultPluginDescriptor;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Abstract class to enable the template method when executing {@link #load(File)}  method
 *
 * @since 4.0
 */
public abstract class AbstractPluginDescriptorLoader {

  private final static File META_INF_FOLDER = new File("META-INF");
  public static final String MULE_PLUGIN_JSON_FILENAME = "mule-plugin.json";
  public final static File PLUGIN_JSON_DESCRIPTOR_FILE = new File(META_INF_FOLDER, MULE_PLUGIN_JSON_FILENAME);

  protected abstract Optional<InputStream> get(File pluginLocation, String resource) throws MalformedPluginException;

  public PluginDescriptor load(File pluginLocation) throws MalformedPluginException {
    Optional<InputStream> jsonDescriptorInputStream = get(pluginLocation, PLUGIN_JSON_DESCRIPTOR_FILE.getPath());
    if (!jsonDescriptorInputStream.isPresent()) {
      throw new MalformedPluginException(format("There is no %s classloadermodel available for the current plugin %s",
                                                PLUGIN_JSON_DESCRIPTOR_FILE.getPath(), pluginLocation.getAbsolutePath()));
    }
    return parseJsonDescriptor(jsonDescriptorInputStream.get());
  }

  private PluginDescriptor parseJsonDescriptor(InputStream descriptorInputStream)
      throws MalformedPluginException {
    try {
      JsonDescriptor jsonDescriptor =
          new Gson().fromJson(new BufferedReader(new InputStreamReader(descriptorInputStream)),
                              JsonDescriptor.class);
      return new DefaultPluginDescriptor(parseName(jsonDescriptor.getName()),
                                         parseMinMuleVersion(jsonDescriptor.getMinMuleVersion()),
                                         parseClassloaderDescriptor(jsonDescriptor.getClassloaderDescriptor()),
                                         parseExtensionModelDescriptor(jsonDescriptor.getExtensionModelDescriptor()));
    } catch (JsonIOException | JsonSyntaxException e) {
      throw new MalformedPluginException("There was an issue parsing the JSON file", e);
    }
  }

  private String parseName(String name) throws MalformedPluginException {
    if (StringUtils.isBlank(name)) {
      throw new MalformedPluginException("The JSON file is missing the 'name' member");
    }
    return name;
  }

  private MuleVersion parseMinMuleVersion(String minMuleVersion) throws MalformedPluginException {
    if (StringUtils.isBlank(minMuleVersion)) {
      throw new MalformedPluginException("The JSON file is missing the 'minMuleVersion' member");
    }
    try {
      return new MuleVersion(minMuleVersion);
    } catch (IllegalArgumentException e) {
      throw new MalformedPluginException("The member 'minMuleVersion' is not well formed", e);
    }
  }

  private Descriptor parseClassloaderDescriptor(JsonDescriptor.ClassloaderDescriptor classloaderDescriptor)
      throws MalformedPluginException {
    if (classloaderDescriptor != null || StringUtils.isNotBlank(classloaderDescriptor.getId())) {
      return new DefaultDescriptor(classloaderDescriptor.getId(), classloaderDescriptor.getAttributes());
    } else {
      throw new MalformedPluginException("The member 'classloaderDescriptor' is not well formed or missing");
    }
  }

  private Optional<Descriptor> parseExtensionModelDescriptor(JsonDescriptor.ExtensionModelDescriptor extensionModelDescriptor)
      throws MalformedPluginException {
    Optional<Descriptor> descriptor = Optional.empty();
    if (extensionModelDescriptor != null) {
      descriptor = Optional.of(new DefaultDescriptor(extensionModelDescriptor.getId(), extensionModelDescriptor.getAttributes()));
    }
    return descriptor;
  }

  /**
   * POJO representation of a mule-plugin.json file to load the parameters from it
   *
   * @since 1.0
   */
  private class JsonDescriptor {

    //TODO MULE-10875 this class should be provided in mule-module-artifact, as well as a builder for it so it can be serialized in one place
    private String name;
    private String minMuleVersion;
    private ClassloaderDescriptor classloaderDescriptor;
    private ExtensionModelDescriptor extensionModelDescriptor;

    public String getName() {
      return name;
    }

    public String getMinMuleVersion() {
      return minMuleVersion;
    }

    public ClassloaderDescriptor getClassloaderDescriptor() {
      return classloaderDescriptor;
    }

    public ExtensionModelDescriptor getExtensionModelDescriptor() {
      return extensionModelDescriptor;
    }

    private class ClassloaderDescriptor {

      private String id;
      private Map<String, Object> attributes;

      public String getId() {
        return id;
      }

      public Map<String, Object> getAttributes() {
        return attributes == null ? new HashMap<>() : attributes;
      }
    }

    private class ExtensionModelDescriptor {

      private String id;
      private Map<String, Object> attributes;

      public String getId() {
        return id;
      }

      public Map<String, Object> getAttributes() {
        return attributes == null ? new HashMap<>() : attributes;
      }
    }
  }
}
