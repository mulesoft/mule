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
package org.mule.util;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * A class for getting the OS environmet properies.  Not this goes out of process and should not be relied upon
 * to obtain critical information
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EnvironmentHelper {

    /**
     * Get the operating system environment properties, should work for Windows and Linux
     * @return Properties map or an empty properties map if there was an error
     */
    public Properties getEnvProperties(){
        Properties envProps = new Properties();
        FileInputStream in = null;
        File f = null;
        try {
            boolean unix = true;
            Process process = null;
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                process = Runtime.getRuntime().exec("cmd /c set>temp.env");
                unix=false;
            } else {
                process = Runtime.getRuntime().exec("export -p >temp.env");
            }
            process.waitFor();
            f = new File("temp.env");

            in = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = br.readLine()) != null) {
                int index = -1;
                if(line.startsWith("declare -")) {
                    line = line.substring(11);
                }

                if ((index = line.indexOf("=")) > -1) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    //remove quotes
                    if(unix && value.length() > 1) {
                        value = value.substring(1, value.length()-1);
                    }
                    envProps.setProperty(key, value);
                } else {
                    envProps.setProperty(line, StringUtils.EMPTY);
                }
            }

        } catch (Exception e) {
            //ignore
        } finally{
            try {
                if(in!=null) {
                    in.close();
                }
            } catch (IOException e) {}
            f.delete();
        }
        return envProps;
    }

}
