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
import org.mule.module.db.internal.domain.param.DefaultInOutQueryParam;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.DefaultOutputQueryParam;
import org.mule.module.db.internal.domain.param.InOutQueryParam;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.param.OutputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.JdbcTypes;
import org.mule.module.db.internal.domain.type.UnknownDbType;
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
    public static final String POSITION_PARAM_NAME = "position";
    public static final String TEMPLATE_PARAM_VALUE = "5";
    public static final String OVERRIDDEN_PARAM_VALUE = "10";

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
        List<QueryParam> defaultParams = Collections.<QueryParam>singletonList(new DefaultInputQueryParam(1, JdbcTypes.INTEGER_DB_TYPE, TEMPLATE_PARAM_VALUE, POSITION_PARAM_NAME));
        QueryTemplate queryTemplate = new QueryTemplate(PARSED_PARAMETERIZED_QUERY, QueryType.SELECT, defaultParams);

        QueryTemplateParser queryParser = mock(QueryTemplateParser.class);
        when(queryParser.parse(PARAMETERIZED_QUERY)).thenReturn(queryTemplate);

        ParameterizedQueryTemplateFactoryBean factoryBean = new ParameterizedQueryTemplateFactoryBean(PARAMETERIZED_QUERY, Collections.EMPTY_LIST, queryParser);

        QueryTemplate createdQueryTemplate = factoryBean.getObject();

        assertThat(createdQueryTemplate.getSqlText(), equalTo(PARSED_PARAMETERIZED_QUERY));
        assertThat(createdQueryTemplate.getType(), equalTo(QueryType.SELECT));
        assertThat(createdQueryTemplate.getParams().size(), equalTo(1));
        InputQueryParam inputQueryParam = createdQueryTemplate.getInputParams().get(0);
        assertThat(inputQueryParam.getValue(), IsEqual.<Object>equalTo(TEMPLATE_PARAM_VALUE));
    }

    @Test
    public void createsQueryWithOverriddenParams() throws Exception
    {
        QueryTemplate createdQueryTemplate = doOverriddenParamTest(JdbcTypes.INTEGER_DB_TYPE, new DefaultInputQueryParam(1, JdbcTypes.INTEGER_DB_TYPE, OVERRIDDEN_PARAM_VALUE, POSITION_PARAM_NAME));

        assertThat(createdQueryTemplate.getSqlText(), equalTo(PARSED_PARAMETERIZED_QUERY));
        assertThat(createdQueryTemplate.getType(), equalTo(QueryType.SELECT));
        assertThat(createdQueryTemplate.getParams().size(), equalTo(1));
        InputQueryParam inputQueryParam = createdQueryTemplate.getInputParams().get(0);
        assertThat(inputQueryParam.getValue(), IsEqual.<Object>equalTo(OVERRIDDEN_PARAM_VALUE));
    }

    @Test
    public void overrideInputParamUsingTemplateType() throws Exception
    {
        doInputParamOverrideTest(JdbcTypes.INTEGER_DB_TYPE, UnknownDbType.getInstance(), JdbcTypes.INTEGER_DB_TYPE);
    }

    @Test
    public void overrideInputParamUsingOverriddenType() throws Exception
    {
        doInputParamOverrideTest(UnknownDbType.getInstance(), JdbcTypes.INTEGER_DB_TYPE, JdbcTypes.INTEGER_DB_TYPE);
    }

    @Test
    public void overrideInOutParamUsingTemplateType() throws Exception
    {
        doInOutParamOverrideTest(JdbcTypes.INTEGER_DB_TYPE, UnknownDbType.getInstance(), JdbcTypes.INTEGER_DB_TYPE);
    }

    @Test
    public void overrideInOutParamUsingOverriddenType() throws Exception
    {
        doInOutParamOverrideTest(UnknownDbType.getInstance(), JdbcTypes.INTEGER_DB_TYPE, JdbcTypes.INTEGER_DB_TYPE);
    }

    @Test
    public void overrideOutputParamUsingTemplateType() throws Exception
    {
        doOutputParamOverrideTest(JdbcTypes.INTEGER_DB_TYPE, UnknownDbType.getInstance(), JdbcTypes.INTEGER_DB_TYPE);
    }

    @Test
    public void overrideOutputParamUsingOverriddenType() throws Exception
    {
        doOutputParamOverrideTest(UnknownDbType.getInstance(), JdbcTypes.INTEGER_DB_TYPE, JdbcTypes.INTEGER_DB_TYPE);
    }

    private void doInputParamOverrideTest(DbType templateParamType, DbType overriddenParamType, DbType expectedParamType) throws Exception
    {
        QueryParam overriddenParam = new DefaultInputQueryParam(2, overriddenParamType, OVERRIDDEN_PARAM_VALUE, POSITION_PARAM_NAME);

        QueryTemplate createdQueryTemplate = doOverriddenParamTest(templateParamType, overriddenParam);

        assertThat(createdQueryTemplate.getParams().size(), equalTo(1));
        InputQueryParam inputQueryParam = createdQueryTemplate.getInputParams().get(0);
        assertThat(inputQueryParam.getIndex(), equalTo(1));
        assertThat(inputQueryParam.getType(), equalTo(expectedParamType));
        assertThat(inputQueryParam.getName(), equalTo(POSITION_PARAM_NAME));
        assertThat(inputQueryParam.getValue(), IsEqual.<Object>equalTo(OVERRIDDEN_PARAM_VALUE));
    }

    private void doInOutParamOverrideTest(DbType templateParamType, DbType overriddenParamType, DbType expectedParamType) throws Exception
    {
        QueryParam overriddenParam = new DefaultInOutQueryParam(2, overriddenParamType, POSITION_PARAM_NAME, OVERRIDDEN_PARAM_VALUE);

        QueryTemplate createdQueryTemplate = doOverriddenParamTest(templateParamType, overriddenParam);

        assertThat(createdQueryTemplate.getParams().size(), equalTo(1));
        InOutQueryParam queryParam = (InOutQueryParam) createdQueryTemplate.getParams().get(0);
        assertThat(queryParam.getIndex(), equalTo(1));
        assertThat(queryParam.getType(), equalTo(expectedParamType));
        assertThat(queryParam.getName(), equalTo(POSITION_PARAM_NAME));
        assertThat(queryParam.getValue(), IsEqual.<Object>equalTo(OVERRIDDEN_PARAM_VALUE));
    }

    private QueryTemplate doOverriddenParamTest(DbType templateParamType, QueryParam overriddenParam) throws Exception
    {
        List<QueryParam> defaultParams = Collections.<QueryParam>singletonList(new DefaultInputQueryParam(1, templateParamType, TEMPLATE_PARAM_VALUE, POSITION_PARAM_NAME));
        QueryTemplate queryTemplate = new QueryTemplate(PARSED_PARAMETERIZED_QUERY, QueryType.SELECT, defaultParams);

        QueryTemplateParser queryParser = mock(QueryTemplateParser.class);
        when(queryParser.parse(PARAMETERIZED_QUERY)).thenReturn(queryTemplate);

        List<QueryParam> overriddenParams = Collections.<QueryParam>singletonList(overriddenParam);

        ParameterizedQueryTemplateFactoryBean factoryBean = new ParameterizedQueryTemplateFactoryBean(PARAMETERIZED_QUERY, overriddenParams, queryParser);

        return factoryBean.getObject();
    }

    private void doOutputParamOverrideTest(DbType templateParamType, DbType overriddenParamType, DbType expectedParamType) throws Exception
    {
        QueryParam overriddenParam = new DefaultOutputQueryParam(2, overriddenParamType, POSITION_PARAM_NAME);

        List<QueryParam> defaultParams = Collections.<QueryParam>singletonList(new DefaultInputQueryParam(1, templateParamType, TEMPLATE_PARAM_VALUE, POSITION_PARAM_NAME));
        QueryTemplate queryTemplate = new QueryTemplate(PARSED_PARAMETERIZED_QUERY, QueryType.SELECT, defaultParams);

        QueryTemplateParser queryParser = mock(QueryTemplateParser.class);
        when(queryParser.parse(PARAMETERIZED_QUERY)).thenReturn(queryTemplate);

        List<QueryParam> overriddenParams = Collections.singletonList(overriddenParam);

        ParameterizedQueryTemplateFactoryBean factoryBean = new ParameterizedQueryTemplateFactoryBean(PARAMETERIZED_QUERY, overriddenParams, queryParser);

        QueryTemplate createdQueryTemplate = factoryBean.getObject();

        assertThat(createdQueryTemplate.getParams().size(), equalTo(1));
        OutputQueryParam queryParam = createdQueryTemplate.getOutputParams().get(0);
        assertThat(queryParam.getIndex(), equalTo(1));
        assertThat(queryParam.getType(), equalTo(expectedParamType));
        assertThat(queryParam.getName(), equalTo(POSITION_PARAM_NAME));
    }
}