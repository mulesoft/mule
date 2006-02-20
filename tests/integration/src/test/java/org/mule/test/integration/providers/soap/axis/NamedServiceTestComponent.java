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
package org.mule.test.integration.providers.soap.axis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.RequestContext;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.test.integration.service.DateService;
import org.mule.umo.UMOEventContext;

import java.util.Date;

/**
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NamedServiceTestComponent extends FunctionalTestComponent implements org.mule.components.simple.EchoService,
        DateService
{
    private static transient Log logger = LogFactory.getLog(NamedServiceTestComponent.class);

    public String echo(String echo)
    {
        UMOEventContext context = RequestContext.getEventContext();
        //todo need to determine how test there the request params are named
        return echo;
    }

    public String getDate()
    {
        return new Date().toString();
    }
}
