/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.model.UMOModel;

/**
 * <code>ModelNotification</code> is fired when an event such as the model starting
 * occurs. The payload of this event will always be a reference to the model.
 * 
 * @see org.mule.umo.model.UMOModel
 */
public class ModelNotification extends UMOServerNotification implements BlockingServerEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1954880336427554435L;

    public static final int MODEL_INITIALISING = MODEL_EVENT_ACTION_START_RANGE + 1;
    public static final int MODEL_INITIALISED = MODEL_EVENT_ACTION_START_RANGE + 2;
    public static final int MODEL_INITIALISING_LISTENERS = MODEL_EVENT_ACTION_START_RANGE + 3;
    public static final int MODEL_INITIALISED_LISTENERS = MODEL_EVENT_ACTION_START_RANGE + 4;
    public static final int MODEL_STARTING = MODEL_EVENT_ACTION_START_RANGE + 5;
    public static final int MODEL_STARTED = MODEL_EVENT_ACTION_START_RANGE + 6;
    public static final int MODEL_STOPPING = MODEL_EVENT_ACTION_START_RANGE + 7;
    public static final int MODEL_STOPPED = MODEL_EVENT_ACTION_START_RANGE + 8;
    public static final int MODEL_DISPOSING = MODEL_EVENT_ACTION_START_RANGE + 9;
    public static final int MODEL_DISPOSED = MODEL_EVENT_ACTION_START_RANGE + 10;

    static {
        registerAction("initialising", MODEL_INITIALISING);
        registerAction("initialised", MODEL_INITIALISED);
        registerAction("initialising listener", MODEL_INITIALISING_LISTENERS);
        registerAction("initialised listener", MODEL_INITIALISED_LISTENERS);
        registerAction("starting", MODEL_STARTING);
        registerAction("started", MODEL_STARTED);
        registerAction("stopping", MODEL_STOPPING);
        registerAction("stopped", MODEL_STOPPED);
        registerAction("disposing", MODEL_DISPOSING);
        registerAction("disposed", MODEL_DISPOSED);
    }

    public ModelNotification(UMOModel model, int action)
    {
        super(model, action);
        resourceIdentifier = model.getName();
    }

    protected String getPayloadToString()
    {
        return ((UMOModel) source).getName();
    }
}
