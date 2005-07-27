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
package org.mule.jbi.routing;

import junit.framework.TestCase;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.config.MuleXmlJbiContainerBuilder;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class InboundRouterFunctionalTestCase extends TestCase
{
    public void testX() throws Exception {
        MuleXmlJbiContainerBuilder builder = new MuleXmlJbiContainerBuilder();
        JbiContainer c = builder.configure("mule-jbi-inbound-router.xml");
        Thread.sleep(600000);
    }
}
