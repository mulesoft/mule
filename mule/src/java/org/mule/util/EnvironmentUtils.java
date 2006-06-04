/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

/**
 * A class for getting the OS environmet properies. Note that this goes out of
 * process and should not be relied upon to obtain critical information
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EnvironmentUtils
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(EnvironmentUtils.class);

    private EnvironmentUtils()
    {
        // forbidden
    }

    /**
     * Get the operating system environment properties, should work for Windows
     * and Linux
     * 
     * @return Properties map or an empty properties map if there was an error
     */
    public static synchronized Properties getEnvironment()
    {
        Properties envProps = new Properties();

        try {
            if (SystemUtils.IS_JAVA_1_5) {
                // the following runaround is necessary since we still want to
                // compile on JDK 1.4
                Method envMethod = System.class.getMethod("getenv", ArrayUtils.EMPTY_CLASS_ARRAY);
                envProps.putAll((Map)envMethod.invoke(System.class, (Class[])null));
            }
            else {
                // fallback
                envProps = getEnvironmentJDK14();
            }
        }
        catch (Exception ex) {
            logger.error("Could not access OS environment.", ex);
        }

        return envProps;
    }

    private static Properties getEnvironmentJDK14() throws Exception
    {
        Properties envProps = new Properties();
        Process process = null;

        try {
            boolean isUnix = true;
            String command;

            if (SystemUtils.IS_OS_WINDOWS) {
                command = "cmd /c set";
                isUnix = false;
            }
            else {
                command = "export -p";
            }

            process = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
                // bash prepends: declare -x
                // zsh prepends: typeset -x
                if (line.startsWith("declare -") || line.startsWith("typeset -")) {
                    line = line.substring(11);
                }

                int index = -1;
                if ((index = line.indexOf('=')) > -1) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    // remove quotes, if any
                    if (isUnix && value.length() > 1 && (value.startsWith("\"") || value.startsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    envProps.setProperty(key, value);
                }
                else {
                    envProps.setProperty(line, StringUtils.EMPTY);
                }
            }
        }
        catch (Exception e) {
            throw e; // bubble up
        }
        finally {
            if (process != null) {
                process.destroy();
            }
        }

        return envProps;
    }

}
