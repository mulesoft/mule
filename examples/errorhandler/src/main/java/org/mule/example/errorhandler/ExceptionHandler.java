/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    public void registerException(Class exceptionClass);

    public void unRegisterException(Class exceptionClass);

    public Iterator getRegisteredClasses();

    public boolean isRegisteredFor(Class exceptionClass);

}
