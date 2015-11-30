/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Provides utility methods to work with ZIP files
 */
public class ZipUtils
{

    private ZipUtils()
    {
    }

    /**
     * Describes a resource that can be compressed in a ZIP file
     */
    public static class ZipResource
    {

        private final File file;
        private final String alias;

        public ZipResource(File file)
        {
            this(file, null);
        }

        public ZipResource(File file, String alias)
        {
            this.file = file;
            this.alias = alias;
        }

        public String getName()
        {
            return StringUtils.isEmpty(alias) ? file.getName() : alias;
        }

        public File getFile()
        {
            return file;
        }
    }

    /**
     * Compress a set of resource files into a ZIP file
     *
     * @param targetFile file that will contain the zipped files
     * @param resources  resources to compress
     * @throws IOException in case of any error processing the files
     */
    public static void compress(File targetFile, ZipResource[] resources) throws IOException
    {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(targetFile)))
        {
            for (ZipResource zipResource : resources)
            {
                try (FileInputStream in = new FileInputStream(zipResource.getFile()))
                {
                    out.putNextEntry(new ZipEntry(zipResource.getName()));

                    byte[] buffer = new byte[1024];

                    int count;
                    while ((count = in.read(buffer)) > 0)
                    {
                        out.write(buffer, 0, count);
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new IOException("Error compressing file " + targetFile.getName(), e);
        }
    }
}
