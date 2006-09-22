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

import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;

/**
 * <code>ModelServiceMBean</code> JMX Service interface for the UMOModel
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface ModelServiceMBean
{

    void start() throws UMOException;

    void stop() throws UMOException;

    boolean isComponentRegistered(String name);

    UMODescriptor getComponentDescriptor(String name);

    void startComponent(String name) throws UMOException;

    void stopComponent(String name) throws UMOException;

    void pauseComponent(String name) throws UMOException;

    void resumeComponent(String name) throws UMOException;

    void unregisterComponent(String name) throws UMOException;

    String getName();

    String getType();
}
