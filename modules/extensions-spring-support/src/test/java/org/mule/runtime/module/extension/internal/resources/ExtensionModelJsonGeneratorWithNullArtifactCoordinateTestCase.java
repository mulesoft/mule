/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;

import static java.lang.Boolean.getBoolean;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.tck.size.SmallTest;
import org.mule.test.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Collection;

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

  @Parameterized.Parameters(name = "{2}")
  public static Collection<Object[]> data() {
    return singletonList(new Object[] {JAVA_LOADER, VeganExtension.class, "vegan-without-artifact-coordinates.json",
        null, emptyList()});
  }

  @Parameterized.Parameter(1)
  public Class extensionClass;

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
    assertThat(result, is(doLoadExtension()));
  }

  @Override
  protected ExtensionModel doLoadExtension() {
    return loadExtension(extensionClass, loader, artifactCoordinates, dslResolvingContext);
  }
}
