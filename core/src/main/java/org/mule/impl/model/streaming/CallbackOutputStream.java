/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.streaming;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CallbackOutputStream extends OutputStream
{

    protected final Log logger = LogFactory.getLog(CallbackOutputStream.class);

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

    public void write(int b) throws IOException
    {
        delegate.write(b);
    }

    public void write(byte b[]) throws IOException
    {
        delegate.write(b);
    }

    public void write(byte b[], int off, int len) throws IOException
    {
        delegate.write(b, off, len);
    }

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
