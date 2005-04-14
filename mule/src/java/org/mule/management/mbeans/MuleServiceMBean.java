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

import org.mule.umo.UMOException;

import java.util.Date;

/**
 * <code>MuleServiceMBean</code> is a JMX service interface for the UMOManager
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface MuleServiceMBean
{
    public boolean isInstanciated();

    public boolean isInitialised();

    public boolean isStopped();

    public Date getStartTime();

    public String getVersion();

    public String getVendor();

    public void start() throws UMOException;

    public void stop() throws UMOException;

    public void dispose() throws UMOException;

    boolean isSynchronous();

    void setSynchronous(boolean synchronous);

    int getSynchronousEventTimeout();

    void setSynchronousEventTimeout(int synchronousEventTimeout);

    boolean isSynchronousReceive();

    boolean isRecoverableMode();

    String getWorkingDirectoy();

    String[] getConfigResources();

    String getServerUrl();
}
