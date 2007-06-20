/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.providers.AbstractConnector;
import org.mule.providers.jdbc.i18n.JdbcMessages;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.ClassUtils;
import org.mule.util.ExceptionUtils;
import org.mule.util.StringUtils;
import org.mule.util.properties.BeanPropertyExtractor;
import org.mule.util.properties.MapPropertyExtractor;
import org.mule.util.properties.MessagePropertyExtractor;
import org.mule.util.properties.PayloadPropertyExtractor;
import org.mule.util.properties.PropertyExtractor;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

public class JdbcConnector extends AbstractConnector
{
    // These are properties that can be overridden on the Receiver by the endpoint
    // declaration
    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency";
    public static final long DEFAULT_POLLING_FREQUENCY = 1000;

    private static final String DEFAULT_QUERY_RUNNER = "org.apache.commons.dbutils.QueryRunner";
    private static final String DEFAULT_RESULTSET_HANDLER = "org.apache.commons.dbutils.handlers.MapListHandler";

    private static final Pattern STATEMENT_ARGS = Pattern.compile("\\$\\{[^\\}]*\\}");

    /* Register the SQL Exception reader if this class gets loaded */
    static
    {
        ExceptionHelper.registerExceptionReader(new SQLExceptionReader());
    }

    protected long pollingFrequency = 0;
    protected DataSource dataSource;
    protected String dataSourceJndiName;
    protected Context jndiContext;
    protected String jndiInitialFactory;
    protected String jndiProviderUrl;
    protected Map providerProperties;
    protected Map queries;
    protected String resultSetHandler = DEFAULT_RESULTSET_HANDLER;
    protected String queryRunner = DEFAULT_QUERY_RUNNER;
    protected Set queryValueExtractors;
    protected Set propertyExtractors;

    protected void doInitialise() throws InitialisationException
    {
        try
        {
            // If we have a dataSource, there is no need to initialise
            // the JndiContext
            if (dataSource == null)
            {
                initJndiContext();
                createDataSource();
            }
            // setup property Extractors for queries
            if (queryValueExtractors == null)
            {
                // Add defaults
                queryValueExtractors = new HashSet();
                queryValueExtractors.add(MessagePropertyExtractor.class.getName());
                queryValueExtractors.add(NowPropertyExtractor.class.getName());
                queryValueExtractors.add(PayloadPropertyExtractor.class.getName());
                queryValueExtractors.add(MapPropertyExtractor.class.getName());
                queryValueExtractors.add(BeanPropertyExtractor.class.getName());

                if (ClassUtils.isClassOnPath("org.mule.util.properties.Dom4jPropertyExtractor", getClass()))
                {
                    queryValueExtractors.add("org.mule.util.properties.Dom4jPropertyExtractor");
                }

                if (ClassUtils.isClassOnPath("org.mule.util.properties.JDomPropertyExtractor", getClass()))
                {
                    queryValueExtractors.add("org.mule.util.properties.JDomPropertyExtractor");
                }
            }

            propertyExtractors = new HashSet();
            for (Iterator iterator = queryValueExtractors.iterator(); iterator.hasNext();)
            {
                String s = (String)iterator.next();
                propertyExtractors.add(ClassUtils.instanciateClass(s, ClassUtils.NO_ARGS));
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(CoreMessages.failedToCreate("Jdbc Connector"), e, this);
        }
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnector#getProtocol()
     */
    public String getProtocol()
    {
        return "jdbc";
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
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
        return getServiceDescriptor().createMessageReceiver(this, component, endpoint, params);
    }

    protected void initJndiContext() throws NamingException
    {
        if (this.jndiContext == null)
        {
            Hashtable props = new Hashtable();
            if (this.jndiInitialFactory != null)
            {
                props.put(Context.INITIAL_CONTEXT_FACTORY, this.jndiInitialFactory);
            }
            if (this.jndiProviderUrl != null)
            {
                props.put(Context.PROVIDER_URL, jndiProviderUrl);
            }
            if (this.providerProperties != null)
            {
                props.putAll(this.providerProperties);
            }
            this.jndiContext = new InitialContext(props);
        }

    }

    protected void createDataSource() throws InitialisationException, NamingException
    {
        Object temp = this.jndiContext.lookup(this.dataSourceJndiName);
        if (temp instanceof DataSource)
        {
            dataSource = (DataSource)temp;
        }
        else
        {
            throw new InitialisationException(
                JdbcMessages.jndiResourceNotFound(this.dataSourceJndiName), this);
        }
    }

    public String[] getReadAndAckStatements(UMOImmutableEndpoint endpoint)
    {
        String str;
        // Find read statement
        String readStmt;
        if ((str = (String)endpoint.getProperty("sql")) != null)
        {
            readStmt = str;
        }
        else
        {
            readStmt = endpoint.getEndpointURI().getAddress();
        }
        // Find ack statement
        String ackStmt;
        if ((str = (String)endpoint.getProperty("ack")) != null)
        {
            ackStmt = str;
            if ((str = getQuery(endpoint, ackStmt)) != null)
            {
                ackStmt = str;
            }
        }
        else
        {
            ackStmt = readStmt + ".ack";
            if ((str = getQuery(endpoint, ackStmt)) != null)
            {
                ackStmt = str;
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
        if (!"select".equalsIgnoreCase(readStmt.substring(0, 6)))
        {
            throw new IllegalArgumentException("Read statement should be a select sql statement");
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

    public String getQuery(UMOImmutableEndpoint endpoint, String stmt)
    {
        Object query = null;
        if (endpoint != null && endpoint.getProperties() != null)
        {
            Object queries = endpoint.getProperties().get("queries");
            if (queries instanceof Map)
            {
                query = ((Map)queries).get(stmt);
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

    /**
     * @return Returns the dataSource.
     */
    public DataSource getDataSource()
    {
        return dataSource;
    }

    /**
     * @param dataSource The dataSource to set.
     */
    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
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

    /**
     * @return Returns the dataSourceJndiName.
     */
    public String getDataSourceJndiName()
    {
        return dataSourceJndiName;
    }

    /**
     * @param dataSourceJndiName The dataSourceJndiName to set.
     */
    public void setDataSourceJndiName(String dataSourceJndiName)
    {
        this.dataSourceJndiName = dataSourceJndiName;
    }

    /**
     * @return Returns the jndiContext.
     */
    public Context getJndiContext()
    {
        return jndiContext;
    }

    /**
     * @param jndiContext The jndiContext to set.
     */
    public void setJndiContext(Context jndiContext)
    {
        this.jndiContext = jndiContext;
    }

    /**
     * @return Returns the jndiInitialFactory.
     */
    public String getJndiInitialFactory()
    {
        return jndiInitialFactory;
    }

    /**
     * @param jndiInitialFactory The jndiInitialFactory to set.
     */
    public void setJndiInitialFactory(String jndiInitialFactory)
    {
        this.jndiInitialFactory = jndiInitialFactory;
    }

    /**
     * @return Returns the jndiProviderUrl.
     */
    public String getJndiProviderUrl()
    {
        return jndiProviderUrl;
    }

    /**
     * @param jndiProviderUrl The jndiProviderUrl to set.
     */
    public void setJndiProviderUrl(String jndiProviderUrl)
    {
        this.jndiProviderUrl = jndiProviderUrl;
    }

    /**
     * @return Returns the providerProperties.
     */
    public Map getProviderProperties()
    {
        return providerProperties;
    }

    /**
     * @param providerProperties The providerProperties to set.
     */
    public void setProviderProperties(Map providerProperties)
    {
        this.providerProperties = providerProperties;
    }

    public Connection getConnection() throws Exception
    {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            if (tx.hasResource(dataSource))
            {
                logger.debug("Retrieving connection from current transaction");
                return (Connection)tx.getResource(dataSource);
            }
        }
        logger.debug("Retrieving new connection from data source");
        Connection con = dataSource.getConnection();

        if (tx != null)
        {
            logger.debug("Binding connection to current transaction");
            try
            {
                tx.bindResource(dataSource, con);
            }
            catch (TransactionException e)
            {
                throw new RuntimeException("Could not bind connection to current transaction", e);
            }
        }
        return con;
    }

    /**
     * @return Returns the resultSetHandler.
     */
    public String getResultSetHandler()
    {
        return this.resultSetHandler;
    }

    /**
     * @param resultSetHandler The resultSetHandler class name to set.
     */
    public void setResultSetHandler(String resultSetHandler)
    {
        this.resultSetHandler = resultSetHandler;
    }

    /**
     * @return a new instance of the ResultSetHandler class as defined in the
     *         JdbcConnector
     */
    protected ResultSetHandler createResultSetHandler()
    {
        try
        {
            return (ResultSetHandler) ClassUtils.instanciateClass(getResultSetHandler(),
                                                                  ClassUtils.NO_ARGS);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Error creating instance of the resultSetHandler class :"
                                               + getResultSetHandler() + System.getProperty("line.separator")
                                               + ExceptionUtils.getFullStackTrace(e));
        }
    }

    public Set getQueryValueExtractors()
    {
        return queryValueExtractors;
    }

    public void setQueryValueExtractors(Set queryValueExtractors)
    {
        this.queryValueExtractors = queryValueExtractors;
    }

    /**
     * @return Returns the queryRunner.
     */
    public String getQueryRunner()
    {
        return this.queryRunner;
    }

    /**
     * @param queryRunner The QueryRunner class name to set.
     */
    public void setQueryRunner(String queryRunner)
    {
        this.queryRunner = queryRunner;
    }

    /**
     * @return a new instance of the QueryRunner class as defined in the
     *         JdbcConnector
     */
    protected QueryRunner createQueryRunner()
    {
        try
        {
            return (QueryRunner) ClassUtils.instanciateClass(getQueryRunner(),
                                                             ClassUtils.NO_ARGS);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Error creating instance of the queryRunner class :"
                                               + getQueryRunner() + System.getProperty("line.separator")
                                               + ExceptionUtils.getFullStackTrace(e));
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
            params.add(key);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public Object[] getParams(UMOImmutableEndpoint endpoint, List paramNames, Object message)
        throws Exception
    {
        Object[] params = new Object[paramNames.size()];
        for (int i = 0; i < paramNames.size(); i++)
        {
            String param = (String)paramNames.get(i);
            String name = param.substring(2, param.length() - 1);
            Object value = null;
            // If we find a value and it happens to be null, thats acceptable
            boolean foundValue = false;
            if (message != null)
            {
                for (Iterator iterator = propertyExtractors.iterator(); iterator.hasNext();)
                {
                    PropertyExtractor pe = (PropertyExtractor)iterator.next();
                    value = pe.getProperty(name, message);
                    if (value != null)
                    {
                        if (value.equals(StringUtils.EMPTY) && pe instanceof BeanPropertyExtractor)
                        {
                            value = null;
                        }
                        foundValue = true;
                        break;
                    }
                }
            }
            if (!foundValue)
            {
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
}
