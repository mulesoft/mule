/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.mule.runtime.container.api.MuleFoldersUtil.PLUGINS_FOLDER;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppClassesFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppLibFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppSharedPluginLibsFolder;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.deployment.internal.artifact.ArtifactFactoryUtils.getDeploymentFile;
import static org.mule.runtime.module.deployment.internal.descriptor.ApplicationDescriptor.DEFAULT_APP_PROPERTIES_RESOURCE;

import org.mule.runtime.core.util.PropertiesUtils;
import org.mule.runtime.module.deployment.internal.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.internal.descriptor.EmptyApplicationDescriptor;
import org.mule.runtime.module.deployment.internal.descriptor.PropertiesDescriptorParser;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.util.FileJarExplorer;
import org.mule.runtime.module.artifact.util.JarExplorer;
import org.mule.runtime.module.artifact.util.JarInfo;
import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginRepository;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Creates artifact descriptor for application
 */
public class ApplicationDescriptorFactory implements ArtifactDescriptorFactory<ApplicationDescriptor> {

  public static final String SYSTEM_PROPERTY_OVERRIDE = "-O";

  private final ArtifactPluginRepository applicationPluginRepository;
  private final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;

  public ApplicationDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                      ArtifactPluginRepository applicationPluginRepository) {
    checkArgument(artifactPluginDescriptorLoader != null, "ApplicationPluginDescriptorFactory cannot be null");
    checkArgument(applicationPluginRepository != null, "ApplicationPluginRepository cannot be null");
    this.applicationPluginRepository = applicationPluginRepository;
    this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
  }

  public ApplicationDescriptor create(File artifactFolder) throws ArtifactDescriptorCreateException {
    if (!artifactFolder.exists()) {
      throw new IllegalArgumentException(format("Application directory does not exist: '%s'", artifactFolder));
    }

    final String appName = artifactFolder.getName();
    ApplicationDescriptor desc;

    try {
      final File deployPropertiesFile = getDeploymentFile(artifactFolder);
      if (deployPropertiesFile != null) {
        // lookup the implementation by extension
        final PropertiesDescriptorParser descriptorParser = new PropertiesDescriptorParser();
        desc = descriptorParser.parse(deployPropertiesFile, appName);
      } else {
        desc = new EmptyApplicationDescriptor(appName);
      }

      // get a ref to an optional app props file (right next to the descriptor)
      final File appPropsFile = new File(artifactFolder, DEFAULT_APP_PROPERTIES_RESOURCE);
      setApplicationProperties(desc, appPropsFile);

      final Set<ArtifactPluginDescriptor> plugins = parsePluginDescriptors(artifactFolder, desc);
      verifyPluginExportedPackages(getAllApplicationPlugins(plugins));
      desc.setPlugins(plugins);
      desc.setRuntimeLibs(findLibraries(appName));
      desc.setSharedRuntimeLibs(findSharedLibraries(appName));
      desc.setClassLoaderFilter(createApplicationClassLoaderFilter(desc));
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException("Unable to create application descriptor", e);
    }

    return desc;
  }

  private URL[] findLibraries(String appName) throws MalformedURLException {
    return findJars(getAppLibFolder(appName)).toArray(new URL[0]);
  }

  private URL[] findSharedLibraries(String appName) throws MalformedURLException {
    return findJars(getAppSharedPluginLibsFolder(appName)).toArray(new URL[0]);
  }

  private ArtifactClassLoaderFilter createApplicationClassLoaderFilter(ApplicationDescriptor descriptor) {
    final JarInfo librariesInfo = findExportedResources(descriptor.getSharedRuntimeLibs());
    final JarInfo classesInfo;
    try {
      final File appClassesFolder = getAppClassesFolder(descriptor.getName());
      if (appClassesFolder.exists()) {
        classesInfo = findExportedResources(appClassesFolder.toURI().toURL());
      } else {
        classesInfo = new JarInfo(emptySet(), emptySet());
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException("Cannot read application classes folder", e);
    }
    librariesInfo.getPackages().addAll(classesInfo.getPackages());
    librariesInfo.getResources().addAll(classesInfo.getResources());

    return new DefaultArtifactClassLoaderFilter(librariesInfo.getPackages(), librariesInfo.getResources());
  }

  private JarInfo findExportedResources(URL... libraries) {
    Set<String> packages = new HashSet<>();
    Set<String> resources = new HashSet<>();
    final JarExplorer jarExplorer = new FileJarExplorer();

    for (URL library : libraries) {
      final JarInfo jarInfo = jarExplorer.explore(library);
      packages.addAll(jarInfo.getPackages());
      resources.addAll(jarInfo.getResources());
    }

    return new JarInfo(packages, resources);
  }

  private List<URL> findJars(File dir) throws MalformedURLException {
    List<URL> result = new LinkedList<>();

    if (dir.exists() && dir.canRead()) {
      @SuppressWarnings("unchecked")
      Collection<File> jars = listFiles(dir, new String[] {"jar"}, false);

      for (File jar : jars) {
        result.add(jar.toURI().toURL());
      }
    }

    return result;
  }

  private List<ArtifactPluginDescriptor> getAllApplicationPlugins(Set<ArtifactPluginDescriptor> plugins) {
    final List<ArtifactPluginDescriptor> result =
        new LinkedList<>(applicationPluginRepository.getContainerArtifactPluginDescriptors());
    result.addAll(plugins);

    // Sorts plugins by name to ensure consistent deployment
    result.sort(new Comparator<ArtifactPluginDescriptor>() {

      @Override
      public int compare(ArtifactPluginDescriptor descriptor1, ArtifactPluginDescriptor descriptor2) {
        return descriptor1.getName().compareTo(descriptor2.getName());
      }
    });

    return result;
  }

  private void verifyPluginExportedPackages(List<ArtifactPluginDescriptor> plugins) {
    final Map<String, List<String>> exportedPackages = new HashMap<>();

    boolean error = false;
    for (ArtifactPluginDescriptor plugin : plugins) {
      for (String packageName : plugin.getClassLoaderFilter().getExportedClassPackages()) {
        List<String> exportedOn = exportedPackages.get(packageName);

        if (exportedOn == null) {
          exportedOn = new LinkedList<>();
          exportedPackages.put(packageName, exportedOn);
        } else {
          error = true;
        }
        exportedOn.add(plugin.getName());
      }
    }

    // TODO(pablo.kraan): MULE-9649 - de add validation when a decision is made about how to, in a plugin,
  }

  private Set<ArtifactPluginDescriptor> parsePluginDescriptors(File appDir, ApplicationDescriptor appDescriptor)
      throws IOException {
    final File pluginsDir = new File(appDir, PLUGINS_FOLDER);
    String[] pluginZips = pluginsDir.list(new SuffixFileFilter(".zip"));
    if (pluginZips == null || pluginZips.length == 0) {
      return emptySet();
    }

    Arrays.sort(pluginZips);
    Set<ArtifactPluginDescriptor> pds = new HashSet<>(pluginZips.length);

    for (String pluginZip : pluginZips) {
      String unpackDestinationFolder = appDescriptor.getName() + separator + PLUGINS_FOLDER + separator;
      File pluginZipFile = new File(pluginsDir, pluginZip);
      pds.add(artifactPluginDescriptorLoader
          .load(pluginZipFile, new File(MuleContainerBootstrapUtils.getMuleTmpDir(), unpackDestinationFolder)));
    }
    return pds;
  }

  public void setApplicationProperties(ApplicationDescriptor desc, File appPropsFile) {
    // ugh, no straightforward way to convert a HashTable to a map
    Map<String, String> m = new HashMap<>();

    if (appPropsFile.exists() && appPropsFile.canRead()) {
      final Properties props;
      try {
        props = PropertiesUtils.loadProperties(appPropsFile.toURI().toURL());
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to obtain application properties file URL", e);
      }
      for (Object key : props.keySet()) {
        m.put(key.toString(), props.getProperty(key.toString()));
      }
    }

    // Override with any system properties prepended with "-O" for ("override"))
    Properties sysProps = System.getProperties();
    for (Map.Entry<Object, Object> entry : sysProps.entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith(SYSTEM_PROPERTY_OVERRIDE)) {
        m.put(key.substring(SYSTEM_PROPERTY_OVERRIDE.length()), entry.getValue().toString());
      }
    }
    desc.setAppProperties(m);
  }
}
