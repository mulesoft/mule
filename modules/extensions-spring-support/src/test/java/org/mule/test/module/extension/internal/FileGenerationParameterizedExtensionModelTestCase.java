/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.internal;

import static org.mule.runtime.api.dsl.DslResolvingContext.nullDslResolvingContext;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;

import static java.util.stream.Collectors.toUnmodifiableSet;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public abstract class FileGenerationParameterizedExtensionModelTestCase extends ParameterizedExtensionModelTestCase {

  @Parameterized.Parameter(0)
  public ExtensionModelLoader loader;

  @Parameterized.Parameter(2)
  public String expectedFilePath;

  @Parameterized.Parameter(3)
  public ArtifactCoordinates artifactCoordinates;

  @Parameterized.Parameter(4)
  public List<Class<? extends ExtensionModel>> dependencies;

  protected String expectedContent;
  protected DslResolvingContext dslResolvingContext;

  @Before
  public void setup() throws IOException {
    expectedContent = getResourceAsString(getExpectedFilesDir() + expectedFilePath, getClass());
  }

  @Before
  public void createDslResolvingContext() throws IOException {
    // TODO MULE-11797: as this utils is consumed from
    // org.mule.runtime.module.extension.internal.capability.xml.schema.AbstractXmlResourceFactory.generateResource(org.mule.runtime.api.meta.model.ExtensionModel),
    // this util should get dropped once the ticket gets implemented.
    dslResolvingContext = DslResolvingContext.getDefault(dependencies
        .stream()
        // loads dependencies as well
        .map(extModelDepClass -> loadExtension(extModelDepClass, loader, null, nullDslResolvingContext()))
        .collect(toUnmodifiableSet()));
  }

  @Test
  public final void generate() throws Exception {
    String actual = doGenerate(doLoadExtension());
    try {
      assertEquals(expectedContent, actual);
    } catch (Throwable t) {
      if (shouldUpdateExpectedFilesOnError()) {
        File root = new File(getResourceAsUrl(getExpectedFilesDir() + expectedFilePath, getClass()).toURI());

        for (root = root.getParentFile(); !root.getName().equals("target"); root = root.getParentFile()) {
          ;
        }
        root = root.getParentFile();

        File testDir = new File(root, "src/test/resources/" + getExpectedFilesDir());
        File target = new File(testDir, expectedFilePath);
        stringToFile(target.getAbsolutePath(), actual);

        System.out.println(expectedFilePath + " fixed");
      }
      throw t;
    }
  }

  protected abstract ExtensionModel doLoadExtension();

  protected abstract String doGenerate(ExtensionModel extensionUnderTest) throws Exception;

  protected abstract void assertEquals(String expectedContent, String actualContent) throws Exception;

  protected abstract String getExpectedFilesDir();

  /**
   * Utility to batch fix input files when severe model changes are introduced. Use carefully, not a mechanism to get away with
   * anything. First check why the generated json is different and make sure you're not introducing any bugs. This should NEVER be
   * committed as true
   *
   * @return whether or not the "expected" test files should be updated when comparison fails
   */
  protected boolean shouldUpdateExpectedFilesOnError() {
    return false;
  }


}
