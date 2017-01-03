/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;

import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Creates a {@link ClassLoaderModel} from a policy's folder
 */
public class FileSystemPolicyClassLoaderModelLoader {

  protected static final String LIB_DIR = "lib";
  protected static final String CLASSES_DIR = "classes";
  private static final String JAR_FILE = ".jar";


  /**
   * Given a policy template's location it will build a {@link ClassLoaderModel} taking in account jars located inside the {@value LIB_DIR}
   * folder and resources located inside the {@value CLASSES_DIR} folder.
   *
   * @param artifactFolder {@link File} where the current plugin to work with.
   * @return a {@link ClassLoaderModel} loaded with all its dependencies and URLs.
   */
  public ClassLoaderModel loadClassLoaderModel(File artifactFolder) {

    final ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModel.ClassLoaderModelBuilder();

    loadUrls(classLoaderModelBuilder, artifactFolder);

    return classLoaderModelBuilder.build();
  }

  private void loadUrls(ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder, File artifactFolder) {
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
  }
}
