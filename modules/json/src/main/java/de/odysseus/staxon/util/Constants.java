/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package de.odysseus.staxon.util;

import static org.mule.api.config.MuleProperties.MULE_XML_JSON_MAX_DEPTH_SIZE;

public class Constants {

    public static final int MAX_DEPTH = Integer.parseInt(System.getProperty(MULE_XML_JSON_MAX_DEPTH_SIZE, "64"));;

}
