/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import org.mule.module.db.internal.resolver.param.StoredProcedureParamTypeResolver;
import org.mule.module.db.internal.resolver.param.StoredProcedureParamTypeResolverTestCase;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link ResultSet} representing stored procedure columns
 */
public class StoredProcedureColumnTypesBuilder
{

    private final ResultSetBuilder resultSetBuilder;

    public StoredProcedureColumnTypesBuilder()
    {
        List<ColumnMetadata> columns = new ArrayList<ColumnMetadata>();
        columns.add(new ColumnMetadata(StoredProcedureParamTypeResolverTestCase.TYPE_COLUMN, StoredProcedureParamTypeResolver.TYPE_ID_COLUMN_INDEX));
        columns.add(new ColumnMetadata(StoredProcedureParamTypeResolverTestCase.NAME_COLUMN, StoredProcedureParamTypeResolver.TYPE_NAME_COLUMN_INDEX));

        resultSetBuilder = new ResultSetBuilder(columns);
    }

    public StoredProcedureColumnTypesBuilder with(TypeMetadata type)
    {

        Map<String, Object> typeRecord = new HashMap<String, Object>();
        typeRecord.put(StoredProcedureParamTypeResolverTestCase.TYPE_COLUMN, type.getId());
        typeRecord.put(StoredProcedureParamTypeResolverTestCase.NAME_COLUMN, type.getName());

        resultSetBuilder.with(typeRecord);

        return this;
    }

    public ResultSet build()
    {
        return resultSetBuilder.build();
    }
}
