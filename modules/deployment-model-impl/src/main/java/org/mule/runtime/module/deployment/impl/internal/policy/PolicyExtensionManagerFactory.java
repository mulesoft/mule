/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_POLICY_ISOLATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.CompositeArtifactExtensionManagerFactory;
import org.mule.runtime.module.extension.api.manager.DefaultExtensionManagerFactory;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ExtensionManagerFactory} that defers the decision of the {@link ExtensionManager} implementation until the MuleContext
 * is available, in order to apply the {@link MuleRuntimeFeature#ENABLE_POLICY_ISOLATION} feature flag.
 */
class PolicyExtensionManagerFactory implements ExtensionManagerFactory {

  private final Application application;
  private final PolicyTemplate template;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;

  public PolicyExtensionManagerFactory(Application application, PolicyTemplate template,
                                       ExtensionModelLoaderRepository extensionModelLoaderRepository) {
    this.application = application;
    this.template = template;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
  }

  @Override
  public ExtensionManager create(MuleContext muleContext) {
    try {
      FeatureFlaggingService featureFlaggingService =
          ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(FeatureFlaggingService.class);
      if (featureFlaggingService.isEnabled(ENABLE_POLICY_ISOLATION)) {
        // The policy will not share extension models and configuration providers with the application that is being applied to.
        ArtifactExtensionManagerFactory artifactExtensionManagerFactory =
            new ArtifactExtensionManagerFactory(template.getOwnArtifactPlugins(), extensionModelLoaderRepository,
                                                new DefaultExtensionManagerFactory());
        return artifactExtensionManagerFactory.create(muleContext, getInheritedExtensionModels());
      } else {
        // The policy will share extension models and configuration providers with the application that is being applied to...
        return new CompositeArtifactExtensionManagerFactory(application, extensionModelLoaderRepository,
                                                            // even if http/sockets are declared as dependency of the policy, if
                                                            // they are included in the app they must be used from there.
                                                            nonInheritedOwnArtifactPlugins(),
                                                            new DefaultExtensionManagerFactory()).create(muleContext);
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private List<ArtifactPlugin> nonInheritedOwnArtifactPlugins() {
    Set<String> inheritedExtensionModelNames = getInheritedExtensionModels().stream()
        .map(em -> em.getName())
        .collect(toSet());

    List<ArtifactPlugin> filteredOwnArtifactPlugins = template.getOwnArtifactPlugins()
        .stream()
        .filter(ownAP -> !inheritedExtensionModelNames
            .contains(ownAP.getDescriptor().getName()))
        .collect(toList());
    return filteredOwnArtifactPlugins;
  }

  /**
   * HTTP and Sockets extension models must be added if found in the parent artifact extensions (backward compatibility for API
   * Gateway).
   * 
   * @return Set containing the parent artifact HTTP and Sockets extension models (if present).
   */
  private Set<ExtensionModel> getInheritedExtensionModels() {
    Set<ExtensionModel> inheritedExtensionModels = new HashSet<>(2);
    ExtensionManager extensionManager = application.getRegistry().<ExtensionManager>lookupByName(OBJECT_EXTENSION_MANAGER).get();
    extensionManager.getExtension("HTTP")
        .ifPresent(inheritedExtensionModels::add);
    extensionManager.getExtension("Sockets")
        .ifPresent(inheritedExtensionModels::add);
    return inheritedExtensionModels;
  }

}
