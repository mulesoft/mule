/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema.persistence;

import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.connectivity.api.test.platform.ConnectivitySchemaTestUtils.getNetsuiteTokenAuthenticationSchema;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.module.extension.internal.connectivity.platform.schema.persistence.ConnectivitySchemaJsonSerializerTestCase.ConnectivitySchemaJsonSerializerTestUnit.newTestUnit;

import org.mule.runtime.connectivity.api.platform.schema.ConnectivitySchema;
import org.mule.runtime.connectivity.api.platform.schema.persistence.ConnectivitySchemaJsonSerializer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

@SmallTest
@RunWith(Parameterized.class)
public class ConnectivitySchemaJsonSerializerTestCase extends AbstractMuleTestCase {

  private ConnectivitySchemaJsonSerializer serializer = ConnectivitySchemaJsonSerializer.builder().build();

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "connectivitySchema.updateExpectedFilesOnError");

  @Parameterized.Parameter
  public ConnectivitySchema schemaObject;

  @Parameterized.Parameter(1)
  public String expectedSource;

  private String expectedJson;

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    List<ConnectivitySchemaJsonSerializerTestUnit> schemas =
        asList(newTestUnit(getNetsuiteTokenAuthenticationSchema(), "connectivity-test-schema-netsuite.json"));

    return schemas.stream().map(t -> t.toTestParams()).collect(toList());
  }

  /**
   * Utility to batch fix input files when severe model changes are introduced. Use carefully, not a mechanism to get away with
   * anything. First check why the generated json is different and make sure you're not introducing any bugs. This should NEVER be
   * committed as true
   *
   * @return whether or not the "expected" test files should be updated when comparison fails
   */
  private boolean shouldUpdateExpectedFilesOnError() {
    return UPDATE_EXPECTED_FILES_ON_ERROR;
  }

  @Before
  public void setup() throws IOException {
    expectedJson = getResourceAsString("connectivity/schema/" + expectedSource, getClass()).trim();
  }

  @Test
  public void serialize() throws Exception {
    final String json = serializer.serialize(schemaObject).trim();
    try {
      JSONAssert.assertEquals(expectedJson, json, true);
    } catch (AssertionError e) {

      if (shouldUpdateExpectedFilesOnError()) {
        updateExpectedJson(json);
      } else {
        System.out.println(json);

        throw e;
      }
    }
  }

  @Test
  public void deserialize() {
    ConnectivitySchema result = serializer.deserialize(expectedJson);
    assertThat(result, is(schemaObject));
  }

  private void updateExpectedJson(String json) throws URISyntaxException, IOException {
    File root = new File(getResourceAsUrl("connectivity/schema/" + expectedSource, getClass()).toURI()).getParentFile()
        .getParentFile().getParentFile().getParentFile();
    File testDir = new File(root, "src/test/resources/connectivity/schema");
    File target = new File(testDir, expectedSource);
    stringToFile(target.getAbsolutePath(), json);

    System.out.println(expectedSource + " fixed");
  }

  static class ConnectivitySchemaJsonSerializerTestUnit {

    final ConnectivitySchema schema;
    final String fileName;

    public ConnectivitySchemaJsonSerializerTestUnit(ConnectivitySchema schema, String fileName) {
      this.schema = schema;
      this.fileName = fileName;
    }

    static ConnectivitySchemaJsonSerializerTestUnit newTestUnit(ConnectivitySchema schema, String fileName) {
      return new ConnectivitySchemaJsonSerializerTestUnit(schema, fileName);
    }

    ConnectivitySchema getSchema() {
      return schema;
    }

    String getFileName() {
      return fileName;
    }

    public Object[] toTestParams() {
      return new Object[] {schema, getFileName()};
    }
  }
}
