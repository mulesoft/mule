/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import org.mule.extension.file.api.FileConnector;
import org.mule.extension.file.api.LocalFileAttributes;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.extension.file.api.FileWriteMode;
import org.mule.runtime.module.extension.file.api.stream.AbstractFileInputStream;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public abstract class FileConnectorTestCase extends ExtensionFunctionalTestCase
{

    protected static final String HELLO_WORLD = "Hello World!";
    protected static final String HELLO_FILE_NAME = "hello.json";
    protected static final String HELLO_PATH = "files/" + HELLO_FILE_NAME;
    protected static final String TEST_FILE_PATTERN = "test-file-%d.html";
    protected static final String SUB_DIRECTORY_NAME = "subDirectory";
    protected static final String CONTENT = "foo";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = none();

    @Rule
    public SystemProperty baseDir = new SystemProperty("baseDir", temporaryFolder.getRoot().getAbsolutePath());

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        if (!temporaryFolder.getRoot().exists())
        {
            temporaryFolder.getRoot().mkdir();
        }
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        temporaryFolder.delete();
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {FileConnector.class};
    }

    protected void assertExists(boolean exists, File... files)
    {
        for (File file : files)
        {
            assertThat(file.exists(), is(exists));
        }
    }

    protected MuleEvent readHelloWorld() throws Exception
    {
        return getPath(HELLO_PATH);
    }

    protected MuleMessage<AbstractFileInputStream, LocalFileAttributes> readPath(String path) throws Exception
    {
        return getPath(path).getMessage().asNewMessage();
    }

    protected MuleEvent getPath(String path) throws Exception
    {
        return flowRunner("read")
                .withFlowVariable("path", path)
                .run();
    }

    protected String readPathAsString(String path) throws Exception
    {
        return IOUtils.toString(readPath(path).getPayload());
    }

    protected void doWrite(String path, Object content, FileWriteMode mode, boolean createParent) throws Exception
    {
        doWrite("write", path, content, mode, createParent);
    }

    protected void doWrite(String flow, String path, Object content, FileWriteMode mode, boolean createParent) throws Exception {
        doWrite(flow, path, content, mode, createParent, null);
    }

    protected void doWrite(String flow, String path, Object content, FileWriteMode mode, boolean createParent, String encoding) throws Exception
    {
        flowRunner(flow)
                .withFlowVariable("path", path)
                .withFlowVariable("createParent", createParent)
                .withFlowVariable("mode", mode)
                .withFlowVariable("encoding", encoding)
                .withPayload(content)
                .run();
    }

    protected File createHelloWorldFile() throws IOException
    {
        File folder = temporaryFolder.newFolder("files");
        File hello = new File(folder, HELLO_FILE_NAME);
        FileUtils.write(hello, HELLO_WORLD);

        return hello;
    }

    protected void createTestFiles() throws Exception
    {
        createTestFiles(temporaryFolder.getRoot(), 0, 5);
        createSubDirectory();
    }

    protected void createSubDirectory() throws Exception
    {
        createTestFiles(temporaryFolder.newFolder(SUB_DIRECTORY_NAME), 5, 7);
    }

    protected void createTestFiles(File parentFolder, int startIndex, int endIndex) throws Exception
    {
        for (int i = startIndex; i < endIndex; i++)
        {
            String name = String.format(TEST_FILE_PATTERN, i);
            File file = new File(parentFolder, name);
            file.createNewFile();
            FileUtils.write(file, CONTENT);
        }
    }
}
