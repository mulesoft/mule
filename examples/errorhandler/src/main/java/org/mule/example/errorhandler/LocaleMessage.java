/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.errorhandler;

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
    private static final LocaleMessage factory = new LocaleMessage();
    
    private static final String BUNDLE_PATH = "messages.errorhandler-example-messages";

    public static String unretrievedException(Exception e)
    {
        return factory.getString(BUNDLE_PATH, 1, e);
    }

    public static String unhandledException(Class<?> class1, Class<?> class2)
    {
        return factory.getString(BUNDLE_PATH, 2, StringMessageUtils.toString(class1), 
            StringMessageUtils.toString(class2));
    }

    public static String businessErrorManagerError()
    {
        return factory.getString(BUNDLE_PATH, 3);
    }

    public static String errorDetail(String detailMessage)
    {
        return factory.getString(BUNDLE_PATH, 4, detailMessage);
    }

    public static String errorClass(Class<?> class1)
    {
        return factory.getString(BUNDLE_PATH, 5, class1.getName());
    }

    public static String handlerFailure(ExceptionHandler eh)
    {
        String handlerDescription = ObjectUtils.toString(eh.getClass().getName(), "null");
        return factory.getString(BUNDLE_PATH, 6, handlerDescription);
    }

    public static String defaultFatalHandling(Class<?> class1)
    {
        return factory.getString(BUNDLE_PATH, 7, StringMessageUtils.toString(class1));
    }

    public static String fatalHandling(Exception e)
    {
        return factory.getString(BUNDLE_PATH, 8, e);
    }

    public static String defaultHandling(Class<?> class1, ExceptionHandler eh, Exception e)
    {
        return factory.getString(BUNDLE_PATH, 9, StringMessageUtils.toString(class1),
            ObjectUtils.toString(eh.getClass().getName() + " : " + e, "null"));
    }

    public static String defaultException(Exception e)
    {
        return factory.getString(BUNDLE_PATH, 10, e);
    }

    public static String defaultHandlerException(HandlerException e)
    {
        return factory.getString(BUNDLE_PATH, 11, e);
    }

    public static String fatalException(Throwable t)
    {
        return factory.getString(BUNDLE_PATH, 12, t);
    }

    public static String businessHandlerMessage()
    {
        return factory.getString(BUNDLE_PATH, 13);
    }

    public static String defaultHandlerMessage()
    {
        return factory.getString(BUNDLE_PATH, 14);
    }

    public static String fatalHandlerMessage()
    {
        return factory.getString(BUNDLE_PATH, 15);
    }

    public static String fatalHandlerException(Throwable t)
    {
        return factory.getString(BUNDLE_PATH, 16, t);
    }

    @Override
    protected ClassLoader getClassLoader()
    {
        return getClass().getClassLoader();
    }
}
