/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.license.api;

/**
 * Holds all the information required for doing the validation of a plugin license.
 * 
 * @since 4.0
 */
public class PluginLicenseValidationRequest {

  private final String entitlement;
  private final String pluginVersion;
  private final String pluginProvider;
  private final String pluginName;
  private final boolean allowsEvaluation;
  private final ClassLoader artifactClassLoader;
  private final ClassLoader pluginClassLoader;

  /**
   * Creates a new {@link PluginLicenseValidationRequest}
   * 
   * @param entitlement the required entitlement within the license
   * @param pluginVersion the plugin version
   * @param pluginProvider the plugin provider name
   * @param pluginName the plugin name
   * @param allowsEvaluation true if the plugin allows running with an evaluation license, false otherwise
   * @param artifactClassLoader the class loader of the artifact that holds the customer license
   * @param pluginClassLoader the plugin class loader that holds the provider license key
   */
  private PluginLicenseValidationRequest(String entitlement, String pluginVersion, String pluginProvider, String pluginName,
                                         boolean allowsEvaluation,
                                         ClassLoader artifactClassLoader, ClassLoader pluginClassLoader) {
    this.entitlement = entitlement;
    this.pluginVersion = pluginVersion;
    this.pluginProvider = pluginProvider;
    this.pluginName = pluginName;
    this.allowsEvaluation = allowsEvaluation;
    this.artifactClassLoader = artifactClassLoader;
    this.pluginClassLoader = pluginClassLoader;
  }

  /**
   * @return the expected entitlement in the license
   */
  public String getEntitlement() {
    return entitlement;
  }

  /**
   * @return the plugin version to be executed
   */
  public String getPluginVersion() {
    return pluginVersion;
  }

  /**
   * @return the plugin provider name
   */
  public String getPluginProvider() {
    return pluginProvider;
  }

  /**
   * @return the plugin name
   */
  public String getPluginName() {
    return pluginName;
  }

  /**
   * @return true if the plugin allows running with an evaluation license, false otherwise
   */
  public boolean isAllowsEvaluation() {
    return allowsEvaluation;
  }

  /**
   * @return the classloader of the artifact in which the customer license is expected
   */
  public ClassLoader getArtifactClassLoader() {
    return artifactClassLoader;
  }

  /**
   * @return the classloader of the provider plugin
   */
  public ClassLoader getPluginClassLoader() {
    return pluginClassLoader;
  }

  /**
   * @return a new builder for instance of {@link PluginLicenseValidationRequest}
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String pluginVersion;
    private String pluginName;
    private String pluginProvider;
    private String entitlement;
    private boolean allowsEvaluation;
    private ClassLoader artifactClassLoader;
    private ClassLoader pluginClassLoader;

    private Builder() {}

    /**
     * @param pluginVersion the plugin version
     * @return the builder
     */
    public Builder withPluginVersion(String pluginVersion) {
      this.pluginVersion = pluginVersion;
      return this;
    }

    /**
     * @param pluginName the plugin name
     * @return the builder
     */
    public Builder withPluginName(String pluginName) {
      this.pluginName = pluginName;
      return this;
    }

    /**
     * @param pluginProvider the plugin provider
     * @return the builder
     */
    public Builder withPluginProvider(String pluginProvider) {
      this.pluginProvider = pluginProvider;
      return this;
    }

    /**
     * @param entitlement the entitlement to validate within the license
     * @return the builder
     */
    public Builder withEntitlement(String entitlement) {
      this.entitlement = entitlement;
      return this;
    }

    /**
     * @param allowsEvaluation the entitlement to validate within the license
     * @return the builder
     */
    public Builder withAllowsEvaluation(boolean allowsEvaluation) {
      this.allowsEvaluation = allowsEvaluation;
      return this;
    }

    /**
     * @param classLoader the classloader of the artifact in which the customer license is expected
     * @return the builder
     */
    public Builder withArtifactClassLoader(ClassLoader classLoader) {
      this.artifactClassLoader = classLoader;
      return this;
    }

    /**
     * @param classLoader the classloader of the provider plugin
     * @return the builder
     */
    public Builder withPluginClassLoader(ClassLoader classLoader) {
      this.pluginClassLoader = classLoader;
      return this;
    }

    /**
     * @return a new {@link PluginLicenseValidationRequest} with the provided information.
     */
    public PluginLicenseValidationRequest build() {
      return new PluginLicenseValidationRequest(entitlement, pluginVersion, pluginProvider, pluginName,
                                                allowsEvaluation, artifactClassLoader, pluginClassLoader);
    }
  }
}
