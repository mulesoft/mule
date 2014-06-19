/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.param;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.domain.type.JdbcTypes;
import org.mule.module.db.internal.domain.type.UnknownDbType;
import org.mule.module.db.test.util.DatabaseMetaDataBuilder;
import org.mule.module.db.test.util.DbConnectionBuilder;
import org.mule.module.db.test.util.DbTypeManagerBuilder;
import org.mule.module.db.test.util.TestDbTypeMetadata;
import org.mule.module.db.test.util.StoredProcedureColumnTypesBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class StoredProcedureParamTypeResolverTestCase extends AbstractMuleTestCase
{

    public static final String TYPE_COLUMN = "type";
    public static final String NAME_COLUMN = "name";

    @Test
    public void resolvesStoredProcedureParamTypes() throws Exception
    {
        final String catalog = "test";

        ResultSet procedureColumns = new StoredProcedureColumnTypesBuilder().with(TestDbTypeMetadata.INTEGER_DB_TYPE_METADATA).build();
        DatabaseMetaData databaseMetaData = new DatabaseMetaDataBuilder().returningStoredProcedureColumns(catalog, "testStoredProcedure", procedureColumns).build();

        final String sqlText = "call testStoredProcedure(?)";

        DbConnection connection = new DbConnectionBuilder().onCalatog(catalog).with(databaseMetaData).build();

        QueryTemplate queryTemplate = new QueryTemplate(sqlText, QueryType.STORE_PROCEDURE_CALL, Collections.<org.mule.module.db.internal.domain.param.QueryParam>singletonList(new DefaultInputQueryParam(1, UnknownDbType.getInstance(), "7", TYPE_COLUMN)));

        DbTypeManager dbTypeManager = new DbTypeManagerBuilder().on(connection).managing(JdbcTypes.INTEGER_DB_TYPE).build();

        StoredProcedureParamTypeResolver paramTypeResolver = new StoredProcedureParamTypeResolver(dbTypeManager);

        Map<Integer, DbType> parameterTypes = paramTypeResolver.getParameterTypes(connection, queryTemplate);

        assertThat(parameterTypes.size(), equalTo(1));
        assertThat(parameterTypes.get(1), equalTo(JdbcTypes.INTEGER_DB_TYPE));
    }

}

