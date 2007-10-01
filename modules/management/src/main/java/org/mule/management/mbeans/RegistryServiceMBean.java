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

/**
 * <code>RegistryServiceMBean</code> JMX Service interface for the Registry
 */
public interface RegistryServiceMBean
{

    void start() throws UMOException;

    void stop() throws UMOException;

    //String getPersistenceMode();

    String getName();

}
