/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LazyTransformedInputStreamTestCase extends AbstractMuleTestCase
{
    private static String message = "abcdefghij";
    private ByteArrayInputStream inputStream;
    private LazyTransformedInputStream transformedInputStream;
    private AddOneStreamTransformer simpleTransformer;

    @Before
    public void setUp() throws Exception
    {
        inputStream = new ByteArrayInputStream(message.getBytes());
        simpleTransformer = new AddOneStreamTransformer(inputStream);
    }

    @After
    public void tearDown() throws Exception
    {
        IOUtils.closeQuietly(inputStream);
    }

    @Test
    public void testTransformPerRequestPolicy() throws Exception
    {
        transformedInputStream = new LazyTransformedInputStream(new TransformPerRequestPolicy(),
            simpleTransformer);

        for (int i = 0; i < message.length(); i++)
        {
            int read = transformedInputStream.read();
            assertEquals(message.charAt(i) + 1, read);
            // only one byte more should be consumed at this point
            assertEquals(i + 1, simpleTransformer.bytesRead);
            Thread.sleep(50);
            assertEquals(i + 1, simpleTransformer.bytesRead);
        }
    }

    @Test
    public void testTransformPerRequestInChunksPolicy() throws Exception
    {
        int chunkSize = 4;
        LazyTransformedInputStream transformedInputStream = new LazyTransformedInputStream(
            new TransformPerRequestInChunksPolicy(chunkSize), simpleTransformer);

        for (int i = 0; i < message.length(); i++)
        {
            int read = transformedInputStream.read();
            assertEquals(message.charAt(i) + 1, read);
            // only one byte more should be consumed at this point
            int shouldBeTransformed = (int) Math.min(message.length(),
                Math.ceil((double) simpleTransformer.bytesRead / (double) chunkSize) * chunkSize);
            assertEquals(shouldBeTransformed, simpleTransformer.bytesRead);
            Thread.sleep(50);
            assertEquals(shouldBeTransformed, simpleTransformer.bytesRead);
        }
    }

    @Test
    public void testTransformContinuouslyPolicy() throws Exception
    {
        LazyTransformedInputStream transformedInputStream = new LazyTransformedInputStream(
            new TransformContinuouslyPolicy(), simpleTransformer);

        int i = 0;
        int read = transformedInputStream.read();
        Thread.sleep(100);
        // all input stream should be consumed at this point
        assertEquals(message.length(), simpleTransformer.bytesRead);
        do
        {
            assertEquals(message.charAt(i) + 1, read);
            read = transformedInputStream.read();
            i++;
        }
        while (i < message.length());
    }

    private class AddOneStreamTransformer implements StreamTransformer
    {

        private InputStream inputStream;
        private int bytesRead;
        private boolean finished;

        public AddOneStreamTransformer(InputStream inputStream)
        {
            this.inputStream = inputStream;
            this.bytesRead = 0;
            this.finished = false;
        }

        @Override
        public boolean write(OutputStream out, AtomicLong bytesRequested) throws Exception
        {

            while (!this.finished && this.bytesRead + 1 <= bytesRequested.get())
            {
                int byteRead = this.inputStream.read();
                if (byteRead == -1)
                {
                    finished = true;
                    out.write(-1);
                }
                else
                {
                    out.write(byteRead + 1);
                    this.bytesRead++;
                }
            }
            return finished;
        }

        @Override
        public void initialize(OutputStream out) throws Exception
        {
            // nothing to do here
        }
    }
}
