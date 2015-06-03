/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.MimeTypes;

import java.util.HashMap;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.junit.BeforeClass;
import org.junit.Test;

@SmallTest
/**
 * Ensures that some of the defined mimeTypes are properly mapped.
 * This test does not cover all the available mimeType mappings as that will
 * require to parse the mime.types file
 */
public class FileExtensionContentTypeResolutionTestCase extends AbstractMuleTestCase
{

    private static final Map<String, String> mimeTypes = new HashMap<>();
    private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    @BeforeClass
    public static void initMimeTypes()
    {
        mimeTypes.put("txt", MimeTypes.TEXT);
        mimeTypes.put("json", MimeTypes.JSON);
        mimeTypes.put("xml", MimeTypes.APPLICATION_XML);
        mimeTypes.put("html", MimeTypes.HTML);
    }

    @Test
    public void resolvesFileMimeType() throws Exception
    {
        for (String extension : mimeTypes.keySet())
        {
            doFileMimeTypeTest(extension, mimeTypes.get(extension));
        }
    }

    @Test
    public void resolvesDefaultMimeType() throws Exception
    {
        doFileMimeTypeTest("xxx", "application/octet-stream");
    }

    private void doFileMimeTypeTest(String fileExtension, String expectedMimeType)
    {
        final String fileName = "test." + fileExtension;

        final String mimeType = mimetypesFileTypeMap.getContentType(fileName);

        assertThat(mimeType, equalTo(expectedMimeType));
    }
}