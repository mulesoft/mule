/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import javax.activation.MimetypesFileTypeMap;

/**
 * Resolves default MIME type for well known file extensions
 */
public class FileMimeTypeResolver
{

    private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    public static String resolveFileMimeType(String name)
    {
        return mimetypesFileTypeMap.getContentType(name.toLowerCase());
    }
}
