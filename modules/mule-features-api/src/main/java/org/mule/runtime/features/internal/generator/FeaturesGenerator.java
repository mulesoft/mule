/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.features.internal.generator;

import java.io.File;
import java.io.IOException;

public class FeaturesGenerator {

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    File outputDir = new File(args[0]);
    new MuleSystemPropertiesGenerator(outputDir).generate();
    new MuleFeaturesGenerator(outputDir).generate();
  }
}
