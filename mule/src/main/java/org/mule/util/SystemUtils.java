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
 *
 */
package org.mule.util;

/**
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 */
public class SystemUtils extends org.apache.commons.lang.SystemUtils {

    public static boolean isSunJDK() {
        return org.apache.commons.lang.SystemUtils.JAVA_VM_VENDOR.toUpperCase().indexOf("SUN") != -1;
    }

    public static boolean isIbmJDK() {
        return org.apache.commons.lang.SystemUtils.JAVA_VM_VENDOR.toUpperCase().indexOf("IBM") != -1;
    }
}
