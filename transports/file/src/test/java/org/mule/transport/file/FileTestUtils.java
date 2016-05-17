/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.util.FileUtils;

import java.io.File;

public class FileTestUtils
{

    private FileTestUtils()
    {
    }

    /**
     * Creates a data file that will be deleted when the JVM finalizes using
     * the default encoding
     *
     * @param folder folder where the file will be created
     * @param testMessage content of the file
     * @return the new file
     * @throws Exception
     */
    public static File createDataFile(File folder, final String testMessage) throws Exception
    {
        return createDataFile(folder, testMessage, null);
    }

    /**
     * Creates a data file that will be deleted when the JVM finalizes
     *
     * @param folder folder where the file will be created
     * @param testMessage content of the file
     * @param encoding encoding used to store the data
     * @return the new file
     * @throws Exception
     */
    public static File createDataFile(File folder, final String testMessage, String encoding) throws Exception
    {
        File temp = File.createTempFile("mule-file-test-", ".txt");
        FileUtils.writeStringToFile(temp, testMessage, encoding);

        // Copies temp file to target
        File target = new File(folder, temp.getName());
        target.deleteOnExit();
        FileUtils.renameFile(temp, target);

        return target;
    }

    /**
     * Creates a folder that will be deleted when the JVM finalizes
     *
     * @param name name of the file
     * @return the new folder
     */
    public static File createFolder(String name)
    {
        return createFolder(null, name);
    }

    /**
     * Creates a folder that will be deleted when the JVM finalizes
     *
     * @param parent folder that contains the created folder
     * @param name name of the file
     * @return the new folder
     */
    public static File createFolder(File parent, String name)
    {
        File result = FileUtils.newFile(parent, name);
        result.delete();
        result.mkdir();
        result.deleteOnExit();

        return result;
    }
}
