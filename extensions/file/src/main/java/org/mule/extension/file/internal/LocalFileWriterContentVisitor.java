/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import static org.mule.extension.file.internal.LocalFileSystem.exception;
import org.mule.api.MuleEvent;
import org.mule.api.transport.OutputHandler;
import org.mule.module.extension.file.internal.FileContentVisitor;
import org.mule.module.extension.file.internal.FileContentWrapper;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * {@link FileContentVisitor} which writes the visited
 * contents into an {@link #outputStream}.
 *
 * @since 4.0
 */
final class LocalFileWriterContentVisitor implements FileContentVisitor
{

    private final OutputStream outputStream;
    private final MuleEvent event;

    /**
     * Creates a new instance
     *
     * @param outputStream the stream on which the content is to be written
     * @param event        the {@link MuleEvent} which triggers the write operation
     */
    public LocalFileWriterContentVisitor(OutputStream outputStream, MuleEvent event)
    {
        this.outputStream = outputStream;
        this.event = event;
    }

    @Override
    public void visit(String content) throws Exception
    {
        IOUtils.write(content, outputStream);
    }

    @Override
    public void visit(byte content) throws Exception
    {
        outputStream.write(content);
    }

    @Override
    public void visit(byte[] content) throws Exception
    {
        IOUtils.write(content, outputStream);
    }

    @Override
    public void visit(OutputHandler handler) throws Exception
    {
        handler.write(event, outputStream);
    }

    @Override
    public void visit(String[] content) throws Exception
    {
        for (String line : content)
        {
            IOUtils.write(line, outputStream);
        }
    }

    @Override
    public void visit(InputStream content) throws Exception
    {
        IOUtils.copy(content, outputStream);
    }

    @Override
    public void visit(Iterable<?> content) throws Exception
    {
        visit(content.iterator());
    }

    @Override
    public void visit(Iterator<?> content) throws Exception
    {
        content.forEachRemaining(item -> {
            if (item == null)
            {
                throw new IllegalArgumentException("Cannot write a null value into a file");
            }
            try
            {
                new FileContentWrapper(item).accept(LocalFileWriterContentVisitor.this);
            }
            catch (Exception e)
            {
                throw exception("Could not write item of type " + item.getClass().getName(), e);
            }
        });
    }
}