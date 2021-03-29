/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ConnectivitySchema {

  public static class ConnectionNode {

    private String classTerm;
    private Map<String, ParameterProperties> mappings = new LinkedHashMap<>();

    public String getClassTerm() {
      return classTerm;
    }

    public Map<String, ParameterProperties> getMappings() {
      return mappings;
    }
  }

  public static class ParameterProperties {

    private String propertyTerm;
    private String range;
    private boolean mandatory;

    public String getPropertyTerm() {
      return propertyTerm;
    }

    public String getRange() {
      return range;
    }

    public boolean isMandatory() {
      return mandatory;
    }
  }

  public static class Builder {

    private final ConnectivitySchema product = new ConnectivitySchema();

    public Builder setGav(String groupId, String artifactId, String version) {
      product.groupId = groupId;
      product.artifactId = artifactId;
      product.version = version;

      return this;
    }

    public Builder addLabel(String key, String value) {
      product.labels.put(key, value);
      return this;
    }

    public Builder addAsset(BundleDescriptor bundleDescriptor) {
      product.assets.add(bundleDescriptor);
      return this;
    }

    public Builder uses(String ns, String url) {
      product.uses.put(ns, url);
      return this;
    }

    public Builder usesExternal(String ns, String url) {
      product.external.put(ns, url);
      return this;
    }

    public Builder addNode(String name, Consumer<ConnectionNodeConfigurer> configurerConsumer) {
      ConnectionNodeConfigurer configurer = new ConnectionNodeConfigurer();
      configurerConsumer.accept(configurer);
      product.nodeMappings.put(name, configurer.product);

      return this;
    }

    public ConnectivitySchema build() {
      return product;
    }
  }

  public static class ConnectionNodeConfigurer {

    private ConnectionNode product = new ConnectionNode();

    public ConnectionNodeConfigurer setClassTerm(String classTerm) {
      product.classTerm = classTerm;
      return this;
    }

    public ConnectionNodeConfigurer addParameter(String name, Consumer<ConnectionParameterConfigurer> configurerConsumer) {
      ConnectionParameterConfigurer configurer = new ConnectionParameterConfigurer();
      configurerConsumer.accept(configurer);
      product.mappings.put(name, configurer.product);

      return this;
    }

  }

  public static class ConnectionParameterConfigurer {
    private ParameterProperties product = new ParameterProperties();

    public ConnectionParameterConfigurer setPropertyTerm(String propertyTerm) {
      product.propertyTerm = propertyTerm;
      return this;
    }

    public ConnectionParameterConfigurer setRange(String range) {
      product.range = range;
      return this;
    }

    public ConnectionParameterConfigurer mandatory(boolean mandatory) {
      product.mandatory = mandatory;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private String groupId;
  private String artifactId;
  private String version;

  private Map<String, String> labels = new LinkedHashMap<>();
  private List<BundleDescriptor> assets = new LinkedList<>();
  private Map<String, String> uses = new LinkedHashMap<>();
  private Map<String, String> external = new LinkedHashMap<>();
  private Map<String, ConnectionNode> nodeMappings = new LinkedHashMap<>();
  private Definition definition = new Definition();

  public ConnectivitySchema() {
    definition.getDocument().getRoot().setEncodes("Connection");
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public List<BundleDescriptor> getAssets() {
    return assets;
  }

  public Map<String, String> getUses() {
    return uses;
  }

  public Map<String, String> getExternal() {
    return external;
  }

  public Map<String, ConnectionNode> getNodeMappings() {
    return nodeMappings;
  }
}
