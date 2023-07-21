/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.api;

import java.net.URL;

/**
 * A {@link ClassPathClassifier} builds the {@link ArtifactsUrlClassification} that would be used for creating the
 * {@link ClassLoader} to run the test.
 *
 * @since 4.0
 */
public interface ClassPathClassifier {

  /**
   * Implements the logic for classifying how the URLs provided for dependencies should be arranged
   *
   * @param context {@link ClassPathClassifierContext} to be used during the classification
   * @return a {@link ArtifactsUrlClassification} with the corresponding {@link URL}s
   */
  ArtifactsUrlClassification classify(ClassPathClassifierContext context);
}
