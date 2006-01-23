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

import org.mule.MuleManager;
import org.mule.config.MuleConfiguration;

/**
 * <code>MuleConfigurationService</code> exposes the MuleConfiguration
 * settings as a management service
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleConfigurationService implements MuleConfigurationServiceMBean
{
    private final MuleConfiguration muleConfiguration = MuleManager.getConfiguration();

    public boolean isSynchronous() {
        return muleConfiguration.isSynchronous();
    }

    public void setSynchronous(boolean synchronous) {
        muleConfiguration.setSynchronous(synchronous);
    }

    public int getSynchronousEventTimeout() {
        return muleConfiguration.getSynchronousEventTimeout();
    }

    public void setSynchronousEventTimeout(int synchronousEventTimeout) {
        muleConfiguration.setSynchronousEventTimeout(synchronousEventTimeout);
    }

    public boolean isRemoteSync() {
        return muleConfiguration.isRemoteSync();
    }

    public void setRemoteSync(boolean remoteSync) {
        muleConfiguration.setRemoteSync(remoteSync);
    }

    public boolean isRecoverableMode() {
        return muleConfiguration.isRecoverableMode();
    }

    public void setRecoverableMode(boolean recoverableMode) {
        muleConfiguration.setRecoverableMode(recoverableMode);
    }

    public String getWorkingDirectory() {
        return muleConfiguration.getWorkingDirectory();
    }

    public void setWorkingDirectory(String workingDirectory) {
        muleConfiguration.setWorkingDirectory(workingDirectory);
    }

    public String[] getConfigResources() {
        return muleConfiguration.getConfigResources();
    }

    public String getServerUrl() {
        return muleConfiguration.getServerUrl();
    }

    public void setServerUrl(String serverUrl) {
        muleConfiguration.setServerUrl(serverUrl);
    }

    public int getTransactionTimeout() {
        return muleConfiguration.getTransactionTimeout();
    }

    public void setTransactionTimeout(int transactionTimeout) {
        muleConfiguration.setTransactionTimeout(transactionTimeout);
    }

    public boolean isClientMode() {
        return muleConfiguration.isClientMode();
    }

    public void setClientMode(boolean clientMode) {
        muleConfiguration.setClientMode(clientMode);
    }

    public boolean isEmbedded() {
        return muleConfiguration.isEmbedded();
    }

    public void setEmbedded(boolean embedded) {
        muleConfiguration.setEmbedded(embedded);
    }

    public String getEncoding() {
        return muleConfiguration.getEncoding();
    }

    public void setEncoding(String encoding) {
        muleConfiguration.setEncoding(encoding);
    }

    public boolean isEnableMessageEvents() {
        return muleConfiguration.isEnableMessageEvents();
    }

    public void setEnableMessageEvents(boolean enableMessageEvents) {
        muleConfiguration.setEnableMessageEvents(enableMessageEvents);
    }
}
