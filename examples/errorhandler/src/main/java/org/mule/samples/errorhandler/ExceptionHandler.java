/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler;

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
