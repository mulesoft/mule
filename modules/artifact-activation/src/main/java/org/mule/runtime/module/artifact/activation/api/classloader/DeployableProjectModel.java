package org.mule.runtime.module.artifact.activation.api.classloader;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.List;

/**
 * Represents the structure of a project, providing what is needed in order to create its {@link ArtifactDescriptor} with a
 * {@link ArtifactDescriptorFactory}.
 * <p>
 * Implementations may be coupled to a specific build tool, dependency management system or project structure.
 * 
 * @since 4.5
 */
public interface DeployableProjectModel {

  /**
   * If an empty result is returned, it means that everything will be exported.
   * 
   * @return the packages configured by the project developer to be exported.
   */
  List<String> getExportedPackages();

  /**
   * If an empty result is returned, it means that everything will be exported.
   * 
   * @return the resources configured by the project developer to be exported.
   */
  List<String> getExportedResources();

  // TODO: project dependencies
  // TODO: sharedLibraries
  // TODO: additionalPluginDependencies
}
