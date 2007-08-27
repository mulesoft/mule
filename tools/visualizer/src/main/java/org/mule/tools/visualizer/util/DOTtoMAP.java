/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.util;

import org.mule.tools.visualizer.config.GraphEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DOTtoMAP
{

    private static final int BUFFER_SIZE = 32768;
    private GraphEnvironment env = null;

    DOTtoMAP(GraphEnvironment env)
    {
        this.env = env;
    }

    public static void transform(String dotExeFileName,
                                 String dotFileName,
                                 String outFormat,
                                 OutputStream out,
                                 GraphEnvironment env) throws IOException
    {
        (new DOTtoMAP(env)).innerTransform(dotExeFileName, dotFileName, outFormat, out);
    }

    public static void transform(String dotExeFileName,
                                 String dotFileName,
                                 String outFileName,
                                 GraphEnvironment env) throws IOException
    {
        (new DOTtoMAP(env)).innerTransform(dotExeFileName, dotFileName, outFileName);
    }

    private String getFormatForFile(String outFileName)
    {
        int idx = outFileName.lastIndexOf(".");
        if (idx == -1 || idx == outFileName.length() - 1)
        {
            throw new IllegalArgumentException("Can't determine file name extention for file name "
                            + outFileName);
        }
        else
        {
            return outFileName.substring(idx + 1);
        }
    }

    private void innerTransform(String dotExeFileName, String dotFileName, String outFormat, OutputStream out)
        throws IOException
    {
        String exeCmd = dotExeFileName + " -T" + outFormat + " " + dotFileName;
        Process p = Runtime.getRuntime().exec(exeCmd);
        InputStream is = p.getInputStream();
        byte buf[] = new byte[BUFFER_SIZE];
        do
        {
            int len = is.read(buf);
            if (len > 0)
            {
                out.write(buf, 0, len);
            }
            else
            {
                is.close();
                return;
            }
        }
        while (true);
    }

    private void innerTransform(String dotExeFileName, String dotFileName, String outFileName)
        throws IOException
    {
        String exeCmd = dotExeFileName + " -T" + getFormatForFile(outFileName) + " " + dotFileName + " -o "
                        + outFileName;
        env.log(exeCmd);
        Process p = Runtime.getRuntime().exec(exeCmd);
        try
        {
            int i = p.waitFor();
            env.log("result code from process is: " + i);
        }
        catch (Exception ie)
        {
            env.logError("Warning: failed to wait for native process to exit...", ie);
        }
    }
}
