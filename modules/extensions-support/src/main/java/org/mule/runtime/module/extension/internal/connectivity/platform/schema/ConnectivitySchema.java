/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ConnectivitySchema {

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

    public Builder addAsset(AssetDescriptor assetDescriptor) {
      product.assets.add(assetDescriptor);
      return this;
    }

    public Builder uses(String ns, String url) {
      product.definition.getUses().put(ns, url);
      return this;
    }

    public Builder usesExternal(String ns, String url) {
      product.definition.getExternal().put(ns, url);
      return this;
    }

    public Builder addNode(String name, Consumer<ConnectionNodeConfigurer> configurerConsumer) {
      ConnectionNodeConfigurer configurer = new ConnectionNodeConfigurer();
      configurerConsumer.accept(configurer);
      product.definition.getNodeMappings().put(name, configurer.product);

      return this;
    }

    public ConnectivitySchema build() {
      return product;
    }
  }

  public static class ConnectionNodeConfigurer {

    private ConnectionNode product = new ConnectionNode();

    public ConnectionNodeConfigurer setClassTerm(String classTerm) {
      product.setClassTerm(classTerm);
      return this;
    }

    public ConnectionNodeConfigurer addParameter(String name, Consumer<ConnectionParameterConfigurer> configurerConsumer) {
      ConnectionParameterConfigurer configurer = new ConnectionParameterConfigurer();
      configurerConsumer.accept(configurer);
      product.getMappings().put(name, configurer.product);

      return this;
    }
  }

  public static class ConnectionParameterConfigurer {
    private ConnectivitySchemaParameter product = new ConnectivitySchemaParameter();

    public ConnectionParameterConfigurer setPropertyTerm(String propertyTerm) {
      product.setPropertyTerm(propertyTerm);
      return this;
    }

    public ConnectionParameterConfigurer setRange(String range) {
      product.setRange(range);
      return this;
    }

    public ConnectionParameterConfigurer mandatory(boolean mandatory) {
      product.setMandatory(mandatory);
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
  private List<AssetDescriptor> assets = new LinkedList<>();
  private DocumentDefinition definition = new DocumentDefinition();

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

  public List<AssetDescriptor> getAssets() {
    return assets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) return false;
    ConnectivitySchema that = (ConnectivitySchema) o;
    return Objects.equals(groupId, that.groupId)
            && Objects.equals(artifactId, that.artifactId)
            && Objects.equals(version, that.version)
            && Objects.equals(labels, that.labels)
            && Objects.equals(assets, that.assets)
            && Objects.equals(definition, that.definition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, labels, assets, definition);
  }
}
