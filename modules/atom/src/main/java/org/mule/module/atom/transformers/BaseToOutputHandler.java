/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.atom.transformers;

import org.mule.api.MuleEvent;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.transformer.AbstractDiscoverableTransformer;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.abdera.model.Base;
import org.apache.abdera.parser.stax.FOMWriterOptions;

/**
 * Converts Abdera model elements which extend {@link Base} to OutputHandlers.
 */
public class BaseToOutputHandler extends AbstractDiscoverableTransformer
{

    public BaseToOutputHandler()
    {
        this.registerSourceType(Base.class);
        setReturnClass(OutputHandler.class);
    }

    // @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            final Base e = (Base) src;

            return new OutputHandler()
            {
                public void write(MuleEvent event, OutputStream out) throws IOException
                {
                    FOMWriterOptions opts = new FOMWriterOptions();
                    opts.setCharset(event.getEncoding());
                    e.writeTo(out, opts);
                }
            };
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
