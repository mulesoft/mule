/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.param;

import static org.mule.extension.db.internal.domain.query.QueryType.STORE_PROCEDURE_CALL;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.DbTypeManager;

/**
 * Creates {@link ParamTypeResolver} for generic databases
 */
public class GenericParamTypeResolverFactory implements ParamTypeResolverFactory {

  private final DbTypeManager dbTypeManager;

  public GenericParamTypeResolverFactory(DbTypeManager dbTypeManager) {
    this.dbTypeManager = dbTypeManager;
  }

  public ParamTypeResolver create(QueryTemplate queryTemplate) {
    ParamTypeResolver metadataParamTypeResolver;

    if (queryTemplate.getType() == STORE_PROCEDURE_CALL) {
      metadataParamTypeResolver = new StoredProcedureParamTypeResolver(dbTypeManager);
    } else {
      metadataParamTypeResolver = new QueryParamTypeResolver(dbTypeManager);
    }

    return new DefaultParamTypeResolver(dbTypeManager, metadataParamTypeResolver);
  }
}
