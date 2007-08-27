/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.oracle;

/**
 * Contains the settings used for testing against a live Oracle database.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public abstract class TestConfig {

    // The following queues are created and dropped automatically by the integration
    // tests.  You only need to change them if these names will create a conflict for
    // you.
    public static String QUEUE_RAW = "TEST_RAW";
    public static String QUEUE_TEXT = "TEST_TEXT";
    public static String QUEUE_TEXT2 = "TEST_TEXT2";
    public static String QUEUE_XML = "TEST_XML";

    public static String TEXT_MESSAGE = "This is a text message.";
    public static String XML_MESSAGE = "<msg attrib=\"attribute\">This is an XML message.</msg>";
    public static String XML_MESSAGE_FILE = "message.xml";
    public static String I18N_MESSAGE = "<msg>\u00E1\u00E9\u00ED\u00F3\u00FA</msg>";
}
