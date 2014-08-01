/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.type.UnknownDbType;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class TemplateQueryConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/template/template-config.xml";
    }

    @Test
    public void definesSelect() throws Exception
    {
        Object queryTemplateBean = muleContext.getRegistry().get("testSelect");
        assertTrue(queryTemplateBean instanceof QueryTemplate);
        QueryTemplate queryTemplate = (QueryTemplate) queryTemplateBean;
        assertEquals(QueryType.SELECT, queryTemplate.getType());
        assertEquals("SELECT * FROM PLANET WHERE POSITION = ?", queryTemplate.getSqlText());
        assertEquals(1, queryTemplate.getInputParams().size());
        InputQueryParam param1 = queryTemplate.getInputParams().get(0);
        assertEquals(UnknownDbType.getInstance(), param1.getType());
        assertEquals("position", param1.getName());
        assertEquals("0", param1.getValue());
    }

    @Test
    public void definesUpdate() throws Exception
    {
        Object queryTemplateBean = muleContext.getRegistry().get("testUpdate");
        assertTrue(queryTemplateBean instanceof QueryTemplate);
        QueryTemplate queryTemplate = (QueryTemplate) queryTemplateBean;
        assertEquals(QueryType.UPDATE, queryTemplate.getType());
        assertEquals("update PLANET set NAME='Mercury' where ID=?", queryTemplate.getSqlText());
        assertEquals(1, queryTemplate.getInputParams().size());
        InputQueryParam param1 = queryTemplate.getInputParams().get(0);
        assertEquals(UnknownDbType.getInstance(), param1.getType());
        assertEquals("id", param1.getName());
        assertEquals("0", param1.getValue());
    }

    @Test
    public void readQueryFromFile() throws Exception
    {
        doQueryFromFileTest(muleContext.getRegistry().get("testFileQuery"));
    }

    @Test
    public void readQueryFromFileAndEmptyContent() throws Exception
    {
        doQueryFromFileTest(muleContext.getRegistry().get("testFileQueryAndEmptyContent"));
    }

    @Test
    public void usesDynamicQuery() throws Exception
    {
        Object queryTemplateBean = muleContext.getRegistry().get("testDynamicQuery");
        assertTrue(queryTemplateBean instanceof QueryTemplate);
        QueryTemplate queryTemplate = (QueryTemplate) queryTemplateBean;
        assertTrue(queryTemplate.isDynamic());
        assertEquals("SELECT * FROM PLANET WHERE POSITION = #[position]", queryTemplate.getSqlText());
        assertEquals(0, queryTemplate.getInputParams().size());
    }

    @Test
    public void usesNullDefaultParamValue() throws Exception
    {
        Object queryTemplateBean = muleContext.getRegistry().get("testNullParamsQuery");

        QueryTemplate queryTemplate = (QueryTemplate) queryTemplateBean;

        assertFalse(queryTemplate.isDynamic());
        assertEquals("SELECT * FROM PLANET WHERE POSITION = ?", queryTemplate.getSqlText());
        assertEquals(1, queryTemplate.getInputParams().size());
        InputQueryParam param1 = queryTemplate.getInputParams().get(0);
        assertEquals(UnknownDbType.getInstance(), param1.getType());
        assertEquals("position", param1.getName());
        assertEquals(null, param1.getValue());
    }

    private void doQueryFromFileTest(Object queryTemplateBean)
    {
        assertTrue(queryTemplateBean instanceof QueryTemplate);
        QueryTemplate queryTemplate = (QueryTemplate) queryTemplateBean;
        assertEquals(QueryType.SELECT, queryTemplate.getType());
        assertEquals("SELECT * FROM PLANET WHERE POSITION = ?", queryTemplate.getSqlText());
        assertEquals(1, queryTemplate.getInputParams().size());
        InputQueryParam param1 = queryTemplate.getInputParams().get(0);
        assertEquals(UnknownDbType.getInstance(), param1.getType());
        assertEquals("position", param1.getName());
        assertEquals("1", param1.getValue());
    }
}
