/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class SchedulerMessages extends MessageFactory
{

    private static final SchedulerMessages factory = new SchedulerMessages();

    private static final String BUNDLE_PATH = getBundlePath("schedulers");

    public static Message couldNotCreateScheduler()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message invalidCronExpression()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }

    public static Message couldNotScheduleJob()
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }

    public static Message couldNotPauseSchedulers()
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }

    public static Message couldNotShutdownScheduler()
    {
        return factory.createMessage(BUNDLE_PATH, 5);
    }
}
