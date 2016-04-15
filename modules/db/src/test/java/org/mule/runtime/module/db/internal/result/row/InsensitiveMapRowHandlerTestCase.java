/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.row;

import org.mule.module.db.integration.TestRecordUtil;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;
import org.mule.module.db.test.util.ColumnMetadata;
import org.mule.module.db.test.util.ResultSetBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class InsensitiveMapRowHandlerTestCase extends AbstractMuleTestCase
{

    public static final String COLUMN_NAME = "columnName";
    public static final String COLUMN_LABEL = "columnLabel";
    public static final String COLUMN_VALUE = "columnValue";

    @Test
    public void usesColumnLabel() throws Exception
    {

        List<ColumnMetadata> columns = new ArrayList<ColumnMetadata>();
        columns.add(new ColumnMetadata(COLUMN_NAME, COLUMN_LABEL, 1));

        ResultSetBuilder resultSetBuilder = new ResultSetBuilder(columns);
        resultSetBuilder.with(Collections.<String, Object>singletonMap(COLUMN_NAME, COLUMN_VALUE));

        ResultSet build = resultSetBuilder.build();
        build.next();

        InsensitiveMapRowHandler rowHandler = new InsensitiveMapRowHandler();

        Map<String, Object> record = rowHandler.process(build);

        TestRecordUtil.assertRecord(new Record(record), new Record(new Field(COLUMN_LABEL, COLUMN_VALUE)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void detectColumnLabelDuplication() throws Exception
    {

        List<ColumnMetadata> columns = new ArrayList<ColumnMetadata>();
        columns.add(new ColumnMetadata(COLUMN_NAME, COLUMN_LABEL, 1));
        columns.add(new ColumnMetadata(COLUMN_NAME, COLUMN_LABEL, 2));

        ResultSetBuilder resultSetBuilder = new ResultSetBuilder(columns);
        resultSetBuilder.with(Collections.<String, Object>emptyMap());
        ResultSet build = resultSetBuilder.build();
        build.next();

        InsensitiveMapRowHandler rowHandler = new InsensitiveMapRowHandler();

        rowHandler.process(build);
    }
}