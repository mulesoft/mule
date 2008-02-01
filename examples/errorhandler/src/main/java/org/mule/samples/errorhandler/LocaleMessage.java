/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler;

import org.mule.config.i18n.MessageFactory;
import org.mule.util.ObjectUtils;
import org.mule.util.StringMessageUtils;

/**
 * <code>LocaleMessage</code> is a convenience interface for retrieving
 * internationalised strings from resource bundles. The actual work is done by
 * the MessageFactory in core.
 */
public class LocaleMessage extends MessageFactory
{
    private static final String BUNDLE_PATH = "messages.errorhandler-example-messages";

    public static String unretrievedException(Exception e)
    {
        return getString(BUNDLE_PATH, 1, e);
    }

    public static String unhandledException(Class class1, Class class2)
    {
        return getString(BUNDLE_PATH, 2, StringMessageUtils.toString(class1), 
            StringMessageUtils.toString(class2));
    }

    public static String businessErrorManagerError()
    {
        return getString(BUNDLE_PATH, 3);
    }

    public static String errorDetail(String detailMessage)
    {
        return getString(BUNDLE_PATH, 4, detailMessage);
    }

    public static String errorClass(Class class1)
    {
        return getString(BUNDLE_PATH, 5, class1.getName());
    }

    public static String handlerFailure(ExceptionHandler eh)
    {
        String handlerDescription = ObjectUtils.toString(eh.getClass().getName(), "null");
        return getString(BUNDLE_PATH, 6, handlerDescription);
    }

    public static String defaultFatalHandling(Class class1)
    {
        return getString(BUNDLE_PATH, 7, StringMessageUtils.toString(class1));
    }

    public static String fatalHandling(Exception e)
    {
        return getString(BUNDLE_PATH, 8, e);
    }

    public static String defaultHandling(Class class1, ExceptionHandler eh, Exception e)
    {
        return getString(BUNDLE_PATH, 9, StringMessageUtils.toString(class1),
            ObjectUtils.toString(eh.getClass().getName() + " : " + e, "null"));
    }

    public static String defaultException(Exception e)
    {
        return getString(BUNDLE_PATH, 10, e);
    }

    public static String defaultHandlerException(HandlerException e)
    {
        return getString(BUNDLE_PATH, 11, e);
    }

    public static String fatalException(Throwable t)
    {
        return getString(BUNDLE_PATH, 12, t);
    }

    public static String businessHandlerMessage()
    {
        return getString(BUNDLE_PATH, 13);
    }

    public static String defaultHandlerMessage()
    {
        return getString(BUNDLE_PATH, 14);
    }

    public static String fatalHandlerMessage()
    {
        return getString(BUNDLE_PATH, 15);
    }

    public static String fatalHandlerException(Throwable t)
    {
        return getString(BUNDLE_PATH, 16, t);
    }
}
