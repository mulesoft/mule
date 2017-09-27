/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.license.api;

public class PluginLicenseValidationRequest {

  private final String entitlement;
  private final String pluginVersion;
  private final String pluginProvider;
  private final String pluginName;
  private final ClassLoader artifactClassLoader;
  private final ClassLoader pluginClassLoader;

  public PluginLicenseValidationRequest(String entitlement, String pluginVersion, String pluginProvider, String pluginName,
                                        ClassLoader artifactClassLoader, ClassLoader pluginClassLoader) {
    this.entitlement = entitlement;
    this.pluginVersion = pluginVersion;
    this.pluginProvider = pluginProvider;
    this.pluginName = pluginName;
    this.artifactClassLoader = artifactClassLoader;
    this.pluginClassLoader = pluginClassLoader;
  }

  public String getEntitlement() {
    return entitlement;
  }

  public String getPluginVersion() {
    return pluginVersion;
  }

  public String getPluginProvider() {
    return pluginProvider;
  }

  public String getPluginName() {
    return pluginName;
  }

  public ClassLoader getArtifactClassLoader() {
    return artifactClassLoader;
  }

  public ClassLoader getPluginClassLoader() {
    return pluginClassLoader;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String pluginVersion;
    private String pluginName;
    private String pluginProvider;
    private String entitlement;
    private ClassLoader artifactClassLoader;
    private ClassLoader pluginClassLoader;

    private Builder() {}

    public Builder withPluginVersion(String pluginVersion) {
      this.pluginVersion = pluginVersion;
      return this;
    }

    public Builder withPluginName(String pluginName) {
      this.pluginName = pluginName;
      return this;
    }

    public Builder withPluginProvider(String pluginProvider) {
      this.pluginProvider = pluginProvider;
      return this;
    }

    public Builder withEntitlement(String entitlement) {
      this.entitlement = entitlement;
      return this;
    }

    public Builder withArtifactClassLoader(ClassLoader classLoader) {
      this.artifactClassLoader = classLoader;
      return this;
    }

    public Builder withPluginClassLoader(ClassLoader classLoader) {
      this.pluginClassLoader = classLoader;
      return this;
    }

    public PluginLicenseValidationRequest build() {
      return new PluginLicenseValidationRequest(entitlement, pluginVersion, pluginProvider, pluginName, artifactClassLoader,
                                                pluginClassLoader);
    }
  }
}
