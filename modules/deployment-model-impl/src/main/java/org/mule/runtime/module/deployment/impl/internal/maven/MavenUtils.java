/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.io.File.separator;
import static java.lang.String.format;
import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.util.FileUtils.createFile;
import static org.mule.runtime.core.internal.util.JarUtils.getUrlWithinJar;
import static org.mule.runtime.core.internal.util.JarUtils.getUrlsWithinJar;
import static org.mule.runtime.core.internal.util.JarUtils.loadFileContentFrom;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_PATH_INSIDE_JAR;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_POM;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.META_INF;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_ARTIFACT_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_GROUP_ID;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Provides utility methods to wrk with Maven
 */
public class MavenUtils {

  private static final String SHARED_LIBRARIES_ELEMENT = "sharedLibraries";

  /**
   * Returns the {@link Model} from a given artifact folder
   * 
   * @param artifactFile file containing the artifact content.
   * @return the {@link Model} from the {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file if available
   * @throws ArtifactDescriptorCreateException if the artifact jar does not contain a
   *         {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file or the file can' be loaded
   */
  public static Model getPomModelFromJar(File artifactFile) {
    String pomFilePath = MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_PLUGIN_POM;
    try {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      return reader.read(new ByteArrayInputStream(loadFileContentFrom(getPomUrlFromJar(artifactFile)).get()));
    } catch (IOException | XmlPullParserException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue reading '%s' for the plugin '%s'",
                                                         pomFilePath, artifactFile.getAbsolutePath()),
                                                  e);
    }
  }

  /**
   * Finds the URL of the pom file within the artifact file.
   * 
   * @param artifactFile the artifact file to search for the pom file.
   * @return the URL to the pom file.
   */
  public static URL getPomUrlFromJar(File artifactFile) {
    String pomFilePath = MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_PLUGIN_POM;
    URL possibleUrl;
    try {
      possibleUrl = getUrlWithinJar(artifactFile, pomFilePath);
      try (InputStream ignored = possibleUrl.openStream()) {
        return possibleUrl;
      } catch (Exception e) {
        List<URL> jarMavenUrls = getUrlsWithinJar(artifactFile, META_INF + "/" + "maven");
        Optional<URL> pomUrl = jarMavenUrls.stream().filter(url -> url.toString().endsWith("pom.xml")).findAny();
        if (!pomUrl.isPresent()) {
          throw new ArtifactDescriptorCreateException(format("The identifier '%s' requires the file '%s' within the artifact(error found while reading plugin '%s')",
                                                             artifactFile.getName(), "pom.xml", artifactFile.getAbsolutePath()));
        }
        return pomUrl.get();
      }
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Returns the {@link Model} from a given artifact folder
   *
   * @param artifactFolder folder containing the exploded artifact file.
   * @return the {@link Model} from the {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM} file if available
   * @throws ArtifactDescriptorCreateException if the folder does not contain a {@value ArtifactPluginDescriptor#MULE_PLUGIN_POM}
   *         file or the file can' be loaded
   */
  public static Model getPomModelFolder(File artifactFolder) {
    final File mulePluginPom = lookupPomFromMavenLocation(artifactFolder);
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model model;
    try (FileReader mulePluginPomFileReader = new FileReader(mulePluginPom)) {
      model = reader.read(mulePluginPomFileReader);
    } catch (IOException | XmlPullParserException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue reading '%s' for the plugin '%s'",
                                                         mulePluginPom.getName(), artifactFolder.getAbsolutePath()),
                                                  e);
    }
    return model;
  }

  /**
   * Updates the pom file from an artifact.
   * 
   * @param artifactFolder the artifact folder
   * @param model the new pom model
   */
  public static void updateArtifactPom(File artifactFolder, Model model) {
    final File mulePluginPom = lookupPomFromMavenLocation(artifactFolder);
    MavenXpp3Writer writer = new MavenXpp3Writer();
    try (FileOutputStream outputStream = new FileOutputStream(mulePluginPom)) {
      writer.write(outputStream, model);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Creates the pom file for a deployable artifact inside the artifact exploded folder
   *
   * @param artifactFolder the deployable artifact folder
   * @param model the pom model
   */
  public static void createDeployablePomFile(File artifactFolder, Model model) {
    File pomFileLocation =
        new File(artifactFolder, Paths.get("META-INF", "maven", model.getGroupId(), model.getArtifactId(), "pom.xml").toString());
    try {
      createFile(pomFileLocation.getAbsolutePath());
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
    try (FileWriter fileWriter = new FileWriter(pomFileLocation)) {
      new MavenXpp3Writer().write(fileWriter, model);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }


  private static File lookupPomFromMavenLocation(File artifactFolder) {
    File mulePluginPom = null;
    File lookupFolder = new File(artifactFolder, "META-INF" + separator + "maven");
    while (lookupFolder != null && lookupFolder.exists()) {
      File possiblePomLocation = new File(lookupFolder, MULE_PLUGIN_POM);
      if (possiblePomLocation.exists()) {
        mulePluginPom = possiblePomLocation;
        break;
      }
      File[] directories = lookupFolder.listFiles((FileFilter) DIRECTORY);
      checkState(directories != null || directories.length == 0,
                 format("No directories under %s so pom.xml file for artifact in folder %s could not be found",
                        lookupFolder.getAbsolutePath(), artifactFolder.getAbsolutePath()));
      checkState(directories.length == 1,
                 format("More than one directory under %s so pom.xml file for artifact in folder %s could not be found",
                        lookupFolder.getAbsolutePath(), artifactFolder.getAbsolutePath()));
      lookupFolder = directories[0];
    }

    // final File mulePluginPom = new File(artifactFolder, MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_POM);
    if (mulePluginPom == null || !mulePluginPom.exists()) {
      throw new ArtifactDescriptorCreateException(format("The maven bundle loader requires the file pom.xml (error found while reading plugin '%s')",
                                                         artifactFolder.getName()));
    }
    return mulePluginPom;
  }

  /**
   * Adds a shared library to the pom model. If the plugin does not exists yet in the model then it will create it.
   * 
   * @param model the pom model
   * @param dependency the descriptor of the dependency
   */
  public static void addSharedLibraryDependency(Model model, Dependency dependency) {
    Build build = model.getBuild();
    if (build == null) {
      build = new Build();
      model.setBuild(build);
    }
    List<Plugin> plugins = build.getPlugins();
    if (plugins == null) {
      plugins = new ArrayList<>();
      build.setPlugins(plugins);
    }
    Optional<Plugin> pluginOptional = plugins.stream().filter(plugin -> plugin.getGroupId().equals(MULE_MAVEN_PLUGIN_GROUP_ID)
        && plugin.getArtifactId().equals(MULE_MAVEN_PLUGIN_ARTIFACT_ID)).findFirst();
    List<Plugin> finalPlugins = plugins;
    Plugin plugin = pluginOptional.orElseGet(() -> {
      Plugin muleMavenPlugin = new Plugin();
      muleMavenPlugin.setGroupId(MULE_MAVEN_PLUGIN_GROUP_ID);
      muleMavenPlugin.setArtifactId(MULE_MAVEN_PLUGIN_ARTIFACT_ID);
      finalPlugins.add(muleMavenPlugin);
      return muleMavenPlugin;
    });
    Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
    if (configuration == null) {
      configuration = new Xpp3Dom("configuration");
      plugin.setConfiguration(configuration);
    }
    Xpp3Dom sharedLibrariesDom = configuration.getChild(SHARED_LIBRARIES_ELEMENT);
    if (sharedLibrariesDom == null) {
      sharedLibrariesDom = new Xpp3Dom(SHARED_LIBRARIES_ELEMENT);
      configuration.addChild(sharedLibrariesDom);
    }
    Xpp3Dom sharedLibraryDom = new Xpp3Dom("sharedLibrary");
    sharedLibrariesDom.addChild(sharedLibraryDom);
    Xpp3Dom groupIdDom = new Xpp3Dom("groupId");
    groupIdDom.setValue(dependency.getGroupId());
    sharedLibraryDom.addChild(groupIdDom);
    Xpp3Dom artifactIdDom = new Xpp3Dom("artifactId");
    artifactIdDom.setValue(dependency.getArtifactId());
    sharedLibraryDom.addChild(artifactIdDom);
  }

}
