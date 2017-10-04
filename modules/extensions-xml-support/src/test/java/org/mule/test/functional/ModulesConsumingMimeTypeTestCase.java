/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.InputStream;

public class ModulesConsumingMimeTypeTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  private static final String JSON_CONTENT_FILE = "{\n"
      + "  \"User\": {\n"
      + "    \"name\": \"somename\",\n"
      + "    \"kind\": \"somekind\",\n"
      + "    \"weight\": 100,\n"
      + "    \"email\": \"somename@domain.com\",\n"
      + "    \"userId\": \"somename-id\"\n"
      + "  }\n"
      + "}";
  private static final String JSON_FILENAME = "someJsonFile";
  private static final String JSON_FILENAME_WITH_EXTENSION = JSON_FILENAME + ".json";
  private static final String JSON_EXTRACTED_VALUE = "{\n  \"value\": \"somename\"\n}";

  private static final String XML_CONTENT_FILE = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<val:User xmlns:val=\"http://validationnamespace.raml.org\">\n"
      + "  <val:name>somename</val:name>\n"
      + "  <val:kind>somekind</val:kind>\n"
      + "  <val:weight>100</val:weight>\n"
      + "  <val:email>somename@domain.com</val:email>\n"
      + "  <val:userId>somename-id</val:userId>\n"
      + "</val:User>";
  private static final String XML_FILENAME = "someXmlFile";
  private static final String XML_FILENAME_WITH_EXTENSION = XML_FILENAME + ".xml";
  private static final String XML_EXTRACTED_VALUE = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<value>somename</value>";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty workingDir = new SystemProperty("workingDir", temporaryFolder.getRoot().getAbsolutePath());

  @BeforeClass
  public static void setUp() throws Exception {
    if (!temporaryFolder.getRoot().exists()) {
      temporaryFolder.getRoot().mkdir();
    }
    final File jsonFileWithExtension = temporaryFolder.newFile(JSON_FILENAME_WITH_EXTENSION);
    FileUtils.write(jsonFileWithExtension, JSON_CONTENT_FILE, Charsets.UTF_8);

    final File jsonFileWithoutExtension = temporaryFolder.newFile(JSON_FILENAME);
    FileUtils.write(jsonFileWithoutExtension, JSON_CONTENT_FILE, Charsets.UTF_8);

    final File xmlFileWithExtension = temporaryFolder.newFile(XML_FILENAME_WITH_EXTENSION);
    FileUtils.write(xmlFileWithExtension, XML_CONTENT_FILE, Charsets.UTF_8);

    final File xmlFileWithoutExtension = temporaryFolder.newFile(XML_FILENAME);
    FileUtils.write(xmlFileWithoutExtension, XML_CONTENT_FILE, Charsets.UTF_8);
  }

  @AfterClass
  public static void tearDown() {
    temporaryFolder.delete();
  }

  @Override
  protected String getModulePath() {
    return "modules/module-multiple-global-elements.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-modules-with-mimetype.xml";
  }

  @Test
  public void testReadJsonFile() throws Exception {
    assertFLowWithPayload("readFile", JSON_FILENAME_WITH_EXTENSION, JSON_CONTENT_FILE, JSON);
  }

  @Test
  public void readJsonFileAndExtractValueWithExpression() throws Exception {
    assertFLowWithPayload("readFileAndExtractValueWithExpression", JSON_FILENAME_WITH_EXTENSION, JSON_EXTRACTED_VALUE, JSON);
  }

  @Test
  public void testReadJsonFileForcingMimeType() throws Exception {
    assertFLowWithPayload("readFile", JSON_FILENAME, JSON_CONTENT_FILE, BINARY);
    assertFLowWithPayloadForcingMimetype("readFile", JSON_FILENAME, JSON_CONTENT_FILE, JSON, JSON.toRfcString());
  }

  @Test
  public void readJsonFileAndExtractValueWithExpressionForcingMimeType() throws Exception {
    try {
      getRunnerFor("readFileAndExtractValueWithExpression", JSON_FILENAME).run();
      fail("Should not have reached here, DW expression over the BINARY payload must fail (using file without extension, thus file connector won't set the right mimetype)");
    } catch (Exception e) {
      // Do nothing, DW expression over BINARY type will fail
    }
    assertFLowWithPayloadForcingMimetype("readFileAndExtractValueWithExpression", JSON_FILENAME, JSON_EXTRACTED_VALUE, JSON,
                                         JSON.toRfcString());
  }

  @Test
  public void testReadXmlFile() throws Exception {
    assertFLowWithPayload("readFile", XML_FILENAME_WITH_EXTENSION, XML_CONTENT_FILE, XML);
  }

  @Test
  public void readXmlFileAndExtractValueWithExpression() throws Exception {
    assertFLowWithPayload("readFileAndExtractValueWithExpression", XML_FILENAME_WITH_EXTENSION, XML_EXTRACTED_VALUE, XML);
  }

  @Test
  public void testReadXmlFileForcingMimeType() throws Exception {
    assertFLowWithPayload("readFile", XML_FILENAME, XML_CONTENT_FILE, BINARY);
    assertFLowWithPayloadForcingMimetype("readFile", XML_FILENAME, XML_CONTENT_FILE, XML, XML.toRfcString());
  }

  @Test
  public void readXmlFileAndExtractValueWithExpressionForcingMimeType() throws Exception {
    try {
      getRunnerFor("readFileAndExtractValueWithExpression", XML_FILENAME).run();
      fail("Should not have reached here, DW expression over the BINARY payload must fail (using file without extension, thus file connector won't set the right mimetype)");
    } catch (Exception e) {
      // Do nothing, DW expression over BINARY type will fail
    }
    assertFLowWithPayloadForcingMimetype("readFileAndExtractValueWithExpression", XML_FILENAME_WITH_EXTENSION,
                                         XML_EXTRACTED_VALUE, XML, XML.toRfcString());
  }

  private void assertFLowWithPayload(String flowName, String filename, String expectedContentFile, MediaType expectedMediaType)
      throws Exception {
    final CoreEvent coreEvent = getRunnerFor(flowName, filename)
        .run();
    final String actualContentFile = toString(coreEvent.getMessage().getPayload().getValue());
    assertThat(actualContentFile, is(expectedContentFile));
    assertThat(coreEvent.getMessage().getPayload().getDataType().getMediaType().getPrimaryType(),
               is(expectedMediaType.getPrimaryType()));
    assertThat(coreEvent.getMessage().getPayload().getDataType().getMediaType().getSubType(), is(expectedMediaType.getSubType()));
  }

  private void assertFLowWithPayloadForcingMimetype(String flowName, String filename, String expectedContentFile,
                                                    MediaType expectedMediaType, String forcedMimeType)
      throws Exception {
    final CoreEvent coreEvent = getRunnerFor(flowName, filename)
        .withVariable("shouldForceMimetype", true)
        .withVariable("forcedMimeType", forcedMimeType)
        .keepStreamsOpen()
        .run();
    final String actualContentFile = toString(coreEvent.getMessage().getPayload().getValue());
    assertThat(actualContentFile, is(expectedContentFile));
    assertThat(coreEvent.getMessage().getPayload().getDataType().getMediaType().getPrimaryType(),
               is(expectedMediaType.getPrimaryType()));
    assertThat(coreEvent.getMessage().getPayload().getDataType().getMediaType().getSubType(), is(expectedMediaType.getSubType()));
  }

  private FlowRunner getRunnerFor(String flowName, String filename) {
    return flowRunner(flowName)
        .withVariable("file", filename)
        .keepStreamsOpen();
  }

  private String toString(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof String) {
      return (String) value;
    }

    InputStream inputStream;
    if (value instanceof CursorStreamProvider) {
      inputStream = ((CursorStreamProvider) value).openCursor();
    } else if (value instanceof InputStream) {
      inputStream = (InputStream) value;
    } else {
      throw new IllegalArgumentException("Result was not of expected type");
    }

    try {
      return org.mule.runtime.core.api.util.IOUtils.toString(inputStream);
    } finally {
      closeQuietly(inputStream);
    }
  }

  //  private void compareXML(String expected, String actual) throws Exception {
  //    setNormalizeWhitespace(true);
  //    setIgnoreWhitespace(true);
  //    setIgnoreComments(true);
  //    setIgnoreAttributeOrder(false);
  //
  //    Diff diff = XMLUnit.compareXML(expected, actual);
  //    if (!(diff.similar() && diff.identical())) {
  //      DetailedDiff detDiff = new DetailedDiff(diff);
  //      @SuppressWarnings("rawtypes")
  //      List differences = detDiff.getAllDifferences();
  //      StringBuilder diffLines = new StringBuilder();
  //      for (Object object : differences) {
  //        Difference difference = (Difference) object;
  //        diffLines.append(difference.toString() + '\n');
  //      }
  //      throw new IllegalArgumentException("Actual XML differs from expected: \n" + diffLines.toString());
  //    }
  //  }
}
