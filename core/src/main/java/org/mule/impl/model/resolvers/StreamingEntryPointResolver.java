/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.resolvers;

import org.mule.impl.NoSatisfiableMethodsException;
import org.mule.impl.TooManySatisfiableMethodsException;
import org.mule.impl.model.streaming.DeferredOutputStream;
import org.mule.impl.model.streaming.StreamingService;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.umo.UMOEventContext;
import org.mule.umo.model.InvocationResult;
import org.mule.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Creates a {@link org.mule.impl.model.resolvers.StreamingEntryPoint}. For use with the Streaming Model.
 *
 * @see org.mule.impl.model.streaming.StreamingModel
 */
public class StreamingEntryPointResolver extends AbstractEntryPointResolver
{
    private Method streamingMethod;
    private boolean inAndOut = false;

    public void initialise(Object component) throws Exception
    {
        if (component instanceof StreamingService)
        {
            streamingMethod = StreamingService.class.getMethods()[0];
        }
        else
        {
            inAndOut = true;
            List methods = ClassUtils.getSatisfiableMethods(component.getClass(), new Class[]{
                    InputStream.class, OutputStream.class}, true, false, null);

            if (methods.size() == 0)
            {
                inAndOut = false;
                methods = ClassUtils.getSatisfiableMethods(component.getClass(),
                        new Class[]{InputStream.class}, true, false, null);
            }

            if (methods.size() == 0)
            {
                throw new NoSatisfiableMethodsException(component, new Class[]{InputStream.class});
            }
            else if (methods.size() > 1)
            {
                throw new TooManySatisfiableMethodsException(component, new Class[]{InputStream.class,
                        OutputStream.class});
            }
            else
            {
                streamingMethod = (Method) methods.get(0);
            }
        }
    }

    public InvocationResult invoke(Object component, UMOEventContext context) throws Exception
    {
        if (streamingMethod == null)
        {
            initialise(component);
        }

        StreamMessageAdapter adapter = (StreamMessageAdapter) context.getMessage().getAdapter();
        OutputStream out = new DeferredOutputStream(context);

        try
        {
            Object result;

            if (component instanceof StreamingService)
            {
                return invokeMethod(component, streamingMethod, new Object[]{adapter.getInputStream(), out, context});
            }
            else if (inAndOut)
            {
                return invokeMethod(component, streamingMethod, new Object[]{adapter.getInputStream(), out});
            }
            else
            {
                return invokeMethod(component, streamingMethod, new Object[]{adapter.getInputStream()});
            }
        }
        catch (Exception e)
        {
            logger.warn("Failed to route streaming event via " + component + ": " + e.getMessage(), e);
            throw e;
        }
        finally
        {
            try
            {
                out.flush();
            }
            catch (IOException e)
            {
                // ignore
                // TODO MULE-863: Why?
            }
        }
    }

}
