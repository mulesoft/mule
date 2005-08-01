package org.mule.management.mbeans;

import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

import java.beans.ExceptionListener;

/**
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
