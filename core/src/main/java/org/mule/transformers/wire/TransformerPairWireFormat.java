/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.wire;

import org.mule.MuleException;
import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public class TransformerPairWireFormat implements WireFormat
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected UMOTransformer inboundTransformer;
    protected UMOTransformer outboundTransformer;

    public Object read(InputStream in) throws UMOException
    {
        if (inboundTransformer == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("inboundTransformer").getMessage());
        }
        if (inboundTransformer.isSourceTypeSupported(InputStream.class))
        {
            return inboundTransformer.transform(in);
        }
        else
        {
            try
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(in, baos);
                return inboundTransformer.transform(baos.toByteArray());
            }
            catch (IOException e)
            {
                throw new MuleException(CoreMessages.failedToReadPayload(), e);
            }
        }
    }

    public void write(OutputStream out, Object o, String encoding) throws UMOException
    {
        if (outboundTransformer == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("outboundTransformer").getMessage());
        }
        try
        {
            Class returnClass = outboundTransformer.getReturnClass();
            if (returnClass == null)
            {
                logger.warn("No return class was set on transformer: " + outboundTransformer
                                + ". Attempting to work out how to treat the result transformation");

                Object result = outboundTransformer.transform(o);

                byte[] bytes;
                if (result instanceof byte[])
                {
                    bytes = (byte[]) result;
                }
                else
                {
                    bytes = result.toString().getBytes(encoding);
                }

                out.write(bytes);
            }
            else if (returnClass.equals(byte[].class))
            {
                byte[] b = (byte[]) outboundTransformer.transform(o);
                out.write(b);
            }
            else if (returnClass.equals(String.class))
            {
                String s = (String) outboundTransformer.transform(o);
                out.write(s.getBytes(encoding));
            }
            else
            {
                throw new TransformerException(CoreMessages.transformFailedFrom(o.getClass()));
            }
        }
        catch (IOException e)
        {
            throw new TransformerException(CoreMessages.transformFailedFrom(o.getClass()), e);
        }
    }

    public UMOTransformer getInboundTransformer()
    {
        return inboundTransformer;
    }

    public void setInboundTransformer(UMOTransformer inboundTransformer)
    {
        this.inboundTransformer = inboundTransformer;
    }

    public UMOTransformer getOutboundTransformer()
    {
        return outboundTransformer;
    }

    public void setOutboundTransformer(UMOTransformer outboundTransformer)
    {
        this.outboundTransformer = outboundTransformer;
    }

}
