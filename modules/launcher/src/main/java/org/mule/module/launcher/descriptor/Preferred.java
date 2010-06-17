/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.descriptor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Provides a means to give preference to an SPI implementation over others. Use either with
 * or without a weight, in the latter case the higher the value, the stronger the preference.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Preferred
{
    int weight() default -1;
}
