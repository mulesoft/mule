/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
     * @return the new file
     */
    public static File createFolder(String name)
    {
        File result = FileUtils.newFile(name);
        result.delete();
        result.mkdir();
        result.deleteOnExit();

        return result;
    }
}
