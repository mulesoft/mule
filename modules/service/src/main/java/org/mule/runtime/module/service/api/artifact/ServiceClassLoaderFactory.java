package org.mule.runtime.module.service.api.artifact;

import org.mule.runtime.container.api.ContainerDependantArtifactClassLoaderFactory;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 *
 * @deprecated since 4.8, use {@link IServiceClassLoaderFactory}
 */
@Deprecated
public class ServiceClassLoaderFactory
    implements ArtifactClassLoaderFactory<ServiceDescriptor>, ContainerDependantArtifactClassLoaderFactory<ServiceDescriptor> {

  @Override
  @Deprecated
  ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor, ClassLoader parent,
                             ClassLoaderLookupPolicy lookupPolicy);

  @Override
  ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor,
                             MuleContainerClassLoaderWrapper containerClassLoader);

  @Override
  void setParentLayerFrom(Class clazz);
}
