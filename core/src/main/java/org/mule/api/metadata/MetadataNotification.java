/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.metadata;

import org.mule.context.notification.CustomNotification;

public class MetadataNotification extends CustomNotification
{

    private static final int METADATA_ACTION_BASE = (CUSTOM_EVENT_ACTION_START_RANGE + 4) * 5;
    private static int ACTION_INDEX = 0;

    public static final int DISPOSE_METADATA_CACHE = ++ACTION_INDEX + METADATA_ACTION_BASE;

    static
    {
        registerAction("Configuration instance is disposed, related metadata cache must be disposed", DISPOSE_METADATA_CACHE);
    }

    public MetadataNotification(int action)
    {
        super(null, action);
    }

    @Override
    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier
               + ", timestamp=" + timestamp + "}";
    }

}
