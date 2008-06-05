/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.support;

import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider
public class ProviderService implements Provider<Source>
{

    public Source invoke(Source arg0)
    {
        return null;
    }

}
