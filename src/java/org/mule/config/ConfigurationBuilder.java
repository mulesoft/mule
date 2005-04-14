/*
 * $Header$ 
 * $Revision$ 
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.config;

import org.mule.umo.manager.UMOManager;

/**
 * <code>ConfigurationBuilder</code> is a Strategy class used to configure a UMOManager
 * instance using different configuration mechanisms, such as for Xml, a script or some
 * other means.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface ConfigurationBuilder
{
    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     * @param configResources a comma separated list of configuration files to load,
     * this should be accessible on the classpath or filesystem
     * @return A configured UMOManager
     * @throws ConfigurationException
     */
    public UMOManager configure(String configResources) throws ConfigurationException;

    /**
     * Will configure a UMOManager based on the configurations made available through Readers
     * @param configResources an array of Readers
     * @return A configured UMOManager
     * @throws ConfigurationException
     */
    UMOManager configure(ReaderResource[] configResources) throws ConfigurationException;

    boolean isConfigured();
}