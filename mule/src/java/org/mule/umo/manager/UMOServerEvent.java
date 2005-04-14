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

/**
 * <code>UMOServerEvent</code> is an event triggered by something happening
 * in the Server itself such as the server starting or a component being registered
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
    public static final int ADMIN_EVENT_ACTION_START_RANGE = 500;
    public static final int CUSTOM_EVENT_ACTION_START_RANGE = 100000;


    public static final int NULL_ACTION = 0;
    public static final Object NULL_MESSAGE = new Object();

    protected int action = NULL_ACTION;

    public UMOServerEvent(Object message, int action)
    {
        super((message==null ? NULL_MESSAGE : message));
        this.action = action;
    }

    public int getAction()
    {
        return action;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getClass().getName());
        buf.append(": ").append(getPayloadToString());
        buf.append(": Action=").append(getActionName(action));
        return buf.toString();
    }

    protected String getPayloadToString() {
        return source.toString();
    }

    protected abstract String getActionName(int action);
}
