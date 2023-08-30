/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.extension.discovery;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Internal interface that extends the {@link ExtensionDiscoveryRequest}.
 * 
 * @since 4.6.0
 */
public interface InternalExtensionDiscoveryRequest extends ExtensionDiscoveryRequest {

  /**
   * @return True if extension enrichment that is useful during design time should be performed. Design time enrichment allows
   *         tools such as IDEs to get extension data that can improve the design experience.
   */
  boolean isPerformDesignTimeEnrichment();

  void setIsPerformDesignTimeEnrichment(boolean isPerformDesignTimeEnrichment);

  static InternalExtensionDiscoveryRequest getAsInternalExtensionDiscoveryRequest(ExtensionDiscoveryRequest extensionDiscoveryRequest) {
    if (extensionDiscoveryRequest == null) {
      return null;
    }

    if (extensionDiscoveryRequest instanceof InternalExtensionDiscoveryRequest) {
      return (InternalExtensionDiscoveryRequest) extensionDiscoveryRequest;
    }

    return new InternalExtensionDiscoveryRequestWrapper(extensionDiscoveryRequest);
  }

  class InternalExtensionDiscoveryRequestWrapper implements InternalExtensionDiscoveryRequest {

    private final ExtensionDiscoveryRequest extensionDiscoveryRequest;

    public InternalExtensionDiscoveryRequestWrapper(ExtensionDiscoveryRequest extensionDiscoveryRequest) {
      this.extensionDiscoveryRequest = extensionDiscoveryRequest;
    }


    @Override
    public <T> Optional<T> getParameter(String key) {
      return extensionDiscoveryRequest.getParameter(key);
    }

    @Override
    public Map<String, Object> getParameters() {
      return extensionDiscoveryRequest.getParameters();
    }

    @Override
    public Collection<ArtifactPluginDescriptor> getArtifactPluginDescriptors() {
      return extensionDiscoveryRequest.getArtifactPluginDescriptors();
    }

    @Override
    public Set<ExtensionModel> getParentArtifactExtensions() {
      return extensionDiscoveryRequest.getParentArtifactExtensions();
    }

    @Override
    public boolean isParallelDiscovery() {
      return extensionDiscoveryRequest.isParallelDiscovery();
    }

    @Override
    public boolean isEnrichDescriptions() {
      return extensionDiscoveryRequest.isEnrichDescriptions();
    }

    @Override
    public boolean isOCSEnabled() {
      return extensionDiscoveryRequest.isOCSEnabled();
    }

    @Override
    public boolean isPerformDesignTimeEnrichment() {
      return true;
    }

    @Override
    public void setIsPerformDesignTimeEnrichment(boolean isPerformDesignTimeEnrichment) {
      // Nothing to do.
    }
  }
}
