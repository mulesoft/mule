/*
 * $Header$
 * $Revision$
 * $Date$
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

import java.util.Date;

import org.mule.umo.UMOException;

/**
 * <code>MuleServiceMBean</code> is a JMX service interface for the UMOManager
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface MuleServiceMBean
{
    boolean isInstanciated();

    boolean isInitialised();

    boolean isStopped();

    Date getStartTime();

    String getVersion();

    String getVendor();

    void start() throws UMOException;

    void stop() throws UMOException;

    void dispose() throws UMOException;

    boolean isSynchronous();

    void setSynchronous(boolean synchronous);

    int getSynchronousEventTimeout();

    void setSynchronousEventTimeout(int synchronousEventTimeout);

    boolean isSynchronousReceive();

    boolean isRecoverableMode();

    String getWorkingDirectory();

    String[] getConfigResources();

    String getServerUrl();
}
