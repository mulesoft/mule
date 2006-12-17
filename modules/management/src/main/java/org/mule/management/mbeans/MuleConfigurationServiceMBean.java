/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

/**
 * <code>MuleConfigurationServiceMBean</code> is a JMx service interface for the
 * Mule server configuration
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface MuleConfigurationServiceMBean
{
    public boolean isSynchronous();

    public void setSynchronous(boolean synchronous);

    public int getSynchronousEventTimeout();

    public void setSynchronousEventTimeout(int synchronousEventTimeout);

    public boolean isRemoteSync();

    public void setRemoteSync(boolean remoteSync);

    public String getWorkingDirectory();

    public void setWorkingDirectory(String workingDirectory);

    public String[] getConfigResources();

    public int getTransactionTimeout();

    public void setTransactionTimeout(int transactionTimeout);

    public String getEncoding();

    public void setEncoding(String encoding);

    public String getOSEncoding();

    public void setOSEncoding(String encoding);

}
