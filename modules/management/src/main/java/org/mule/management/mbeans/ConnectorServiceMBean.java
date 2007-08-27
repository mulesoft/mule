/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Initialisable;

import java.beans.ExceptionListener;

public interface ConnectorServiceMBean extends Initialisable
{

    boolean isStarted();

    boolean isDisposed();

    boolean isDisposing();

    String getName();

    String getProtocol();

    ExceptionListener getExceptionListener();

    void startConnector() throws UMOException;

    void stopConnector() throws UMOException;

    void dispose();


}
