package org.mule.runtime.module.service.api.artifact;

import org.mule.runtime.container.api.ContainerDependantArtifactClassLoaderFactory;
import org.mule.runtime.container.internal.DefaultMuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.exception.ArtifactClassloaderCreationException;

public interface IServiceClassLoaderFactory
    extends ArtifactClassLoaderFactory<ServiceDescriptor>, ContainerDependantArtifactClassLoaderFactory<ServiceDescriptor> {

  @Deprecated
  ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor, ClassLoader parent,
                             ClassLoaderLookupPolicy lookupPolicy)
      throws ArtifactClassloaderCreationException;

  default ArtifactClassLoader create(String serviceName, ServiceDescriptor descriptor, ArtifactClassLoader containerClassLoader)
      throws ArtifactClassloaderCreationException {
    return create(serviceName, descriptor, new DefaultMuleContainerClassLoaderWrapper(containerClassLoader));
  }
}
