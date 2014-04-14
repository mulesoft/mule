/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.query;

import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.param.OutputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.util.StringUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.Validate;

/**
 * Defines a SQL query that could be executed against a database connection.
 */
public class QueryTemplate
{

    private final String sqlText;
    private final List<InputQueryParam> inputParams;
    private final List<OutputQueryParam> outputParams;
    private final List<QueryParam> params;
    private final QueryType type;
    private final boolean dynamic;
    private final boolean namedParams;

    /**
     * Creates a static SQL query template.
     *
     * @param sqlText sql text containing placeholders for each input and output parameter.
     * @param type    the type of SQL query.
     * @param params  parameters definitions for the query. Non null
     */
    public QueryTemplate(String sqlText, QueryType type, List<QueryParam> params)
    {
        this(sqlText, type, params, false);
    }

    /**
     * Creates a SQL query template.
     *
     * @param sqlText sql text containing placeholders for each input and output parameter.
     * @param type    the type of SQL query.
     * @param params  parameters definitions for the query. Non null
     * @param dynamic indicates whether or not the query is dynamic
     */
    public QueryTemplate(String sqlText, QueryType type, List<QueryParam> params, boolean dynamic)
    {
        Validate.notEmpty(sqlText);
        this.sqlText = sqlText;
        Validate.notNull(type);
        this.type = type;
        this.inputParams = filterInputSqlParams(params);
        this.outputParams = filterOutputSqlParams(params);
        this.params = Collections.unmodifiableList(params);
        this.dynamic = dynamic;
        this.namedParams = usesNamedParams();
    }

    /**
     * Creates a SQL template from another query template
     * @param source query template to clone
     */
    public QueryTemplate(QueryTemplate source)
    {
        this.sqlText = source.sqlText;
        this.type = source.type;
        this.inputParams = source.inputParams;
        this.outputParams = source.outputParams;
        this.params = source.params;
        this.dynamic = source.dynamic;
        this.namedParams = source.namedParams;
    }

    private boolean usesNamedParams()
    {
        boolean firstParam = true;
        boolean namedParams = false;

        for (InputQueryParam inputParam : inputParams)
        {
            if (firstParam)
            {
                namedParams = !StringUtils.isEmpty(inputParam.getName());
                firstParam = false;
            }
            else
            {
                if (namedParams == StringUtils.isEmpty(inputParam.getName()))
                {
                    throw new IllegalArgumentException("Cannot mix named and inline parameters in the same query");
                }
            }
        }

        return namedParams;
    }

    private List<InputQueryParam> filterInputSqlParams(List<QueryParam> inputParams)
    {
        List<InputQueryParam> result = new LinkedList<InputQueryParam>();
        for (QueryParam param : inputParams)
        {
            if (param instanceof InputQueryParam)
            {
                result.add((InputQueryParam) param);
            }
        }

        return result;
    }

    private List<OutputQueryParam> filterOutputSqlParams(List<QueryParam> inputParams)
    {
        List<OutputQueryParam> result = new LinkedList<OutputQueryParam>();
        for (QueryParam param : inputParams)
        {
            if (param instanceof OutputQueryParam)
            {
                result.add((OutputQueryParam) param);
            }
        }

        return result;
    }

    /**
     * Returns the SQL sentence for this query.
     */
    public String getSqlText()
    {
        return sqlText;
    }

    /**
     * Returns the input parameter definitions. Input/output parameters should be also included.
     */
    public List<InputQueryParam> getInputParams()
    {
        return inputParams;
    }

    /**
     * Returns the output parameter definitions. Input/output parameters should be also included.
     */
    public List<OutputQueryParam> getOutputParams()
    {
        return outputParams;
    }

    public List<QueryParam> getParams()
    {
        return params;
    }

    /**
     * Returns the type of SQL query.
     */
    public QueryType getType()
    {
        return type;
    }

    /**
     * Indicates whether or not the query is dynamic.
     * <p/>
     * A query is dynamic when the effective SQL text that will be used to execute it will
     * be resolved on runtime depending on external information.
     */
    public boolean isDynamic()
    {
        return dynamic;
    }

    /**
     * Indicates whether or not the query uses named parameters.
     *
     * @return true if the query uses named parameters, false if there are
     * inline or no parameters
     */
    public boolean usesNamedParameters()
    {
        return namedParams;
    }
}
