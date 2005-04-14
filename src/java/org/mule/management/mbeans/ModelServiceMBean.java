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

    public void start() throws UMOException;

    public void stop() throws UMOException;

    public boolean isComponentRegistered(String name);

    public UMODescriptor getComponentDescriptor(String name);

    public void startComponent(String name) throws UMOException;

    public void stopComponent(String name) throws UMOException;

    public void pauseComponent(String name) throws UMOException;

    public void resumeComponent(String name) throws UMOException;

    public void unregisterComponent(String name) throws UMOException;

    public String getName();
}
