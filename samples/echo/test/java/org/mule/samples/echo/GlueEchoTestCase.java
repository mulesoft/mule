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
package org.mule.samples.echo;


/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GlueEchoTestCase extends AxisEchoTestCase {


    protected String getConfigResources() {
        return "echo-glue-config.xml";
    }

    protected String getProtocol() {
        return "glue";
    }

     public void testPostEcho() throws Exception {
        //Glue doesn't use the Mule Http transport so cannot provide automatic transformations
    }

    public void testGetEcho() throws Exception {
        //Glue doesn't use the Mule Http transport so cannot provide automatic transformations        
    }
}
