/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.internal.config.processor;

import org.mule.runtime.module.db.internal.domain.query.QueryType;

import java.util.ArrayList;
import java.util.List;

public class DeleteProcessorBeanDefinitionParser extends AbstractUpdateProcessorBeanDefinitionParser {

  @Override
  protected List<QueryType> getQueryType() {
    List<QueryType> validQueryTypes = new ArrayList<QueryType>();
    validQueryTypes.add(QueryType.DELETE);
    validQueryTypes.add(QueryType.STORE_PROCEDURE_CALL);
    return validQueryTypes;
  }
}

