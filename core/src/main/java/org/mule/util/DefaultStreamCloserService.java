/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.api.MuleContext;
import org.mule.api.util.StreamCloser;
import org.mule.api.util.StreamCloserService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

/**
 * Closes streams of different types by looking up available {@link StreamCloser}'s
 * from the Mule registry.
 */
public class DefaultStreamCloserService implements StreamCloserService
{

    private static final Log log = LogFactory.getLog(DefaultStreamCloserService.class);

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
                Iterator closers = muleContext.getRegistry().lookupObjects(StreamCloser.class).iterator();
                while (closers.hasNext())
                {
                    StreamCloser closer = (StreamCloser) closers.next();
                    if (closer.canClose(stream.getClass()))
                    {
                        closer.close(stream);
                    }
                    else
                    {
                        log.debug("Unable to find an StreamCloser for the stream type: " + stream.getClass()
                                  + ", the stream: " + stream + " will not be closed.");
                    }
                }
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
                    // no-op
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
        
    }

}
