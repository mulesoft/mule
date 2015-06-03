/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.transformer.types.MimeTypes;
import org.mule.util.FilenameUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves default MIME type for well known file extensions
 */
public class FileMimeTypeResolver
{

    public static Map<String, String> mimeTypes = createDefaultMimeTypeMapping();

    private FileMimeTypeResolver()
    {
    }

    private static HashMap<String, String> createDefaultMimeTypeMapping()
    {
        HashMap<String, String> mapping = new HashMap<>();
        mapping.put("txt", MimeTypes.TEXT);
        mapping.put("json", MimeTypes.JSON);
        mapping.put("xml", MimeTypes.XML);
        mapping.put("html", MimeTypes.HTML);

        return mapping;
    }

    public static String resolveFileMimeType(String name)
    {
        final String extension = FilenameUtils.getExtension(name);

        return mimeTypes.get(extension.toLowerCase());
    }
}
