/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import java.net.URL;

/**
 * A {@link ClassPathClassifier} builds the {@link ArtifactUrlClassification} that would be used for creating
 * the {@link ClassLoader} to run the test.
 *
 * @since 4.0
 */
public interface ClassPathClassifier
{
    /**
     * Implements the logic for classifying how the URLs provided for dependencies should be arranged
     *
     * @param context {@link ClassPathClassifierContext} to be used during the classification
     * @return a {@link ArtifactUrlClassification} with the corresponding {@link URL}s
     */
    ArtifactUrlClassification classify(ClassPathClassifierContext context);
}
