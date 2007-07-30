/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mule;

import org.mule.impl.InterceptorsInvoker;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.interceptors.LoggingInterceptor;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMODescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class InterceptorsTestCase extends AbstractMuleTestCase
{
    protected void doSetUp() throws Exception
    {
        // Make sure there is no current event
        RequestContext.safeSetEvent(null);
    }

    public void testSingleFilter() throws Exception
    {
        String data = "The quick brown fox jumped over the lazy dog";
        UMODescriptor descriptor = getTestDescriptor("orange", Orange.class.getName());
        List interceptors = new ArrayList();
        interceptors.add(new LoggingInterceptor());
        InterceptorsInvoker invoker = new InterceptorsInvoker(interceptors, descriptor, new MuleMessage(data));
        invoker.execute();
    }
}
