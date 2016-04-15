/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

import org.mule.module.db.internal.result.resultset.ResultSetHandler;
import org.mule.tck.size.SmallTest;

@SmallTest
public class StreamingStatementResultHandlerTestCase extends AbstractStatementResultHandlerTestCase
{

    @Override
    protected StatementResultHandler createStatementResultHandler(ResultSetHandler resultSetHandler)
    {
        return new StreamingStatementResultHandler(resultSetHandler);
    }

    @Override
    protected boolean mustCloseResources()
    {
        return false;
    }
}
