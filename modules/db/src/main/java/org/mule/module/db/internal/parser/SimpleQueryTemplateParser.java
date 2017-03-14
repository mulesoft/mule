/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.parser;

import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.type.UnknownDbType;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple SQL parser
 */
public class SimpleQueryTemplateParser implements QueryTemplateParser
{

    private static final char[] PARAMETER_SEPARATORS =
            new char[] {'"', '\'', ':', '&', ',', ';', '(', ')', '|', '=', '+', '-', '*', '%', '/', '\\', '<', '>', '^'};

    /**
     * Set of characters that qualify as comment or quotes starting characters.
     */
    private static final String[] BEGIN_SKIP =
            new String[] {"'", "\"", "--", "/*"};

    /**
     * Set of characters that at are the corresponding comment or quotes ending characters.
     */
    private static final String[] END_SKIP =
            new String[] {"'", "\"", "\n", "*/"};

    private static final Log logger = LogFactory.getLog(SimpleQueryTemplateParser.class);

    private static final String STORED_PROCEDURE_REGEX = "(?ms)(\\{\\s*)?(:\\S+\\s*=)?\\s*CALL\\s* \\s*.*";
    private static final String OPERATION_REGEX_TEMPLATE = "(?ms)%s\\s++.+";
    private static final String UPDATE_REGEX = String.format(OPERATION_REGEX_TEMPLATE, "UPDATE");
    private static final String SELECT_REGEX = String.format(OPERATION_REGEX_TEMPLATE, "SELECT");
    private static final String INSERT_REGEX = String.format(OPERATION_REGEX_TEMPLATE, "INSERT");
    private static final String DELETE_REGEX = String.format(OPERATION_REGEX_TEMPLATE, "DELETE");
    private static final String TRUNCATE_REGEX = String.format(OPERATION_REGEX_TEMPLATE, "TRUNCATE TABLE");
    private static final String MERGE_REGEX = String.format(OPERATION_REGEX_TEMPLATE, "MERGE");
    private static final String WITH_REGEX = String.format(OPERATION_REGEX_TEMPLATE, "WITH");

    private final Pattern storedProcedureMatcher = Pattern.compile(STORED_PROCEDURE_REGEX);
    private final Pattern updateMatcher = Pattern.compile(UPDATE_REGEX);
    private final Pattern selectMatcher = Pattern.compile(SELECT_REGEX);
    private final Pattern insertMatcher = Pattern.compile(INSERT_REGEX);
    private final Pattern deleteMatcher = Pattern.compile(DELETE_REGEX);
    private final Pattern truncateMatcher = Pattern.compile(TRUNCATE_REGEX);
    private final Pattern mergeMatcher = Pattern.compile(MERGE_REGEX);
    private final Pattern withMatcher = Pattern.compile(WITH_REGEX);

    @Override
    public QueryTemplate parse(String sql)
    {
        sql = sql.trim();

        QueryType queryType = getQueryType(sql);

        return doParse(sql, queryType);
    }

    private QueryType getQueryType(String sql)
    {
        QueryType queryType;

        sql = sql.toUpperCase();

        if (isSelect(sql))
        {
            queryType = QueryType.SELECT;
        }
        else if (isInsert(sql))
        {
            queryType = QueryType.INSERT;
        }
        else if (isDelete(sql))
        {
            queryType = QueryType.DELETE;
        }
        else if (isUpdate(sql))
        {
            queryType = QueryType.UPDATE;
        }
        else if (isStoredProcedureCall(sql))
        {
            queryType = QueryType.STORE_PROCEDURE_CALL;
        }
        else if (isTruncate(sql))
        {
            queryType = QueryType.TRUNCATE;
        }
        else if (isMerge(sql))
        {
            queryType = QueryType.MERGE;
        }
        else if (isWith(sql))
        {
            queryType = QueryType.SELECT;
        }
        else
        {
            queryType = QueryType.DDL;
        }
        return queryType;
    }

    private QueryTemplate doParse(String sqlText, QueryType queryType)
    {
        if (StringUtils.isEmpty(sqlText))
        {
            throw new QueryTemplateParsingException("SQL text cannot be empty");
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Parsing SQL: " + sqlText);
        }

        String sqlToUse = "";
        List<QueryParam> parameterList = new ArrayList<QueryParam>();

        char[] sqlTextChars = sqlText.toCharArray();
        int tokenStart = 0;
        int paramIndex = 1;

        while (tokenStart < sqlTextChars.length)
        {
            int skipToPosition;

            while (tokenStart < sqlTextChars.length)
            {
                skipToPosition = skipCommentsAndQuotes(sqlTextChars, tokenStart);
                if (tokenStart == skipToPosition)
                {
                    break;
                }
                else
                {
                    sqlToUse = sqlToUse + sqlText.substring(tokenStart, skipToPosition);
                    tokenStart = skipToPosition;
                }
            }
            if (tokenStart >= sqlTextChars.length)
            {
                break;
            }
            char currentChar = sqlTextChars[tokenStart];
            int tokenEnd = tokenStart + 1;
            if (tokenEnd < sqlTextChars.length && currentChar == '#' && sqlTextChars[tokenEnd] == '[')
            {
                int openBrackets = 0;
                while (tokenEnd < sqlTextChars.length)
                {
                    if (sqlTextChars[tokenEnd] == ']')
                    {
                        openBrackets--;
                    }
                    else if (sqlTextChars[tokenEnd] == '[')
                    {
                        openBrackets++;
                    }

                    if (openBrackets == 0)
                    {
                        break;
                    }
                    tokenEnd++;

                }
                if (tokenEnd == sqlTextChars.length)
                {
                    throw new QueryTemplateParsingException("Invalid Mule expression: " + sqlText.substring(tokenStart));
                }

                tokenEnd++;
                String value = sqlText.substring(tokenStart, tokenEnd);
                QueryParam inputParam = new DefaultInputQueryParam(paramIndex++, UnknownDbType.getInstance(), value);
                parameterList.add(inputParam);
                sqlToUse = sqlToUse + "?";
                tokenStart = tokenEnd;
            }
            else if (currentChar == ':')
            {
                if (tokenEnd < sqlTextChars.length && '=' == sqlTextChars[tokenEnd])
                {
                    sqlToUse = sqlToUse + currentChar;
                    tokenStart++;
                }
                else
                {
                    String parameter;

                    while (tokenEnd < sqlTextChars.length && !isParameterSeparator(sqlTextChars[tokenEnd]))
                    {
                        tokenEnd++;
                    }
                    if (tokenEnd - tokenStart > 1)
                    {
                        sqlToUse = sqlToUse + "?";
                        parameter = sqlText.substring(tokenStart + 1, tokenEnd);
                        QueryParam inputParam = new DefaultInputQueryParam(paramIndex++, UnknownDbType.getInstance(), null, parameter);
                        parameterList.add(inputParam);
                    }
                    tokenStart = tokenEnd;
                }
            }
            else if (isParamChar(currentChar))
            {
                QueryParam inputParam = new DefaultInputQueryParam(paramIndex++, UnknownDbType.getInstance(), null);
                parameterList.add(inputParam);
                tokenStart++;
                sqlToUse = sqlToUse + currentChar;
            }
            else
            {
                sqlToUse = sqlToUse + currentChar;
                tokenStart++;
            }
        }

        return new QueryTemplate(sqlToUse, queryType, parameterList);
    }

    private boolean isParamChar(char c)
    {
        return c == '?';
    }

    private boolean isStoredProcedureCall(String sqlText)
    {
        Matcher m = storedProcedureMatcher.matcher(sqlText);

        return m.matches();
    }

    private boolean isTruncate(String sqlText)
    {
        Matcher m = truncateMatcher.matcher(sqlText);

        return m.matches();
    }

    private boolean isMerge(String sqlText)
    {
        Matcher m = mergeMatcher.matcher(sqlText);

        return m.matches();
    }

    private boolean isWith(String sqlText)
    {
        Matcher m = withMatcher.matcher(sqlText);

        return m.matches();
    }

    private boolean isUpdate(String sqlText)
    {
        Matcher m = updateMatcher.matcher(sqlText);

        return m.matches();
    }

    private boolean isInsert(String sqlText)
    {
        Matcher m = insertMatcher.matcher(sqlText);

        return m.matches();
    }

    private boolean isDelete(String sqlText)
    {
        Matcher m = deleteMatcher.matcher(sqlText);

        return m.matches();
    }

    private boolean isSelect(String sqlText)
    {
        Matcher m = selectMatcher.matcher(sqlText);

        return m.matches();
    }

    private static boolean isParameterSeparator(char c)
    {
        if (Character.isWhitespace(c))
        {
            return true;
        }
        for (char separator : PARAMETER_SEPARATORS)
        {
            if (c == separator)
            {
                return true;
            }
        }
        return false;
    }

    private static int skipCommentsAndQuotes(char[] statement, int position)
    {
        for (int i = 0; i < BEGIN_SKIP.length; i++)
        {
            if (statement[position] == BEGIN_SKIP[i].charAt(0))
            {
                boolean match = true;
                for (int j = 1; j < BEGIN_SKIP[i].length(); j++)
                {
                    if (!(statement[position + j] == BEGIN_SKIP[i].charAt(j)))
                    {
                        match = false;
                        break;
                    }
                }
                if (match)
                {
                    int offset = BEGIN_SKIP[i].length();
                    for (int m = position + offset; m < statement.length; m++)
                    {
                        if (statement[m] == END_SKIP[i].charAt(0))
                        {
                            boolean endMatch = true;
                            int endPos = m;
                            for (int n = 1; n < END_SKIP[i].length(); n++)
                            {
                                if (m + n >= statement.length)
                                {
                                    // last comment not closed properly
                                    return statement.length;
                                }
                                if (!(statement[m + n] == END_SKIP[i].charAt(n)))
                                {
                                    endMatch = false;
                                    break;
                                }
                                endPos = m + n;
                            }
                            if (endMatch)
                            {
                                // found character sequence ending comment or quote
                                return endPos + 1;
                            }
                        }
                    }
                    // character sequence ending comment or quote not found
                    return statement.length;
                }

            }
        }
        return position;
    }

}
