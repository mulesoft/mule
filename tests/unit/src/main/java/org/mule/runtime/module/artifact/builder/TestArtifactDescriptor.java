/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.builder;

import java.io.File;

/**
 * Provides information about a mule artifact used for testing purposes.
 */
public interface TestArtifactDescriptor {

  /**
   * @return the artifact identifier. Non empty
   */
  String getId();

  /**
   * @return the path for the artifact file. Non empty
   */
  String getZipPath();

  /**
   * @return the name of the artifact config file. Null if the artifact does not have a config.
   */
  String getConfigFile();

  /**
   * @return path resulting of deploying the artifact relative to the artifact parent folder.
   */
  String getDeployedPath();

  /**
   * @return a non null file representation of the mule artifact.
   * @throws Exception if fiel cannot be created.
   */
  File getArtifactFile() throws Exception;
}
