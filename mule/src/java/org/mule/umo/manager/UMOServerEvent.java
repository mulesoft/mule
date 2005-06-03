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
package org.mule.umo.manager;

import java.util.EventObject;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.util.ClassHelper;

/**
 * <code>UMOServerEvent</code> is an event triggered by something happening in
 * the Server itself such as the server starting or a component being registered
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class UMOServerEvent extends EventObject
{
    public static final int MANAGER_EVENT_ACTION_START_RANGE = 100;
    public static final int MODEL_EVENT_ACTION_START_RANGE = 200;
    public static final int COMPONENT_EVENT_ACTION_START_RANGE = 300;
    public static final int SECURITY_EVENT_ACTION_START_RANGE = 400;
    public static final int MANAGEMENT_EVENT_ACTION_START_RANGE = 500;
    public static final int ADMIN_EVENT_ACTION_START_RANGE = 600;
    public static final int CONNECTION_EVENT_ACTION_START_RANGE = 700;
    public static final int CUSTOM_EVENT_ACTION_START_RANGE = 100000;

    public static final int NULL_ACTION = 0;
    public static final Object NULL_MESSAGE = new Object();

    protected int action = NULL_ACTION;

    /**
     * The resourceIdentifier is used when firing inbound server events such as
     * Admin events or other action events triggered by an external source Used
     * to associate the event with a particular resource. For example, if the
     * event was a ComponentEvent the resourceIdentifier could be the name of a
     * particular component
     */
    protected String resourceIdentifier = null;

    public UMOServerEvent(Object message, int action)
    {
        super((message == null ? NULL_MESSAGE : message));
        this.action = action;
    }

    public UMOServerEvent(Object message, int action, String resourceIdentifier)
    {
        super((message == null ? NULL_MESSAGE : message));
        this.action = action;
        this.resourceIdentifier = resourceIdentifier;
    }

    public int getAction()
    {
        return action;
    }

    public String getResourceIdentifier()
    {
        return resourceIdentifier;
    }

    public boolean isResourceIdentifierAnUri()
    {
        return MuleEndpointURI.isMuleUri(resourceIdentifier);
    }

    public String toString()
    {
        return ClassHelper.getClassName(getClass()) + "{" + "action=" + getActionName(action)
                + ", resourceIdentifier='" + resourceIdentifier + "'" + "}";
    }

    protected String getPayloadToString()
    {
        return source.toString();
    }

    protected abstract String getActionName(int action);
}
