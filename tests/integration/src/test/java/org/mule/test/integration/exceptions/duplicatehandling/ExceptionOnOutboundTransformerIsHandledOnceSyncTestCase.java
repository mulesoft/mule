/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions.duplicatehandling;

public class ExceptionOnOutboundTransformerIsHandledOnceSyncTestCase extends
    AbstractExceptionOnOutboundTransformerIsHandledOnceTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/duplicatehandling/exception-on-outbound-transformer-is-handler-once-sync.xml";
    }
}


