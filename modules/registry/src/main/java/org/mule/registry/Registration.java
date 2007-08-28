/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import java.util.HashMap;

/**
 * The Registration interface represents a component that is storable
 * in the registry.
 *
 * @version $Revision$
 */

public interface Registration {

    public static int STATE_NEW = 0;
    public static int STATE_INSTANTIATED = 1;
    public static int STATE_STARTING = 2;
    public static int STATE_STARTED = 3;
    public static int STATE_STOPPING = 4;
    public static int STATE_STOPPED = 5;
    public static int STATE_DISPOSED = 6;

    public String getType();

    public String getId();

    public String getParentId();

    public HashMap getProperties();

    public Object getProperty(String key);

    public int getState();

    public HashMap retrieveChildren();

    public Registration retrieveChild(String childId);

    public ComponentVersion getVersion();

    public void setType(String type);

    public void setId(String id);

    public void setParentId(String parentId);

    public void setProperties(HashMap properties);

    public void setProperty(String key, Object property);

    public void addChild(Registration component);

    public void setVersion(ComponentVersion version);

    /*
    public void register();

    public void deploy();

    public void undeploy();

    public void unregister();

    public void addChildReference(Registrant ref);
    */
}

