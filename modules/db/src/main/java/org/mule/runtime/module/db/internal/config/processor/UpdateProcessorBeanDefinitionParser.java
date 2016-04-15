/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.processor;

import static org.mule.module.db.internal.domain.query.QueryType.MERGE;
import static org.mule.module.db.internal.domain.query.QueryType.STORE_PROCEDURE_CALL;
import static org.mule.module.db.internal.domain.query.QueryType.TRUNCATE;
import static org.mule.module.db.internal.domain.query.QueryType.UPDATE;
import org.mule.module.db.internal.domain.query.QueryType;

import java.util.Arrays;
import java.util.List;

public class UpdateProcessorBeanDefinitionParser extends AbstractUpdateProcessorBeanDefinitionParser
{

    @Override
    protected List<QueryType> getQueryType()
    {
        return Arrays.asList(UPDATE, STORE_PROCEDURE_CALL, TRUNCATE, MERGE);
    }
}
