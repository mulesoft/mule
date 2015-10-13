/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import static org.mule.module.db.internal.domain.query.QueryType.UPDATE;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.tck.size.SmallTest;

import java.util.Collections;

@SmallTest
public class UpdateMessageProcessorDebugInfoTestCase extends AbstractParameterizedSingleQueryMessageProcessorDebugInfoTestCase
{

    @Override
    protected AbstractSingleQueryDbMessageProcessor createMessageProcessor()
    {
        return new UpdateMessageProcessor(dbConfigResolver, queryResolver, null, null, Collections.singletonList(UPDATE));
    }

    @Override
    protected String getSqlText()
    {
        return "UPDATE PLANET SET NAME = 'Mercury' WHERE NAME = ? AND POSITION = ?";
    }

    @Override
    protected QueryType getQueryType()
    {
        return UPDATE;
    }
}