/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.HTML;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.api.metadata.MediaType.XML;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.junit.Test;

/**
 * Ensures that some of the defined mimeTypes are properly mapped. This test does not cover all the available mimeType mappings as
 * that will require to parse the mime.types file
 */
@SmallTest
public class FileExtensionContentTypeResolutionTestCase extends AbstractMuleTestCase {

  private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
  public static final MediaType DEFAULT_CONTENT_TYPE = BINARY;

  @Test
  public void resolvesFileMimeType() throws Exception {
    Map<String, MediaType> mimeTypes = new HashMap<>();
    mimeTypes.put("txt", TEXT);
    mimeTypes.put("json", JSON);
    mimeTypes.put("xml", XML);
    mimeTypes.put("html", HTML);
    mimeTypes.put("csv", MediaType.create("text", "csv"));

    for (String extension : mimeTypes.keySet()) {
      doFileMimeTypeTest(extension, mimeTypes.get(extension));
    }
  }

  @Test
  public void resolvesDefaultMimeType() throws Exception {
    doFileMimeTypeTest("xxxxxx", DEFAULT_CONTENT_TYPE);
  }

  private void doFileMimeTypeTest(String fileExtension, MediaType expectedMimeType) throws IOException {
    String filename = "test." + fileExtension;
    String mimeType = mimetypesFileTypeMap.getContentType(filename);

    assertThat(mimeType, equalTo(expectedMimeType.toString()));
  }
}
