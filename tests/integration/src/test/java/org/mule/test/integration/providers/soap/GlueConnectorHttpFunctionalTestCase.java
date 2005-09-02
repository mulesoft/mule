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
package org.mule.test.integration.providers.soap;

/**
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GlueConnectorHttpFunctionalTestCase extends AxisConnectorHttpFunctionalTestCase
{
    static public class ComponentWithoutInterfaces
    {
        public String echo(String msg)
        {
            return msg;
        }
    }

    protected String getProtocol() {
        return "glue";
    }

    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/glue-http-mule-config.xml";
    }
}
