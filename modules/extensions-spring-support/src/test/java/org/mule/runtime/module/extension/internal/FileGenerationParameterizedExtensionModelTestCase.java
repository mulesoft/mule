/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public abstract class FileGenerationParameterizedExtensionModelTestCase extends ParameterizedExtensionModelTestCase {

  public static class ResourceExtensionUnitTest extends ExtensionUnitTest {

    public static ResourceExtensionUnitTest newUnitTest(ExtensionModelLoader loader, Class<?> extensionClass, String filename) {
      return new ResourceExtensionUnitTest(loader, extensionClass, filename);
    }

    private final String filename;

    protected ResourceExtensionUnitTest(ExtensionModelLoader loader, Class<?> extensionClass, String filename) {
      super(loader, extensionClass);
      this.filename = filename;
    }

    public String getFilename() {
      return filename;
    }

    @Override
    protected Object[] buildTestParams(ExtensionModel extensionModel) {
      return new Object[] {extensionModel, filename};
    }
  }

  @Parameterized.Parameter(1)
  public String expectedFilePath;

  protected String expectedContent;

  @Before
  public void setup() throws IOException {
    expectedContent = getResourceAsString(getExpectedFilesDir() + expectedFilePath, getClass());
  }

  @Test
  public final void generate() throws Exception {
    String actual = doGenerate(extensionUnderTest);
    try {
      assertEquals(expectedContent, actual);
    } catch (Throwable t) {
      if (shouldUpdateExpectedFilesOnError()) {
        File root = new File(getResourceAsUrl(getExpectedFilesDir() + expectedFilePath, getClass()).toURI()).getParentFile()
                .getParentFile().getParentFile().getParentFile();
        File testDir = new File(root, "src/test/resources/" + getExpectedFilesDir());
        File target = new File(testDir, expectedFilePath);
        stringToFile(target.getAbsolutePath(), actual);

        System.out.println(expectedFilePath + " fixed");
      }
      throw t;
    }
  }

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
