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

import org.mule.MuleRuntimeException;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessageConstants;
import org.mule.config.i18n.Message;

import java.io.UnsupportedEncodingException;
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
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String DEFAULT_OS_ENCODING = System.getProperty("file.encoding");

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
        int size;
        StringBuffer buf = new StringBuffer(messages.size() * maxlength);
        int trimLength = maxlength - ( c == ' ' ? 2 : 4);

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
                    x = msg.indexOf(Utility.CRLF);

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
        buf.append(Utility.CRLF);
        if(c != ' ' ) buf.append(charString(c, maxlength));

        for (int i = 0; i < messages.size(); i++) {
            buf.append(Utility.CRLF);
             if(c != ' ' ) buf.append(c);
            buf.append(" ");
            buf.append(messages.get(i));

            int padding;
            try {
                padding = trimLength - messages.get(i).toString().getBytes(getOSEncoding()).length;
            } catch (UnsupportedEncodingException ueex) {
                throw new MuleRuntimeException(
                        new Message(CoreMessageConstants.FAILED_TO_CONVERT_STRING_USING_X_ENCODING, getOSEncoding()));
            }
            if (padding > 0) {
                buf.append(charString(' ', padding));
            }
            buf.append(" ");
             if(c != ' ' ) buf.append(c);
        }
        buf.append(Utility.CRLF);
         if(c != ' ' ) buf.append(charString(c, maxlength));
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

    public static byte[] getBytes(String string) {
        try {
            return string.getBytes(getEncoding());
        } catch (UnsupportedEncodingException e) {
            //We can ignore this as the encoding is validated on start up
            return null;
        }
    }

    public static String getString(byte[] bytes) {
        try {
            return new String(bytes, getEncoding());
        } catch (UnsupportedEncodingException e) {
            //We can ignore this as the encoding is validated on start up
            return null;
        }
    }

    public static String getString(byte[] bytes,String encoding) {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            //We can ignore this as the encoding is validated on start up
            return null;
        }
    }
    
    private static String getEncoding() {
        //Note that the org.mule.encoding property will not be set by Mule until the MuleManager.initialise
        //method is called, thus if you need to set an encoding other than UTF-8 before the Manager is invoked,
        //you can set this property on the JVM
        return System.getProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY, DEFAULT_ENCODING);
    }
    private static String getOSEncoding() {
        //Note that the org.mule.encoding property will not be set by Mule until the MuleManager.initialise
        //method is called, thus if you need to set an encoding other than UTF-8 before the Manager is invoked,
        //you can set this property on the JVM
        return System.getProperty(MuleProperties.MULE_OS_ENCODING_SYSTEM_PROPERTY, DEFAULT_OS_ENCODING);
    }
}
