/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import org.mule.runtime.api.connection.ConnectionProvider;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Models the AML dialect used by the CFM service to represent a connection schema
 *
 * @since 4.4.0
 */
public class ConnectivitySchema {

  /**
   * Builder object for producing {@link ConnectivitySchema} instances
   *
   * @since 4.4.0
   */
  public static class Builder {

    private final ConnectivitySchema product = new ConnectivitySchema();
    private final ConnectivitySchemaNode connectionNode = new ConnectivitySchemaNode();

    public Builder() {
      product.definition.getNodeMappings().put("Connection", connectionNode);
      uses("connectivity", "anypoint://semantics/connectivity")
          .usesExternal("core", "anypoint://semantics/core")
          .usesExternal("apiContract", "anypoint://semantics/api-contract");
    }

    /**
     * Sets the schema's GAV
     *
     * @param groupId    a valid Maven {@code groupId}
     * @param artifactId a valid Maven {@code artifactId}
     * @param version    a valid Maven {@code version}
     * @return {@code this} builder
     */
    public Builder gav(String groupId, String artifactId, String version) {
      product.groupId = groupId;
      product.artifactId = artifactId;
      product.version = version;

      return this;
    }

    /**
     * Specifies an identifier for the authentication type modeled by this schema.
     * <p>
     * This identifier should ideally be defined in the connectivity AML vocabulary.
     *
     * @param authenticationType the authentication type id
     * @return {@code this} builder
     */
    public Builder authenticationType(String authenticationType) {
      product.labels.put("type", authenticationType);
      return this;
    }

    /**
     * Specifies the name of the system targeted by this connection
     *
     * @param system the system name
     * @return {@code this} builder
     */
    public Builder system(String system) {
      product.labels.put("system", system);
      return this;
    }

    /**
     * Specifies the name of a Mule connector's {@link ConnectionProvider} which implements this schema
     *
     * @param connectionProviderName a ConnectionProvider name
     * @return {@code this} builder
     */
    public Builder connectionProviderName(String connectionProviderName) {
      product.labels.put("connectionProvider", connectionProviderName);
      return this;
    }

    /**
     * Adds a label
     *
     * @param key   the label key
     * @param value the label value
     * @return {@code this} builder
     */
    public Builder withLabel(String key, String value) {
      product.labels.put(key, value);
      return this;
    }

    /**
     * Adds a dependant assert declaration
     *
     * @param assetDescriptor the asset descriptor
     * @return {@code this} builder
     */
    public Builder addAsset(ExchangeAssetDescriptor assetDescriptor) {
      product.assets.add(assetDescriptor);
      return this;
    }

    /**
     * Specifies a dependant vocabulary referenced in the produced schema
     *
     * @param ns  the vocabulary namespace
     * @param url the vocabulary url
     * @return {@code this} builder
     */
    public Builder uses(String ns, String url) {
      product.definition.getUses().put(ns, url);
      return this;
    }

    /**
     * Specifies an external dependant vocabulary referenced in the produced schema
     *
     * @param ns  the vocabulary namespace
     * @param url the vocabulary url
     * @return {@code this} builder
     */
    public Builder usesExternal(String ns, String url) {
      product.definition.getExternal().put(ns, url);
      return this;
    }

    /**
     * Specifies the connection class term as defined in the Connectivity AML Vocabulary
     *
     * @param classTerm the connectivity class term
     * @return {@code this} builder
     */
    public Builder connectionClassTerm(String classTerm) {
      connectionNode.setClassTerm(classTerm);
      return this;
    }

    /**
     * Adds a parameter into the range through the use of a {@link ConnectionParameterConfigurer}
     *
     * @param name               the parameter name
     * @param configurerConsumer a {@link Consumer} for a {@link ConnectionParameterConfigurer} on which the parameter is
     *                           described
     * @return {@code this} builder
     */
    public Builder withParameter(String name, Consumer<ConnectionParameterConfigurer> configurerConsumer) {
      ConnectionParameterConfigurer configurer = new ConnectionParameterConfigurer();
      configurerConsumer.accept(configurer);
      connectionNode.getMappings().put(name, configurer.product);

      return this;
    }

    /**
     * Specifies a schema specific custom range through the use of a {@link CustomRangeConfigurer}
     *
     * @param name               the range name
     * @param configurerConsumer a {@link Consumer} for a {@link CustomRangeConfigurer} on which the range is described
     * @return {@code this} builder
     */
    public Builder withCustomRange(String name, Consumer<CustomRangeConfigurer> configurerConsumer) {
      CustomRangeConfigurer configurer = new CustomRangeConfigurer();
      configurerConsumer.accept(configurer);
      product.definition.getNodeMappings().put(name, configurer.product);

      return this;
    }

    /**
     * @return The produced in
     */
    public ConnectivitySchema build() {
      return product;
    }
  }

  /**
   * A configurer for describing a custom Range
   *
   * @since 4.4.0
   */
  public static class CustomRangeConfigurer {

    private ConnectivitySchemaNode product = new ConnectivitySchemaNode();

    /**
     * Specifies the custom range class term. Ideally, this term is defined in the Connectivity AML Vocabulary.
     *
     * @param classTerm the class term.
     * @return {@code this} configurer
     */
    public CustomRangeConfigurer setClassTerm(String classTerm) {
      product.setClassTerm(classTerm);
      return this;
    }

    /**
     * Adds a parameter into the range through the use of a {@link ConnectionParameterConfigurer}
     *
     * @param name               the parameter name
     * @param configurerConsumer a {@link Consumer} for a {@link ConnectionParameterConfigurer} on which the parameter is
     *                           described
     * @return {@code this} configurer
     */
    public CustomRangeConfigurer addParameter(String name, Consumer<ConnectionParameterConfigurer> configurerConsumer) {
      ConnectionParameterConfigurer configurer = new ConnectionParameterConfigurer();
      configurerConsumer.accept(configurer);
      product.getMappings().put(name, configurer.product);

      return this;
    }
  }

  /**
   * A configurer for describing a parameter
   *
   * @since 4.4.0
   */
  public static class ConnectionParameterConfigurer {

    private ConnectivitySchemaParameter product = new ConnectivitySchemaParameter();

    /**
     * Specifies the parameter's property term. Ideally, this term is defined in the Connectivity AML Vocabulary.
     *
     * @param propertyTerm the property term.
     * @return {@code this} configurer
     */
    public ConnectionParameterConfigurer setPropertyTerm(String propertyTerm) {
      product.setPropertyTerm(propertyTerm);
      return this;
    }

    /**
     * Specifies the parameter's range
     *
     * @param range a range
     * @return {@code this} configurer
     */
    public ConnectionParameterConfigurer setRange(String range) {
      product.setRange(range);
      return this;
    }

    /**
     * Specifies whether the parameter is mandatory or not
     *
     * @param mandatory the mandatory boolean value
     * @return {@code this} configurer
     */
    public ConnectionParameterConfigurer mandatory(boolean mandatory) {
      product.setMandatory(mandatory);
      return this;
    }
  }

  /**
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  private String groupId;
  private String artifactId;
  private String version;

  private Map<String, String> labels = new LinkedHashMap<>();
  private List<ExchangeAssetDescriptor> assets = new LinkedList<>();
  private ConnectivitySchemaDefinition definition = new ConnectivitySchemaDefinition();

  public ConnectivitySchema() {
    definition.getDocument().getRoot().setEncodes("Connection");
  }

  /**
   * @return the schema's groupId
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * @return the schema's artifactId
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * @return the schema's version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @return The schema's labels
   */
  public Map<String, String> getLabels() {
    return labels;
  }

  /**
   * @return The schema's assets
   */
  public List<ExchangeAssetDescriptor> getAssets() {
    return assets;
  }

  /**
   * @return The schema's definition
   */
  public ConnectivitySchemaDefinition getDefinition() {
    return definition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass())
      return false;
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
