/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom.transformers;

import org.mule.api.MuleEvent;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.transformer.AbstractDiscoverableTransformer;
import org.mule.transformer.types.DataTypeFactory;

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
        this.registerSourceType(DataTypeFactory.create(Base.class));
        setReturnDataType(DataTypeFactory.create(OutputHandler.class));
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
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
