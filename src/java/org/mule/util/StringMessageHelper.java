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
 *
 */

package org.mule.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>StringMessageHelper</code> contains some useful methods for
 * formatting message strings for logging or exceptions.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class StringMessageHelper
{

    public static String getFormattedMessage(String msg, Object[] arguments)
    {
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = getObjectValue(arguments[i]);
            }
        }
        return MessageFormat.format(msg, arguments);
    }

    public static String getObjectValue(Object object)
    {
        if (object instanceof String) {
            return (String) object;
        } else if (object instanceof Class) {
            return ((Class) object).getName();
        } else if (object.toString().indexOf(String.valueOf(object.hashCode())) == -1) {
            return object.toString();
        } else {
            return object.getClass().getName();
        }
    }

    public static String getBoilerPlate(String message)
    {
        return getBoilerPlate(message, '*', 80);
    }

    public static String getBoilerPlate(String message, char c, int maxlength)
    {
        List list = new ArrayList();
        list.add(message);
        return getBoilerPlate(list, c, maxlength);
    }

    public static String getBoilerPlate(List messages, char c, int maxlength)
    {
        int size = 0;
        StringBuffer buf = new StringBuffer(messages.size() * maxlength);
        int trimLength = maxlength - 4;

        for (int i = 0; i < messages.size(); i++) {
            size = messages.get(i).toString().length();
            if (size > trimLength) {
                String temp = messages.get(i).toString();
                int k = i;
                int x;
                int len;
                messages.remove(i);
                while (temp.length() > 0) {
                    len=(trimLength < temp.length() ? trimLength : temp.length());
                    String msg = temp.substring(0, len);
                    x = msg.indexOf("\n");

                    if(x > -1) {
                        msg = msg.substring(0, x);
                        len = x+1;
                    } else {
                        x = msg.lastIndexOf(" ");
                         if(x > -1 && len == trimLength) {
                            msg = msg.substring(0, x);
                            len = x+1;
                        }
                    }
                    if(msg.startsWith(" ")) msg = msg.substring(1);

                    temp = temp.substring(len);
                    messages.add(k, msg);
                    k++;
                }
            }
        }
        buf.append("\n");
        buf.append(charString(c, maxlength));

        for (int i = 0; i < messages.size(); i++) {
            buf.append("\n");
            buf.append(c);
            buf.append(" ");
            buf.append(messages.get(i));
            int padding = trimLength - messages.get(i).toString().length();
            if (padding > 0) {
                buf.append(charString(' ', padding));
            }
            buf.append(" ");
            buf.append(c);
        }
        buf.append("\n");
        buf.append(charString(c, maxlength));
        return buf.toString();
    }

    public static String charString(char c, int len)
    {
        StringBuffer buf = new StringBuffer(len);
        for (int i = 0; i < len; i++) {
            buf.append(c);
        }
        return buf.toString();
    }

    public static String truncate(String message, int length, boolean includeCount)
    {
        if (message == null) {
            return null;
        }
        if (message.length() <= length) {
            return message;
        }
        String result = message.substring(0, length) + "...";
        if (includeCount) {
            result += "[" + length + " of " + message.length() + "]";
        }
        return result;
    }
}
