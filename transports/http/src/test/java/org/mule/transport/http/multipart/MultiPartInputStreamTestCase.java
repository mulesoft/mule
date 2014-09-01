/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.multipart;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for the {@link MultiPartInputStream}.
 */
public class MultiPartInputStreamTestCase extends AbstractMuleTestCase
{
    private static final String MULTIPART_BOUNDARY = "----------------------------299df9f9431b";
    private static final int NUMBER_OF_PARTS = 3;
    private static final String TMP_DIR = "./multipartTmpDir";

    private File tmpDir;
    private Set<String> partContents;
    private String multipartMessage;

    @Before
    public void setUp() throws IOException
    {
        // Create temp. dir. for the MultiPartInputStream to store intermediate part files.
        tmpDir = createTempDirectory(TMP_DIR);

        // Create part contents and build multipart message.
        partContents = new HashSet<String>();
        StringBuilder multipartMessageBuilder = new StringBuilder();
        for (int i = 0; i < NUMBER_OF_PARTS; i++)
        {
            String partContent = "part " + i;
            partContents.add(partContent);

            multipartMessageBuilder.append("--").append(MULTIPART_BOUNDARY).append("\r\n");
            multipartMessageBuilder.append("Content-Disposition: form-data; name=\"").append(partContent).
                    append("\"; filename=\"").append(partContent).append("\"\r\n");
            multipartMessageBuilder.append("Content-Type: application/octet-stream\r\n\r\n");
            multipartMessageBuilder.append(partContent).append("\r\n");
        }
        multipartMessage = multipartMessageBuilder.toString();
    }

    @After
    public void tearDown() throws IOException
    {
        FileUtils.deleteDirectory(tmpDir);
    }

    @Test
    public void buildMultiPartInputStream() throws Exception
    {
        buildMultiPartInputStream(multipartMessage);
    }

    @Test
    public void multiPartWithContentBeforeFirstBoundary() throws Exception
    {
        buildMultiPartInputStream("\nprologue\n" + multipartMessage);
    }

    private void buildMultiPartInputStream(String body) throws Exception
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(body.getBytes("UTF-8"));
        MultiPartInputStream mpis = new MultiPartInputStream(bis, "multipart/form-data; boundary=" + MULTIPART_BOUNDARY,
                                                             new MultipartConfiguration(TMP_DIR));

        Collection<Part> parts = mpis.getParts();
        assertEquals(NUMBER_OF_PARTS, parts.size());

        // Read parsed part contents and close input stream.
        Set<String> parsedPartContents = new HashSet<String>();
        for (Part part : parts)
        {
            InputStream pis = part.getInputStream();
            parsedPartContents.add(IOUtils.toString(pis));
            // No need to close the input stream as it is closed automatically when end of input has been reached.
        }

        // Assert that the part contents were all parsed correctly.
        assertEquals(partContents, parsedPartContents);

        // Assert that no temp files remain in place.
        assertTrue("Temporary directory should be empty", FileUtils.listFiles(tmpDir, null, false).isEmpty());
    }

    /**
     * Creates an empty temporary directory.
     *
     * @param tmpDirName The name of the temporary directory.
     * @return The created temporary directory.
     * @throws java.io.IOException If there is an error creating the file
     */
    private static File createTempDirectory(String tmpDirName) throws IOException
    {
        File tmpDir = FileUtils.openDirectory(tmpDirName);
        FileUtils.deleteTree(tmpDir);
        return tmpDir;
    }
}
