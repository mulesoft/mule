/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns data one byte at a time.  By default the data are a 4 byte integer, value 1, and
 * a single byte value -1.
 */
public class SlowInputStream extends InputStream
{

    public static final int EOF = -1;
    public static int PAYLOAD = 255;
    public static int[] CONTENTS = new int[]{0, 0, 0, 1, PAYLOAD};
    public static final int FULL_LENGTH = CONTENTS.length;
    private static final Log logger = LogFactory.getLog(SlowInputStream.class);

    private int[] contents;
    private int next = 0;
    private int mark = 0;

    public SlowInputStream()
    {
        this(CONTENTS);
    }

    public SlowInputStream(int[] contents)
    {
        this.contents = contents;
    }

    public SlowInputStream(byte[] bytes)
    {
        contents = new int[bytes.length];
        for (int i = 0; i < bytes.length; ++i)
        {
            contents[i] = bytes[i];
        }
    }

    public int available() throws IOException
    {
        int available = next < contents.length ? 1 : 0;
        logger.debug("available: " + available);
        return available;
    }

    public int read() throws IOException
    {
        int value = available() > 0 ? contents[next++] : EOF;
        logger.debug("read: " + value);
        return value;
    }

    public int read(byte b[], int off, int len) throws IOException
    {
        int value = read();
        if (value != EOF)
        {
            b[off] = (byte) value;
            return 1;
        }
        else
        {
            return EOF;
        }
    }

    public synchronized void reset() throws IOException
    {
        next = mark;
    }

    public synchronized void mark(int readlimit)
    {
        mark = next;
    }

    public boolean markSupported()
    {
        return true;
    }

}
