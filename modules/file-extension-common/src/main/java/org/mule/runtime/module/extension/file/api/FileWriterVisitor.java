/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * A {@link FileContentVisitor} which writes the received
 * content into an {@link #outputStream}
 *
 * @since 4.0
 */
public class FileWriterVisitor implements FileContentVisitor
{

    private final OutputStream outputStream;
    private final MuleEvent event;

    /**
     * Creates a new instance
     *
     * @param outputStream the stream to write into
     * @param event        a {@link MuleEvent} to be used to power the {@link #visit(OutputHandler)} case
     */
    public FileWriterVisitor(OutputStream outputStream, MuleEvent event)
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
                new FileContentWrapper(item, event).accept(this);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(createStaticMessage("Could not write item of type " + item.getClass().getName()), e);
            }
        });
    }
}
