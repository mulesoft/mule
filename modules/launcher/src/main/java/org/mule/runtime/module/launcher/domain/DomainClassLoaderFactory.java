/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.domain;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getDomainLibFolder;
import static org.mule.runtime.module.launcher.domain.Domain.DEFAULT_DOMAIN_NAME;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.Preconditions;
import org.mule.runtime.core.util.SystemUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ShutdownListener;
import org.mule.runtime.module.launcher.DeploymentException;
import org.mule.runtime.module.launcher.MuleFoldersUtil;
import org.mule.runtime.module.launcher.MuleSharedDomainClassLoader;
import org.mule.runtime.module.launcher.application.FilePackageDiscoverer;
import org.mule.runtime.module.launcher.application.PackageDiscoverer;
import org.mule.runtime.module.launcher.descriptor.DomainDescriptor;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link ArtifactClassLoader} for domain artifacts.
 */
public class DomainClassLoaderFactory implements DeployableArtifactClassLoaderFactory<DomainDescriptor> {

  protected static final Logger logger = LoggerFactory.getLogger(DomainClassLoaderFactory.class);
  private final ClassLoader parentClassLoader;

  private Map<String, ArtifactClassLoader> domainArtifactClassLoaders = new HashMap<>();
  private PackageDiscoverer packageDiscoverer = new FilePackageDiscoverer();

  /**
   * Creates a new instance
   *
   * @param parentClassLoader parent classLoader of the created instance. Can be null.
   */
  public DomainClassLoaderFactory(ClassLoader parentClassLoader) {
    this.parentClassLoader = parentClassLoader;
  }

  public void setPackageDiscoverer(PackageDiscoverer packageDiscoverer) {
    this.packageDiscoverer = packageDiscoverer;
  }

  @Override
  public ArtifactClassLoader create(ArtifactClassLoader parent, DomainDescriptor descriptor,
                                    List<ArtifactClassLoader> artifactClassLoaders) {
    String domain = descriptor.getName();
    Preconditions.checkArgument(domain != null, "Domain name cannot be null");

    ArtifactClassLoader domainClassLoader = domainArtifactClassLoaders.get(domain);
    if (domainClassLoader != null) {
      return domainClassLoader;
    } else {
      synchronized (this) {
        domainClassLoader = domainArtifactClassLoaders.get(domain);
        if (domainClassLoader == null) {
          if (domain.equals(DEFAULT_DOMAIN_NAME)) {
            domainClassLoader = getDefaultDomainClassLoader(parent.getClassLoaderLookupPolicy());
          } else {
            domainClassLoader = getCustomDomainClassLoader(parent.getClassLoaderLookupPolicy(), domain);
          }

          domainArtifactClassLoaders.put(domain, domainClassLoader);
        }
      }
    }

    return domainClassLoader;
  }

  private ArtifactClassLoader getCustomDomainClassLoader(ClassLoaderLookupPolicy containerLookupPolicy, String domain) {
    validateDomain(domain);
    final List<URL> urls = getDomainUrls(domain);
    final Map<String, ClassLoaderLookupStrategy> domainLookStrategies = getLookStrategiesFrom(urls);
    final ClassLoaderLookupPolicy domainLookupPolicy = containerLookupPolicy.extend(domainLookStrategies);

    ArtifactClassLoader classLoader = new MuleSharedDomainClassLoader(domain, parentClassLoader, domainLookupPolicy, urls);

    return createClassLoaderUnregisterWrapper(classLoader);
  }

  private Map<String, ClassLoaderLookupStrategy> getLookStrategiesFrom(List<URL> libraries) {
    final Map<String, ClassLoaderLookupStrategy> result = new HashMap<>();

    for (URL library : libraries) {
      Set<String> packages = packageDiscoverer.findPackages(library);
      for (String packageName : packages) {
        result.put(packageName, PARENT_FIRST);
      }
    }

    return result;
  }

  private List<URL> getDomainUrls(String domain) throws DeploymentException {
    try {
      List<URL> urls = new LinkedList<>();
      urls.add(MuleFoldersUtil.getDomainFolder(domain).toURI().toURL());
      File domainLibraryFolder = getDomainLibFolder(domain);

      if (domainLibraryFolder.exists()) {
        Collection<File> jars = listFiles(domainLibraryFolder, new String[] {"jar"}, false);

        if (logger.isDebugEnabled()) {
          StringBuilder sb = new StringBuilder();
          sb.append("Loading Shared ClassLoader Domain: ").append(domain).append(SystemUtils.LINE_SEPARATOR);
          sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

          for (File jar : jars) {
            sb.append(jar.toURI().toURL()).append(SystemUtils.LINE_SEPARATOR);
          }

          sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

          logger.debug(sb.toString());
        }

        for (File jar : jars) {
          urls.add(jar.toURI().toURL());
        }
      }

      return urls;
    } catch (MalformedURLException e) {
      throw new DeploymentException(CoreMessages.createStaticMessage(format("Cannot read domain '%s' libraries", domain)), e);
    }
  }

  private ArtifactClassLoader getDefaultDomainClassLoader(ClassLoaderLookupPolicy containerLookupPolicy) {
    return new MuleSharedDomainClassLoader(DEFAULT_DOMAIN_NAME, parentClassLoader, containerLookupPolicy.extend(emptyMap()),
                                           emptyList());
  }

  private void validateDomain(String domain) {
    File domainFolder = new File(MuleContainerBootstrapUtils.getMuleDomainsDir(), domain);
    if (!(domainFolder.exists() && domainFolder.isDirectory())) {
      throw new DeploymentException(CoreMessages.createStaticMessage(format("Domain %s does not exists", domain)));
    }
  }

  private ArtifactClassLoader createClassLoaderUnregisterWrapper(final ArtifactClassLoader classLoader) {
    return new ArtifactClassLoader() {

      @Override
      public String getArtifactName() {
        return classLoader.getArtifactName();
      }

      @Override
      public URL findResource(String resource) {
        return classLoader.findResource(resource);
      }

      @Override
      public Enumeration<URL> findResources(String name) throws IOException {
        return classLoader.findResources(name);
      }

      @Override
      public URL findLocalResource(String resource) {
        return classLoader.findLocalResource(resource);
      }

      @Override
      public ClassLoader getClassLoader() {
        return classLoader.getClassLoader();
      }

      @Override
      public void dispose() {
        domainArtifactClassLoaders.remove(classLoader.getArtifactName());
        classLoader.dispose();
      }

      @Override
      public void addShutdownListener(ShutdownListener listener) {
        classLoader.addShutdownListener(listener);
      }

      @Override
      public ClassLoaderLookupPolicy getClassLoaderLookupPolicy() {
        return classLoader.getClassLoaderLookupPolicy();
      }
    };
  }
}
