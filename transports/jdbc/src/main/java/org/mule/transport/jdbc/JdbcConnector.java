/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageReceiver;
import org.mule.common.DefaultTestResult;
import org.mule.common.FailureType;
import org.mule.common.TestResult;
import org.mule.common.Testable;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.MessageFactory;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectException;
import org.mule.transport.MessageDispatcherUtils;
import org.mule.transport.jdbc.sqlstrategy.DefaultSqlStatementStrategyFactory;
import org.mule.transport.jdbc.sqlstrategy.SqlStatementStrategyFactory;
import org.mule.transport.jdbc.xa.CompositeDataSourceDecorator;
import org.mule.util.StringUtils;
import org.mule.util.TemplateParser;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang.BooleanUtils;

public class JdbcConnector extends AbstractConnector implements Testable
{
    public static final String JDBC = "jdbc";

    // These are properties that can be overridden on the Receiver by the endpoint
    // declaration
    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency";
    public static final long DEFAULT_POLLING_FREQUENCY = 1000;

    private static final Pattern STATEMENT_ARGS = TemplateParser.WIGGLY_MULE_TEMPLATE_PATTERN;

    public static final String USE_DISPATCHER_POOL_SYSTEM_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX
                                                                                + "transport."
                                                                                + JDBC + ".useDispatcherPool";

    private final CompositeDataSourceDecorator databaseDecorator = new CompositeDataSourceDecorator();
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

    private int queryTimeout;

    /**
     * Should each DB record be received in a separate transaction or should there be a single transaction for
     * the entire ResultSet?
     */
    protected boolean transactionPerMessage = true;

    private boolean useDispatcherPool = false;
    protected Map<OutboundEndpoint, MessageDispatcher> endpointDispatchers = new ConcurrentHashMap<OutboundEndpoint, MessageDispatcher>();

    public JdbcConnector(MuleContext context)
    {
        super(context);
        useDispatcherPool = BooleanUtils.toBoolean(System.getProperty(USE_DISPATCHER_POOL_SYSTEM_PROPERTY));
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        databaseDecorator.init(muleContext);
        createMultipleTransactedReceivers = false;

        if (dataSource == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Missing data source"), this);
        }
        if (resultSetHandler == null)
        {
            resultSetHandler = new org.apache.commons.dbutils.handlers.MapListHandler(
                new ColumnAliasRowProcessor());
        }
        decorateDataSourceIfRequired();
        if (queryRunner == null)
        {
            if (this.queryTimeout >= 0)
            {
                queryRunner = new ExtendedQueryRunner(dataSource, this.queryTimeout);
            }
            else
            {
                // Fix for MULE-5825 (Mariano Capurro) -> Adding dataSource parameter
                queryRunner = new QueryRunner(dataSource);
            }
        }
    }

    private void decorateDataSourceIfRequired()
    {
        dataSource = databaseDecorator.decorate(dataSource, getName(), muleContext);
    }

    @Override
    public MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws Exception
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
        return getServiceDescriptor().createMessageReceiver(this, flowConstruct, endpoint, params);
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

        if (!"select".equalsIgnoreCase(readStmt.substring(0, 6))
            && !"call".equalsIgnoreCase(readStmt.substring(0, 4)))
        {
            throw new IllegalArgumentException(
                "Read statement should be a select sql statement or a stored procedure");
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
            logger.warn("transactionPerMessage property is set to false so setting createMultipleTransactedReceivers "
                        + "to false also to prevent creation of multiple JdbcMessageReceivers");
            setCreateMultipleTransactedReceivers(transactionPerMessage);
        }
    }

    /**
     * Parse the given statement filling the parameter list and return the ready to use statement.
     *
     * @param stmt
     * @param params
     */
    public String parseStatement(String stmt, List<String> params)
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
            Object value = getParamValue(endpoint, message, param);

            params[i] = value;
        }
        return params;
    }

    protected Object getParamValue(ImmutableEndpoint endpoint, MuleMessage message, String param)
    {
        Object value = null;
        if (muleContext.getExpressionManager().isValidExpression(param))
        {
            try
            {
                value = muleContext.getExpressionManager().evaluate(param, message);
            }
            catch (ExpressionRuntimeException e)
            {
                // If expression evaluation fails then give the legacy approach a chance.
                logger.warn(MessageFormat.format(
                    "Config is using the legacy param format {0} (no evaluator defined)."
                                    + " This expression can be replaced with {1}header:{2}{3}", param,
                    ExpressionManager.DEFAULT_EXPRESSION_PREFIX, name,
                    ExpressionManager.DEFAULT_EXPRESSION_POSTFIX));
                value = endpoint.getProperty(getNameFromParam(param));
            }
        }
        return value;
    }

    protected String getNameFromParam(String param)
    {
        return param.substring(2, param.length() - 1);
    }

    @Override
    protected void doDispose()
    {
        if (dataSource instanceof Disposable)
        {
            ((Disposable) dataSource).dispose();
        }
    }

    @Override
    protected void doConnect() throws Exception
    {
        Connection connection = null;

        try
        {
            connection = getConnection();
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    /**
     * Verify that we are able to connect to the DataSource (needed for retry policies)
     *
     * @param retryContext
     */
    @Override
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

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method
    }

    @Override
    protected void doStop() throws MuleException
    {
        // template method
    }

    // ////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    // ////////////////////////////////////////////////////////////////////////////////////

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
        this.dataSource = dataSource;
    }

    public ResultSetHandler getResultSetHandler()
    {
        return resultSetHandler;
    }

    public void setResultSetHandler(ResultSetHandler resultSetHandler)
    {
        this.resultSetHandler = resultSetHandler;
    }

    public QueryRunner getQueryRunnerFor(ImmutableEndpoint endpoint)
    {
        String queryTimeoutAsString = (String) endpoint.getProperty("queryTimeout");
        Integer queryTimeout = -1;

        try
        {
            queryTimeout = Integer.valueOf(queryTimeoutAsString);
        }
        catch (NumberFormatException e)
        {

        }

        if (queryTimeout >= 0)
        {
            ExtendedQueryRunner extendedQueryRunner = new ExtendedQueryRunner(
                    this.queryRunner.getDataSource(), queryTimeout);
            return extendedQueryRunner;
        }
        else
        {
            return queryRunner;
        }
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

    public int getQueryTimeout()
    {
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout)
    {
        this.queryTimeout = queryTimeout;
    }

    @Override
    protected <T> T getOperationResourceFactory()
    {
        return (T) getDataSource();
    }

    @Override
    protected <T> T createOperationResource(ImmutableEndpoint endpoint) throws MuleException
    {
        try
        {
            return (T) getDataSource().getConnection();
        }
        catch (SQLException e)
        {
            throw new DefaultMuleException(e);
        }
    }

    @Override
    public TestResult test()
    {
        if (isConnected())
        {
            return new DefaultTestResult(TestResult.Status.SUCCESS);
        }
        Connection con = null;
        try
        {
            con = dataSource.getConnection();
            return new DefaultTestResult(TestResult.Status.SUCCESS);
        }
        catch (Exception e)
        {
            // this surely doesn't cover all cases for all kinds of jdbc drivers but it is better than nothing
            FailureType failureType = FailureType.UNSPECIFIED;
            String msg = e.getMessage();
            if (msg != null && msg.contains("Communications link failure"))
            {
                failureType = FailureType.CONNECTION_FAILURE;
            }
            else if (msg != null && msg.contains("Access denied for user"))
            {
                failureType = FailureType.INVALID_CREDENTIALS;
            }
            return new DefaultTestResult(TestResult.Status.FAILURE, e.getMessage(), failureType, e);
        }
        finally
        {
            if (con != null)
            {
                try
                {
                    con.close();
                }
                catch (SQLException e)
                {
                    // eat exception
                }
            }
        }
    }

    @Override
    public MessageProcessor createDispatcherMessageProcessor(OutboundEndpoint endpoint) throws MuleException
    {
        if (!useDispatcherPool)
        {
            // Avoid lazy initialization of dispatcher in borrow method which would be less performant by
            // creating the dispatcher instance when DispatcherMessageProcessor is created.
            MessageDispatcher dispatcher = dispatcherFactory.create(endpoint);
            applyDispatcherLifecycle(dispatcher);
            endpointDispatchers.put(endpoint, dispatcher);
        }
        return super.createDispatcherMessageProcessor(endpoint);
    }

    @Override
    protected MessageDispatcher borrowDispatcher(OutboundEndpoint endpoint) throws MuleException
    {
        if (useDispatcherPool)
        {
            return super.borrowDispatcher(endpoint);
        }
        else
        {
            return endpointDispatchers.get(endpoint);
        }
    }

    @Override
    protected void returnDispatcher(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        if (useDispatcherPool)
        {
            super.returnDispatcher(endpoint, dispatcher);
        }
        else
        {
            // Nothing to do because implementation of borrowDispatcher doesn't use dispatcher pool
        }
    }

    protected void applyDispatcherLifecycle(MessageDispatcher dispatcher) throws MuleException
    {
        MessageDispatcherUtils.applyLifecycle(dispatcher);
    }

}
