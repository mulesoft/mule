/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.DEFAULT_POLICY_CONFIGURATION_RESOURCE;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Creates descriptors for policy templates
 */
public class PolicyTemplateDescriptorFactory implements ArtifactDescriptorFactory<PolicyTemplateDescriptor> {

  protected static final String CLASSES_DIR = "classes";
  protected static final String LIB_DIR = "lib";
  private static final String JAR_FILE = ".jar";

  public static final String POLICY_PROPERTIES = "policy.properties";
  public static final String MISSING_POLICY_PROPERTIES_FILE = "Policy must contain a " + POLICY_PROPERTIES + " file";
  public static final String POLICY_PROPERTIES_FILE_READ_ERROR = "Cannot read " + POLICY_PROPERTIES + " file";

  @Override
  public PolicyTemplateDescriptor create(File artifactFolder) throws ArtifactDescriptorCreateException {
    final String pluginName = artifactFolder.getName();
    final PolicyTemplateDescriptor descriptor = new PolicyTemplateDescriptor(pluginName);

    final File policyPropsFile = new File(artifactFolder, POLICY_PROPERTIES);
    if (!policyPropsFile.exists()) {
      throw new ArtifactDescriptorCreateException(MISSING_POLICY_PROPERTIES_FILE);
    }
    Properties props = new Properties();
    try {
      props.load(new FileReader(policyPropsFile));
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(POLICY_PROPERTIES_FILE_READ_ERROR, e);
    }

    ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModel.ClassLoaderModelBuilder();
    try {
      classLoaderModelBuilder.containing(new File(artifactFolder, CLASSES_DIR).toURI().toURL());
      final File libDir = new File(artifactFolder, LIB_DIR);
      if (libDir.exists()) {
        final File[] jars = libDir.listFiles((FilenameFilter) new SuffixFileFilter(JAR_FILE));
        for (int i = 0; i < jars.length; i++) {
          classLoaderModelBuilder.containing(jars[i].toURI().toURL());
        }
      }
    } catch (MalformedURLException e) {
      throw new ArtifactDescriptorCreateException("Failed to create plugin descriptor " + artifactFolder);
    }
    descriptor.setClassLoaderModel(classLoaderModelBuilder.build());
    descriptor.setConfigResourceFiles(new File[] {new File(artifactFolder, DEFAULT_POLICY_CONFIGURATION_RESOURCE)});
    descriptor.setRootFolder(artifactFolder);

    return descriptor;
  }
}
