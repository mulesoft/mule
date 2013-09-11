/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.api.Closeable;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.util.StreamCloser;
import org.mule.api.util.StreamCloserService;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * Closes streams of different types by looking up available {@link StreamCloser}'s
 * from the Mule registry.
 */
public class DefaultStreamCloserService implements StreamCloserService
{

    private static final Logger log = LoggerFactory.getLogger(DefaultStreamCloserService.class);

    private MuleContext muleContext;
    private StreamCloser coreStreamTypesCloser = new CoreStreamTypesCloser();

    public void closeStream(Object stream)
    {
        try
        {
            if (coreStreamTypesCloser.canClose(stream.getClass()))
            {
                coreStreamTypesCloser.close(stream);
            }
            else
            {
                for (StreamCloser closer : muleContext.getRegistry().lookupObjects(StreamCloser.class))
                {
                    if (closer.canClose(stream.getClass()))
                    {
                        closer.close(stream);
                        return;
                    }
                }
                log.debug("Unable to find an StreamCloser for the stream type: " + stream.getClass()
                          + ", the stream: " + stream + " will not be closed.");
            }
        }
        catch (Exception e)
        {
            log.debug("Exception closing stream: " + stream, e);
        }

    }

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    static class CoreStreamTypesCloser implements StreamCloser
    {

        public boolean canClose(Class streamType)
        {
            return InputStream.class.isAssignableFrom(streamType)
                   || InputSource.class.isAssignableFrom(streamType)
                   || StreamSource.class.isAssignableFrom(streamType)
                   || Closeable.class.isAssignableFrom(streamType)
                   || (SAXSource.class.isAssignableFrom(streamType) && !streamType.getName().endsWith(
                       "StaxSource"));
        }

        public void close(Object stream) throws IOException
        {
            if (stream instanceof InputStream)
            {
                try
                {
                    ((InputStream) stream).close();
                }
                catch (IOException e)
                {
                    this.logCloseException(stream, e);
                }
            }
            else if (stream instanceof InputSource)
            {
                closeInputSourceStream((InputSource) stream);
            }
            else if (stream instanceof SAXSource)
            {
                closeInputSourceStream(((SAXSource) stream).getInputSource());
            }
            else if (stream instanceof StreamSource)
            {
                try
                {
                    ((StreamSource) stream).getInputStream().close();
                }
                catch (IOException e)
                {
                    this.logCloseException(stream, e);
                }
            }
            else if (stream instanceof Closeable)
            {
                try
                {
                    ((Closeable) stream).close();
                }
                catch (MuleException e)
                {
                    this.logCloseException(stream, e);
                }
            }
        }

        private void closeInputSourceStream(InputSource payload) throws IOException
        {
            if (payload.getByteStream() != null)
            {
                payload.getByteStream().close();
            }
            else if (payload.getCharacterStream() != null)
            {
                payload.getCharacterStream().close();
            }
        }

        private void logCloseException(Object stream, Throwable e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Exception was found trying to close resource of class "
                         + stream.getClass().getCanonicalName(), e);
            }
        }

    }

}
