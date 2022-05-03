package org.mule.runtime.module.artifact.activation.api.classloader;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.Map;

/**
 * Provides methods to create the descriptors of different kind of artifacts (applications, plugins, domains) from the
 * {@link DeployableProjectModel}.
 * 
 * @since 4.5
 */
public interface ArtifactDescriptorFactory {

  public ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                           Map<String, String> deploymentProperties,
                                                           PluginModelResolver pluginModelResolver,
                                                           PluginDescriptorResolver pluginDescriptorResolver);

  public DomainDescriptor createDomainDescriptor(DeployableProjectModel model,
                                                 Map<String, String> deploymentProperties,
                                                 PluginModelResolver pluginModelResolver,
                                                 PluginDescriptorResolver pluginDescriptorResolver);

  public ArtifactPluginDescriptor createPluginDescriptor(MulePluginModel pluginModel,
                                                         DeployableArtifactDescriptor ownerDescriptor);

}
