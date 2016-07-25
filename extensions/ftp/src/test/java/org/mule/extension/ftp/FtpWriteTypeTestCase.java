/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.FtpTestHarness.HELLO_WORLD;
import org.mule.extension.FtpTestHarness;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.extension.file.api.FileWriteMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class FtpWriteTypeTestCase extends FtpConnectorTestCase
{

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                {"Ftp - String", new ClassicFtpTestHarness(), HELLO_WORLD, HELLO_WORLD},
                {"Ftp - native byte", new ClassicFtpTestHarness(), "A".getBytes()[0], "A"},
                {"Ftp - Object byte", new ClassicFtpTestHarness(), new Byte("A".getBytes()[0]), "A"},
                {"Ftp - OutputHandler", new ClassicFtpTestHarness(), new TestOutputHandler(), HELLO_WORLD},
                {"Ftp - InputStream", new ClassicFtpTestHarness(), new ByteArrayInputStream(HELLO_WORLD.getBytes()), HELLO_WORLD},

                {"Sftp - String", new SftpTestHarness(), HELLO_WORLD, HELLO_WORLD},
                {"Sftp - native byte", new SftpTestHarness(), "A".getBytes()[0], "A"},
                {"Sftp - Object byte", new SftpTestHarness(), new Byte("A".getBytes()[0]), "A"},
                {"Sftp - byte[]", new SftpTestHarness(), HELLO_WORLD.getBytes(), HELLO_WORLD},
                {"Sftp - OutputHandler", new SftpTestHarness(), new TestOutputHandler(), HELLO_WORLD},
                {"Sftp - InputStream", new SftpTestHarness(), new ByteArrayInputStream(HELLO_WORLD.getBytes()), HELLO_WORLD},
        });
    }

    private final Object content;
    private final String expected;
    private String path;

    public FtpWriteTypeTestCase(String name, FtpTestHarness testHarness, Object content, String expected)
    {
        super(name, testHarness);
        this.content = content;
        this.expected = expected;
    }

    @Override
    protected String getConfigFile()
    {
        return "ftp-write-config.xml";
    }


    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        final String folder = "test";
        testHarness.makeDir(folder);
        path = folder + "/test.txt";
    }

    @Test
    public void writeAndAssert() throws Exception
    {
        write(content);
        assertThat(readPathAsString(path), equalTo(expected));
    }

    private void write(Object content) throws Exception
    {
        doWrite(path, content, FileWriteMode.APPEND, false);
    }

    private static class TestOutputHandler implements OutputHandler
    {

        @Override
        public void write(MuleEvent event, OutputStream out) throws IOException
        {
            IOUtils.write(HELLO_WORLD, out);
        }
    }

    private static class HelloWorld
    {

        @Override
        public String toString()
        {
            return HELLO_WORLD;
        }
    }
}
