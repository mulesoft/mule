/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.transformers;

import org.mule.api.MuleEvent;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

/**
 * Adds support for converting a {@link javax.servlet.http.HttpServletRequest} into an {@link org.mule.api.transport.OutputHandler}
 */
public class ServletRequestToOutputHandler extends AbstractTransformer implements DiscoverableTransformer
{
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public ServletRequestToOutputHandler()
    {
        registerSourceType(DataTypeFactory.create(HttpServletRequest.class));
        setReturnDataType(DataTypeFactory.create(OutputHandler.class));
    }

    @Override
    public Object doTransform(final Object src, String encoding) throws TransformerException
    {
            return new OutputHandler()
            {
                public void write(MuleEvent event, OutputStream out) throws IOException
                {
                    InputStream is = ((HttpServletRequest) src).getInputStream();
                    try
                    {
                        IOUtils.copyLarge(is, out);
                    }
                    finally
                    {
                        is.close();
                    }
                }
            };
        }

    /**
     * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
     *
     * @return the priority weighting for this transformer. This is a value between
     *         {@link #MIN_PRIORITY_WEIGHTING} and {@link #MAX_PRIORITY_WEIGHTING}.
     */
    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    /**
     * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
     *
     * @param weighting the priority weighting for this transformer. This is a value between
     *                  {@link #MIN_PRIORITY_WEIGHTING} and {@link #MAX_PRIORITY_WEIGHTING}.
     */
    public void setPriorityWeighting(int weighting)
    {
        priorityWeighting = weighting;
    }
}
