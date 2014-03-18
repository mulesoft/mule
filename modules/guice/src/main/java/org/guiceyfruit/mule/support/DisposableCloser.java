/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.guiceyfruit.mule.support;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;

import org.guiceyfruit.support.Closer;

/**
 * Disposes objects that implement {@link org.mule.api.lifecycle.Disposable} in the current scope when the scope closes
 */
public class DisposableCloser implements Closer
{
    public void close(Object object) throws Throwable
    {
        if (object instanceof Disposable && !(object instanceof MuleContext))
        {
            Disposable disposable = (Disposable) object;
            disposable.dispose();
        }
    }
}
