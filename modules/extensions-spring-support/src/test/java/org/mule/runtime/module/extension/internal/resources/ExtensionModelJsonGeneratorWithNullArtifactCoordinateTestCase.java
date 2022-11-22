/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase.ResourceExtensionUnitTest.newUnitTest;

import static java.lang.Boolean.getBoolean;
import static java.util.Collections.singletonList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

@SmallTest
@RunWith(Parameterized.class)
public class ExtensionModelJsonGeneratorWithNullArtifactCoordinateTestCase
    extends FileGenerationParameterizedExtensionModelTestCase {

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "extensionModelJson.updateExpectedFilesOnError");

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    List<ResourceExtensionUnitTest> extensions;
    extensions = singletonList(newUnitTest(JAVA_LOADER, VeganExtension.class, "vegan-without-artifact-coordinates.json"));

    return createExtensionModels(extensions);
  }

  private final ExtensionModelJsonSerializer generator = new ExtensionModelJsonSerializer(true);

  @Override
  protected boolean shouldUpdateExpectedFilesOnError() {
    return UPDATE_EXPECTED_FILES_ON_ERROR;
  }

  @Override
  protected String getExpectedFilesDir() {
    return "models/";
  }

  @Override
  protected String doGenerate(ExtensionModel extensionUnderTest) {
    return generator.serialize(extensionUnderTest).trim();
  }

  @Override
  protected void assertEquals(String expectedContent, String actualContent) throws Exception {
    JSONAssert.assertEquals(expectedContent, actualContent, true);
  }

  @Test
  public void load() {
    ExtensionModel result = generator.deserialize(expectedContent);
    assertThat(result, is(extensionUnderTest));
  }
}
