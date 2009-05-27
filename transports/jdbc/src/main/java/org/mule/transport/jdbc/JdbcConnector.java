/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.retry.RetryContext;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.MessageFactory;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectException;
import org.mule.transport.jdbc.sqlstrategy.DefaultSqlStatementStrategyFactory;
import org.mule.transport.jdbc.sqlstrategy.SqlStatementStrategyFactory;
import org.mule.transport.jdbc.xa.DataSourceWrapper;
import org.mule.util.StringUtils;
import org.mule.util.TemplateParser;

import java.sql.Connection;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

public class JdbcConnector extends AbstractConnector
{
    public static final String JDBC = "jdbc";

    // These are properties that can be overridden on the Receiver by the endpoint
    // declaration
    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency";
    public static final long DEFAULT_POLLING_FREQUENCY = 1000;

    private static final Pattern STATEMENT_ARGS = TemplateParser.WIGGLY_MULE_TEMPLATE_PATTERN;

    private SqlStatementStrategyFactory sqlStatementStrategyFactory = new DefaultSqlStatementStrategyFactory();


    /* Register the SQL Exception reader if this class gets loaded */
    static
    {
        ExceptionHelper.registerExceptionReader(new SQLExceptionReader());
    }

    protected long pollingFrequency = 0;
    protected Map queries;

    protected DataSource dataSource;
    protected ResultSetHandler resultSetHandler;
    protected QueryRunner queryRunner;
    
    /** 
     * Should each DB record be received in a separate transaction or should 
     * there be a single transaction for the entire ResultSet? 
     */
    protected boolean transactionPerMessage = true;

    protected void doInitialise() throws InitialisationException
    {
        createMultipleTransactedReceivers = false;

        if (dataSource == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Missing data source"), this);
        }
        if (resultSetHandler == null)
        {
            resultSetHandler = new org.apache.commons.dbutils.handlers.MapListHandler();
        }
        if (queryRunner == null)
        {
            queryRunner = new QueryRunner();
        }
    }

    public MessageReceiver createReceiver(Service service, InboundEndpoint endpoint) throws Exception
    {
        Map props = endpoint.getProperties();
        if (props != null)
        {
            String tempPolling = (String) props.get(PROPERTY_POLLING_FREQUENCY);
            if (tempPolling != null)
            {
                pollingFrequency = Long.parseLong(tempPolling);
            }
        }

        if (pollingFrequency <= 0)
        {
            pollingFrequency = DEFAULT_POLLING_FREQUENCY;
        }

        String[] params = getReadAndAckStatements(endpoint);
        return getServiceDescriptor().createMessageReceiver(this, service, endpoint, params);
    }

    public String[] getReadAndAckStatements(ImmutableEndpoint endpoint)
    {
        String str;

        // Find read statement
        String readStmt;
        if ((str = (String) endpoint.getProperty("sql")) != null)
        {
            readStmt = str;
        }
        else
        {
            readStmt = endpoint.getEndpointURI().getAddress();
        }

        // Find ack statement
        String ackStmt;
        if ((str = (String) endpoint.getProperty("ack")) != null)
        {
            ackStmt = str;
            if ((str = getQuery(endpoint, ackStmt)) != null)
            {
                ackStmt = str;
            }
            ackStmt = ackStmt.trim();
        }
        else
        {
            ackStmt = readStmt + ".ack";
            if ((str = getQuery(endpoint, ackStmt)) != null)
            {
                ackStmt = str.trim();
            }
            else
            {
                ackStmt = null;
            }
        }

        // Translate both using queries map
        if ((str = getQuery(endpoint, readStmt)) != null)
        {
            readStmt = str;
        }
        if (readStmt == null)
        {
            throw new IllegalArgumentException("Read statement should not be null");
        }
        else
        {
            // MULE-3109: trim the readStatement for better user experience
            readStmt = readStmt.trim();
        }

        if (!"select".equalsIgnoreCase(readStmt.substring(0, 6)) && !"call".equalsIgnoreCase(readStmt.substring(0, 4)))
        {
            throw new IllegalArgumentException("Read statement should be a select sql statement or a stored procedure");
        }
        if (ackStmt != null)
        {
            if (!"insert".equalsIgnoreCase(ackStmt.substring(0, 6))
                    && !"update".equalsIgnoreCase(ackStmt.substring(0, 6))
                    && !"delete".equalsIgnoreCase(ackStmt.substring(0, 6)))
            {
                throw new IllegalArgumentException(
                        "Ack statement should be an insert / update / delete sql statement");
            }
        }
        return new String[]{readStmt, ackStmt};
    }

    public String getQuery(ImmutableEndpoint endpoint, String stmt)
    {
        Object query = null;
        if (endpoint != null && endpoint.getProperties() != null)
        {
            Object queries = endpoint.getProperties().get("queries");
            if (queries instanceof Map)
            {
                query = ((Map) queries).get(stmt);
            }
        }
        if (query == null)
        {
            if (this.queries != null)
            {
                query = this.queries.get(stmt);
            }
        }
        return query == null ? null : query.toString();
    }

    public Connection getConnection() throws Exception
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            if (tx.hasResource(dataSource))
            {
                logger.debug("Retrieving connection from current transaction: " + tx);
                return (Connection) tx.getResource(dataSource);
            }
        }
        logger.debug("Retrieving new connection from data source");
        
        Connection con;
        try
        {
            con = dataSource.getConnection();
        }
        catch (Exception e)
        {
            throw new ConnectException(e, this);
        }

        if (tx != null)
        {
            logger.debug("Binding connection " + con + " to current transaction: " + tx);
            try
            {
                tx.bindResource(dataSource, con);
            }
            catch (TransactionException e)
            {
                JdbcUtils.close(con);
                throw new RuntimeException("Could not bind connection to current transaction: " + tx, e);
            }
        }
        return con;
    }

    public boolean isTransactionPerMessage()
    {
        return transactionPerMessage;
    }

    public void setTransactionPerMessage(boolean transactionPerMessage)
    {
        this.transactionPerMessage = transactionPerMessage;
        if (!transactionPerMessage)
        {
            logger.warn("transactionPerMessage property is set to false so setting createMultipleTransactedReceivers " +
                    "to false also to prevent creation of multiple JdbcMessageReceivers");
            setCreateMultipleTransactedReceivers(transactionPerMessage);
        }
    }

    /**
     * Parse the given statement filling the parameter list and return the ready to
     * use statement.
     *
     * @param stmt
     * @param params
     * @return
     */
    public String parseStatement(String stmt, List params)
    {
        if (stmt == null)
        {
            return stmt;
        }
        Matcher m = STATEMENT_ARGS.matcher(stmt);
        StringBuffer sb = new StringBuffer(200);
        while (m.find())
        {
            String key = m.group();
            m.appendReplacement(sb, "?");
            //Special legacy handling for #[payload]
            if (key.equals("#[payload]"))
            {
                //MULE-3597
                logger.error("invalid expression template #[payload]. It should be replaced with #[payload:] to conform with the correct expression syntax. Mule has replaced this for you, but may not in future versions.");
                key = "#[payload:]";
            }
            params.add(key);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public Object[] getParams(ImmutableEndpoint endpoint, List paramNames, MuleMessage message, String query)
            throws Exception
    {
        Object[] params = new Object[paramNames.size()];
        for (int i = 0; i < paramNames.size(); i++)
        {
            String param = (String) paramNames.get(i);
            Object value = null;
            // If we find a value and it happens to be null, thats acceptable
            boolean foundValue = false;
            boolean validExpression = muleContext.getExpressionManager().isValidExpression(param);
            //There must be an expression namespace to use the ExpresionEvaluator i.e. header:type
            if (message != null && validExpression)
            {
                value = muleContext.getExpressionManager().evaluate(param, message);
                foundValue = value != null;
            }
            if (!foundValue)
            {
                String name = param.substring(2, param.length() - 1);
                //MULE-3597
                if (!validExpression)
                {
                    logger.warn(MessageFormat.format("Config is using the legacy param format {0} (no evaluator defined)." +
                                                     " This expression can be replaced with {1}header:{2}{3}",
                                                     param, ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                                                     name, ExpressionManager.DEFAULT_EXPRESSION_POSTFIX));
                }
                value = endpoint.getProperty(name);
            }

            // Allow null values which may be acceptable to the user
            // Why shouldn't nulls be allowed? Otherwise every null parameter has to
            // be defined
            // if (value == null && !foundValue)
            // {
            // throw new IllegalArgumentException("Can not retrieve argument " +
            // name);
            // }
            params[i] = value;
        }
        return params;
    }

    protected String getNameFromParam(String param)
    {
        return param.substring(2, param.length() - 1);
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    /** 
     * Verify that we are able to connect to the DataSource (needed for retry policies)
     * @param retryContext
     */
    public RetryContext validateConnection(RetryContext retryContext)
    {
        Connection con;
        try
        {
            con = getConnection();
            if (con != null)
            {
                con.close();
            }
            retryContext.setOk();
        }
        catch (Exception ex)
        {
            retryContext.setFailed(ex);
        }
        finally
        {
            con = null;
        }

        return retryContext;
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws MuleException
    {
        // template method
    }

    protected void doStop() throws MuleException
    {
        // template method
    }

    //////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    //////////////////////////////////////////////////////////////////////////////////////

    public String getProtocol()
    {
        return JDBC;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        if (dataSource instanceof XADataSource)
        {
            this.dataSource = new DataSourceWrapper((XADataSource) dataSource);
        }
        else
        {
            this.dataSource = dataSource;
        }
    }

    public ResultSetHandler getResultSetHandler()
    {
        return resultSetHandler;
    }

    public void setResultSetHandler(ResultSetHandler resultSetHandler)
    {
        this.resultSetHandler = resultSetHandler;
    }

    public QueryRunner getQueryRunner()
    {
        return queryRunner;
    }

    public void setQueryRunner(QueryRunner queryRunner)
    {
        this.queryRunner = queryRunner;
    }

    /**
     * @return Returns the pollingFrequency.
     */
    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    /**
     * @param pollingFrequency The pollingFrequency to set.
     */
    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    /**
     * @return Returns the queries.
     */
    public Map getQueries()
    {
        return queries;
    }

    /**
     * @param queries The queries to set.
     */
    public void setQueries(Map queries)
    {
        this.queries = queries;
    }

    public SqlStatementStrategyFactory getSqlStatementStrategyFactory()
    {
        return sqlStatementStrategyFactory;
    }

    public void setSqlStatementStrategyFactory(SqlStatementStrategyFactory sqlStatementStrategyFactory)
    {
        this.sqlStatementStrategyFactory = sqlStatementStrategyFactory;
    }

    public String getStatement(ImmutableEndpoint endpoint)
    {
        String writeStmt = endpoint.getEndpointURI().getAddress();
        String str;
        if ((str = getQuery(endpoint, writeStmt)) != null)
        {
            writeStmt = str;
        }
        writeStmt = StringUtils.trimToEmpty(writeStmt);
        if (StringUtils.isBlank(writeStmt))
        {
            throw new IllegalArgumentException("Missing statement");
        }

        return writeStmt;
    }
}
