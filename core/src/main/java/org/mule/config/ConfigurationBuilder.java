/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import java.util.Properties;

import org.mule.umo.manager.UMOManager;

/**
 * <code>ConfigurationBuilder</code> is a Strategy class used to configure a
 * UMOManager instance using different configuration mechanisms, such as for Xml, a
 * script or some other means.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface ConfigurationBuilder
{
    /**
     * Will configure a UMOManager based on the configurations made available through
     * Readers
     * 
     * @param configResources an array of Readers
     * @return A configured UMOManager
     * @throws ConfigurationException
     */
    UMOManager configure(ReaderResource[] configResources) throws ConfigurationException;

    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     * 
     * @param configResources a comma separated list of configuration files to load,
     *            this should be accessible on the classpath or filesystem
     * @return A configured UMOManager
     * @throws ConfigurationException
     */
    UMOManager configure(String configResources) throws ConfigurationException;

    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     * 
     * @param configResources - A comma-separated list of configuration files to
     *            load, these should be accessible on the classpath or filesystem
     * @param startupPropertiesFile - An optional file containing startup properties.
     *            This is useful for managing different environments (dev, test,
     *            production)
     * @return A configured UMOManager
     * @throws ConfigurationException
     */
    UMOManager configure(String configResources, String startupPropertiesFile) throws ConfigurationException;

    /**
     * Will configure a UMOManager based on the configurations made available through
     * Readers
     * 
     * @param configResources - An array of Readers, each Reader contains a portion
     *            of the Mule server configuration.
     * @param startupProperties - Optional properties to be set before configuring
     *            the Mule server. This is useful for managing different environments
     *            (dev, test, production)
     * @return A configured UMOManager
     * @throws ConfigurationException
     */
    UMOManager configure(ReaderResource[] configResources, Properties startupProperties)
        throws ConfigurationException;

    boolean isConfigured();
}
