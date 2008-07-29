/*
 * $Id: ProviderService.java 11549 2008-04-09 05:12:30Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.support;

import javax.xml.transform.Source;

public interface ProxyService
{

    Source invoke(Source arg0);

}
