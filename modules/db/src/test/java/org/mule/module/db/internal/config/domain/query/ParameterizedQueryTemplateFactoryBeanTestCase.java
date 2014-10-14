/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.type.JdbcTypes;
import org.mule.module.db.internal.parser.QueryTemplateParser;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.List;

import org.hamcrest.core.IsEqual;
import org.junit.Test;

@SmallTest
public class ParameterizedQueryTemplateFactoryBeanTestCase extends AbstractMuleTestCase
{

    public static final String QUERY = "select * from test";
    public static final String PARAMETERIZED_QUERY = "select * from test where position = :position";
    public static final String PARSED_PARAMETERIZED_QUERY = "select * from test where position = ?";

    @Test
    public void createsQueryWithNoParams() throws Exception
    {
        QueryTemplate queryTemplate = new QueryTemplate(QUERY, QueryType.SELECT, Collections.<QueryParam>emptyList());

        QueryTemplateParser queryParser = mock(QueryTemplateParser.class);
        when(queryParser.parse(QUERY)).thenReturn(queryTemplate);

        ParameterizedQueryTemplateFactoryBean factoryBean = new ParameterizedQueryTemplateFactoryBean(QUERY, Collections.EMPTY_LIST, queryParser);

        QueryTemplate createdQueryTemplate = factoryBean.getObject();

        assertThat(createdQueryTemplate.getSqlText(), equalTo(QUERY));
        assertThat(createdQueryTemplate.getType(), equalTo(QueryType.SELECT));
        assertThat(createdQueryTemplate.getParams(), is(empty()));
    }


    @Test
    public void createsQueryWithDefaultParams() throws Exception
    {
        List<QueryParam> defaultParams = Collections.<QueryParam>singletonList(new DefaultInputQueryParam(1, JdbcTypes.INTEGER_DB_TYPE, "5", "position"));
        QueryTemplate queryTemplate = new QueryTemplate(PARSED_PARAMETERIZED_QUERY, QueryType.SELECT, defaultParams);

        QueryTemplateParser queryParser = mock(QueryTemplateParser.class);
        when(queryParser.parse(PARAMETERIZED_QUERY)).thenReturn(queryTemplate);

        ParameterizedQueryTemplateFactoryBean factoryBean = new ParameterizedQueryTemplateFactoryBean(PARAMETERIZED_QUERY, Collections.EMPTY_LIST, queryParser);

        QueryTemplate createdQueryTemplate = factoryBean.getObject();

        assertThat(createdQueryTemplate.getSqlText(), equalTo(PARSED_PARAMETERIZED_QUERY));
        assertThat(createdQueryTemplate.getType(), equalTo(QueryType.SELECT));
        assertThat(createdQueryTemplate.getParams().size(), equalTo(1));
        InputQueryParam inputQueryParam = createdQueryTemplate.getInputParams().get(0);
        assertThat(inputQueryParam.getValue(), IsEqual.<Object>equalTo("5"));
    }

    @Test
    public void createsQueryWithOverridenParams() throws Exception
    {
        List<QueryParam> defaultParams = Collections.<QueryParam>singletonList(new DefaultInputQueryParam(1, JdbcTypes.INTEGER_DB_TYPE, "5", "position"));
        QueryTemplate queryTemplate = new QueryTemplate(PARSED_PARAMETERIZED_QUERY, QueryType.SELECT, defaultParams);

        QueryTemplateParser queryParser = mock(QueryTemplateParser.class);
        when(queryParser.parse(PARAMETERIZED_QUERY)).thenReturn(queryTemplate);

        List<QueryParam> overriddenParams = Collections.<QueryParam>singletonList(new DefaultInputQueryParam(1, JdbcTypes.INTEGER_DB_TYPE, "10", "position"));
        ParameterizedQueryTemplateFactoryBean factoryBean = new ParameterizedQueryTemplateFactoryBean(PARAMETERIZED_QUERY, overriddenParams, queryParser);

        QueryTemplate createdQueryTemplate = factoryBean.getObject();

        assertThat(createdQueryTemplate.getSqlText(), equalTo(PARSED_PARAMETERIZED_QUERY));
        assertThat(createdQueryTemplate.getType(), equalTo(QueryType.SELECT));
        assertThat(createdQueryTemplate.getParams().size(), equalTo(1));
        InputQueryParam inputQueryParam = createdQueryTemplate.getInputParams().get(0);
        assertThat(inputQueryParam.getValue(), IsEqual.<Object>equalTo("10"));
    }
}