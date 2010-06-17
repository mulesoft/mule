/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;


public interface ApplicationDescriptor
{
    String getEncoding();

    /**
     * Config builder name. If the name not found among available builder shortcuts
     * (e.g. 'spring' for default xml-based Mule config), then a FQN of the class to
     * use.
     * @return null for defaults
     */
    String getConfigurationBuilder();

    String getDomainName();

    /**
     * Default (true) mode is a regular java classloading policy. When inverted (false),
     * application libraries will be consulted before any other locations.
     * @return default is true
     */
    boolean isParentFirstClassLoader();

    /**
     * @return an internal version number (not tied to Mule version directly)
     */
    int getDescriptorVersion();
}
