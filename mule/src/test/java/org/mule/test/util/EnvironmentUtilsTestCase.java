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

package org.mule.test.util;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.EnvironmentUtils;

import java.util.Properties;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EnvironmentUtilsTestCase extends AbstractMuleTestCase
{

    public void testEnvironment() throws Exception
    {
        Properties p = EnvironmentUtils.getEnvironment();
        assertNotNull(p);
        p.list(System.out);
        assertFalse(p.isEmpty());
        assertNotNull(p.getProperty("JAVA_HOME"));
    }

}
