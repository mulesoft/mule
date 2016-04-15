/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.tck.size.SmallTest;

@SmallTest
public class SelectMessageProcessorDebugInfoTestCase extends AbstractParameterizedSingleQueryMessageProcessorDebugInfoTestCase
{

    @Override
    protected AbstractSingleQueryDbMessageProcessor createMessageProcessor()
    {
        return new SelectMessageProcessor(dbConfigResolver, queryResolver, null, null, false);
    }

    @Override
    protected String getSqlText()
    {
        return "SELECT * FROM PLANET WHERE NAME = ? AND POSITION = ?";
    }

    @Override
    protected QueryType getQueryType()
    {
        return QueryType.SELECT;
    }
}
