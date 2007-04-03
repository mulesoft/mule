/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple object for colecting and reporting error messages
 */
public class XslHelper
{
    private static List errors = new ArrayList();
    private static List warnings = new ArrayList();

    private static String currentModel;

    public static void reportError(String message)
    {
        //Lets not duplicate error messages
        if(!errors.contains(errors.size() + ". " + message))
        {
            errors.add((errors.size() + 1) + ". " + message);
        }
    }

    public static void reportWarning(String message)
    {
        //Lets not duplicate error messages
        if(!warnings.contains(warnings.size() + ". " + message))
        {
            warnings.add((warnings.size() + 1) + ". " + message);
        }
    }

    public static boolean hasErrorReport()
    {
        return errors.size() > 0;
    }

    public static boolean hasWarningReport()
    {
        return warnings.size() > 0;
    }

    public static String getErrorReport()
    {
        List messages = new ArrayList();
        messages.add("ERROS");
        messages.addAll(errors);
        return StringMessageUtils.getBoilerPlate(messages, '#', 80);
    }

    public static String getWarningReport()
    {
        List messages = new ArrayList();
        messages.add("WARNINGS");
        messages.addAll(warnings);
        return StringMessageUtils.getBoilerPlate(messages, '#', 80);
    }

    public static String getFullReport()
    {
        List messages = new ArrayList();
        if (hasErrorReport())
        {
            messages.add("ERRORS");
            messages.addAll(errors);
            messages.add(StringUtils.repeat('=', 76));
        }
        if (hasWarningReport())
        {
            messages.add("WARNINGS");
            messages.addAll(warnings);
        }
        if (messages.size() > 0)
        {
            return StringMessageUtils.getBoilerPlate(messages, '#', 80);
        }
        else
        {
            return null;
        }
    }

    public static void clearReport()
    {
        errors.clear();
        warnings.clear();
    }

    public static String getUniqueId()
    {
        return UUID.getUUID();
    }

    public static String concatId(String string)
    {
        return string + "#" + UUID.getUUID();
    }


    public static String getCurrentModel()
    {
        return currentModel;
    }

    public static void setCurrentModel(String model)
    {
        currentModel = model;
    }
   

    public static List getErrors()
    {
        return new ArrayList(errors);
    }

    public static List getWarnings()
    {
        return new ArrayList(warnings);
    }
}
