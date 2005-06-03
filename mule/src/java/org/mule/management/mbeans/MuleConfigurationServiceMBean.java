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

import org.mule.config.QueueProfile;

/**
 * <code>MuleConfigurationServiceMBean</code> is a JMx service interface for
 * the Mule server configuration
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface MuleConfigurationServiceMBean
{

    // ThreadingProfile getMessageDispatcherThreadingProfile();

    // ThreadingProfile getMessageReceiverThreadingProfile();

    // ThreadingProfile getComponentThreadingProfile();

    // ThreadingProfile getDefaultThreadingProfile();

    // PoolingProfile getPoolingProfile();

    // QueueProfile getQueueProfile();

    boolean isSynchronous();

    void setSynchronous(boolean synchronous);

    int getSynchronousEventTimeout();

    void setSynchronousEventTimeout(int synchronousEventTimeout);

    boolean isSynchronousReceive();

    QueueProfile getQueueProfile();

    boolean isRecoverableMode();

    String getWorkingDirectory();

    String[] getConfigResources();

    String getServerUrl();
}
