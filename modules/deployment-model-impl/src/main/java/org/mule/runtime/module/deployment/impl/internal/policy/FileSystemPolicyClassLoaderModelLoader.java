/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.Map;

import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Creates a {@link ClassLoaderModel} from a policy's folder
 */
public class FileSystemPolicyClassLoaderModelLoader implements ClassLoaderModelLoader {

  public static final String FILE_SYSTEM_POLICY_MODEL_LOADER_ID = "FILE_SYSTEM_POLICY_MODEL_LOADER";
  static final String LIB_DIR = "lib";
  private static final String JAR_FILE = ".jar";


  @Override
  public String getId() {
    return FILE_SYSTEM_POLICY_MODEL_LOADER_ID;
  }

  /**
   * Given a policy template's location it will build a {@link ClassLoaderModel} taking in account jars located inside the
   * {@value LIB_DIR} folder and resources located inside the artifact folder.
   *
   * @param artifactFolder {@link File} where the current plugin to work with.
   * @param attributes collection of attributes describing the loader. Non null.
   * @param artifactType artifactType the type of the artifact of the descriptor to be loaded.
   * 
   * @return a {@link ClassLoaderModel} loaded with all its dependencies and URLs
   */
  @Override
  public ClassLoaderModel load(File artifactFolder, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {

    final ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModel.ClassLoaderModelBuilder();

    loadUrls(classLoaderModelBuilder, artifactFolder);

    return classLoaderModelBuilder.build();
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return true;
  }

  private void loadUrls(ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder, File artifactFolder)
      throws InvalidDescriptorLoaderException {
    try {
      classLoaderModelBuilder.containing(artifactFolder.toURI().toURL());
      final File libDir = new File(artifactFolder, LIB_DIR);
      if (libDir.exists()) {
        final File[] jars = libDir.listFiles((FilenameFilter) new SuffixFileFilter(JAR_FILE));
        for (int i = 0; i < jars.length; i++) {
          classLoaderModelBuilder.containing(jars[i].toURI().toURL());
        }
      }
    } catch (MalformedURLException e) {
      throw new InvalidDescriptorLoaderException("Failed to create plugin descriptor " + artifactFolder);
    }
  }
}
