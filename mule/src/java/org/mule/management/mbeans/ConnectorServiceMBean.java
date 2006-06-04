/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.mbeans;

import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

import java.beans.ExceptionListener;

/**
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 * 
 * $Id$
 */
public interface ConnectorServiceMBean
{

    boolean isStarted();

    boolean isDisposed();

    boolean isDisposing();

    String getName();

    String getProtocol();

    ExceptionListener getExceptionListener();

    UMOMessageDispatcherFactory getDispatcherFactory();

    void startConnector()
            throws UMOException;

    void stopConnector()
            throws UMOException;

    void dispose();

    void initialise()
            throws InitialisationException, RecoverableException;
}
