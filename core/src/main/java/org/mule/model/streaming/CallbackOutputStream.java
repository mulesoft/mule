/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.model.streaming;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CallbackOutputStream extends OutputStream
{
    private static final Log logger = LogFactory.getLog(CallbackOutputStream.class);

    public static interface Callback
    {
        public void onClose() throws Exception;
    }

    private OutputStream delegate;
    private Callback callback;

    public CallbackOutputStream(OutputStream delegate, Callback callback)
    {
        this.delegate = delegate;
        this.callback = callback;
    }

    @Override
    public void write(int b) throws IOException
    {
        delegate.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException
    {
        delegate.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException
    {
        delegate.write(b, off, len);
    }

    @Override
    public void close() throws IOException
    {
        try
        {
            delegate.close();
        }
        finally
        {
            closeCallback();
        }
    }

    private void closeCallback()
    {
        if (null != callback)
        {
            try
            {
                callback.onClose();
            }
            catch(Exception e)
            {
                logger.debug("Suppressing exception while releasing resources: " + e.getMessage());
            }
        }

    }
}
