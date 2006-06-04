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
package org.mule.test.integration.providers.file;

import org.mule.tck.FunctionalTestCase;

import java.io.File;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class FileFunctionalTestCase extends FunctionalTestCase
{
    protected String getConfigResources() {
        return "org/mule/test/integration/providers/file/file-config.xml";
    }

    public void testRelative() throws Exception
    {
        File f = new File("./test/toto.txt");
        f.createNewFile();
        Thread.sleep(1000);
    }

}
