/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.internal;

import org.mule.api.transport.OutputHandler;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Wraps a file's content so that operations can be performed over it regardless of its type
 *
 * @since 4.0
 */
public final class FileContentWrapper
{

    private final Object content;

    /**
     * Creates a new instance
     *
     * @param content the content to be wrapped
     */
    public FileContentWrapper(Object content)
    {
        this.content = content;
    }

    /**
     * Accepts the given {@code visitor}
     *
     * @param visitor a {@link FileContentVisitor}
     * @throws Exception if the visitation failed
     */
    public void accept(FileContentVisitor visitor) throws Exception
    {
        if (content instanceof String)
        {
            visitor.visit((String) content);
        }
        else if (content instanceof InputStream)
        {
            visitor.visit((InputStream) content);
        }
        else if (content instanceof OutputHandler)
        {
            visitor.visit((OutputHandler) content);
        }
        else if (content instanceof String[])
        {
            visitor.visit((String[]) content);
        }
        else if (content instanceof Iterable)
        {
            visitor.visit((Iterable) content);
        }
        else if (content instanceof Iterator)
        {
            visitor.visit((Iterator) content);
        }
        else if (content instanceof byte[])
        {
            visitor.visit((byte[]) content);
        }
        else if (byte.class.isAssignableFrom(content.getClass()))
        {
            visitor.visit((byte) content);
        }
        else if (content instanceof Byte)
        {
            visitor.visit((Byte) content);
        }
        else
        {
            visitor.visit(content.toString());
        }
    }

}
