/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.errorhandler;

import java.util.Iterator;

/**
 * <code>ExceptionHandler</code> TODO (document class)
 */
public interface ExceptionHandler
{
    public ErrorManager getErrorManager();

    public void setErrorManager(ErrorManager errorManager);

    public void onException(ErrorMessage message) throws HandlerException;

    public void registerException(Class<? extends Throwable> exceptionClass);

    public void unRegisterException(Class<? extends Throwable> exceptionClass);

    public Iterator<Class<? extends Throwable>> getRegisteredClasses();

    public boolean isRegisteredFor(Class<? extends Throwable> exceptionClass);
}
