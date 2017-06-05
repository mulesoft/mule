/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.domain;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainLibFolder;
import static org.mule.runtime.deployment.model.api.domain.Domain.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.artifact.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.reboot.MuleContainerBootstrapUtils.getMuleDomainsDir;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.classloader.ShutdownListener;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.util.FileJarExplorer;
import org.mule.runtime.module.artifact.util.JarExplorer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
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
  private JarExplorer jarExplorer = new FileJarExplorer();

  /**
   * Creates a new instance
   *
   * @param parentClassLoader parent classLoader of the created instance. Can be null.
   */
  public DomainClassLoaderFactory(ClassLoader parentClassLoader) {
    checkArgument(parentClassLoader != null, "parentClassLoader cannot be null");
    this.parentClassLoader = parentClassLoader;
  }

  public void setJarExplorer(JarExplorer jarExplorer) {
    this.jarExplorer = jarExplorer;
  }

  /**
   * @param domainName name of the domain. Non empty.
   * @return the unique identifier for the domain in the container.
   */
  public static String getDomainId(String domainName) {
    checkArgument(!isEmpty(domainName), "domainName cannot be empty");

    return "domain/" + domainName;
  }

  @Override
  public ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, DomainDescriptor descriptor,
                                    List<ArtifactClassLoader> artifactClassLoaders) {
    String domainId = getDomainId(descriptor.getName());

    ArtifactClassLoader domainClassLoader = domainArtifactClassLoaders.get(domainId);
    if (domainClassLoader != null) {
      return domainClassLoader;
    } else {
      synchronized (this) {
        domainClassLoader = domainArtifactClassLoaders.get(domainId);
        if (domainClassLoader == null) {
          if (descriptor.getName().equals(DEFAULT_DOMAIN_NAME)) {
            domainClassLoader = getDefaultDomainClassLoader(parent.getClassLoaderLookupPolicy());
          } else {
            domainClassLoader = getCustomDomainClassLoader(parent.getClassLoaderLookupPolicy(), descriptor);
          }

          domainArtifactClassLoaders.put(domainId, domainClassLoader);
        }
      }
    }

    return domainClassLoader;
  }

  private ArtifactClassLoader getCustomDomainClassLoader(ClassLoaderLookupPolicy containerLookupPolicy, DomainDescriptor domain) {
    validateDomain(domain.getName());
    final List<URI> uris = getDomainUrls(domain.getName());
    final Map<String, LookupStrategy> domainLookStrategies = getLookStrategiesFrom(uris);
    final ClassLoaderLookupPolicy domainLookupPolicy = containerLookupPolicy.extend(domainLookStrategies);

    ArtifactClassLoader classLoader =
        new MuleSharedDomainClassLoader(domain, parentClassLoader, domainLookupPolicy, uris.stream().map(uri -> {
          try {
            return uri.toURL();
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
        }).collect(toList()));

    return createClassLoaderUnregisterWrapper(classLoader);
  }

  private Map<String, LookupStrategy> getLookStrategiesFrom(List<URI> libraries) {
    final Map<String, LookupStrategy> result = new HashMap<>();

    for (URI library : libraries) {
      Set<String> packages = jarExplorer.explore(library).getPackages();
      for (String packageName : packages) {
        result.put(packageName, PARENT_FIRST);
      }
    }

    return result;
  }

  private List<URI> getDomainUrls(String domain) throws DeploymentException {
    List<URI> urls = new LinkedList<>();
    urls.add(MuleFoldersUtil.getDomainFolder(domain).toURI());
    File domainLibraryFolder = getDomainLibFolder(domain);

    if (domainLibraryFolder.exists()) {
      Collection<File> jars = listFiles(domainLibraryFolder, new String[] {"jar"}, false);

      if (logger.isDebugEnabled()) {
        StringBuilder sb = new StringBuilder();
        sb.append("Loading Shared ClassLoader Domain: ").append(domain).append(LINE_SEPARATOR);
        sb.append("=============================").append(LINE_SEPARATOR);

        for (File jar : jars) {
          sb.append(jar.getPath()).append(LINE_SEPARATOR);
        }

        sb.append("=============================").append(LINE_SEPARATOR);

        logger.debug(sb.toString());
      }

      for (File jar : jars) {
        urls.add(jar.toURI());
      }
    }

    return urls;
  }

  private ArtifactClassLoader getDefaultDomainClassLoader(ClassLoaderLookupPolicy containerLookupPolicy) {
    return new MuleSharedDomainClassLoader(new DomainDescriptor(DEFAULT_DOMAIN_NAME), parentClassLoader,
                                           containerLookupPolicy.extend(emptyMap()), emptyList());
  }

  private void validateDomain(String domain) {
    File domainFolder = new File(getMuleDomainsDir(), domain);
    if (!(domainFolder.exists() && domainFolder.isDirectory())) {
      throw new DeploymentException(createStaticMessage(format("Domain %s does not exists", domain)));
    }
  }

  private ArtifactClassLoader createClassLoaderUnregisterWrapper(final ArtifactClassLoader classLoader) {
    return new ArtifactClassLoader() {

      @Override
      public String getArtifactId() {
        return classLoader.getArtifactId();
      }

      @Override
      public <T extends ArtifactDescriptor> T getArtifactDescriptor() {
        return classLoader.getArtifactDescriptor();
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
      public Class<?> findLocalClass(String name) throws ClassNotFoundException {
        return classLoader.findLocalClass(name);
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
        domainArtifactClassLoaders.remove(classLoader.getArtifactId());
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
