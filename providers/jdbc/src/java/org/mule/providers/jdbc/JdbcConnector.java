/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jdbc;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.mule.InitialisationException;
import org.mule.providers.TransactionEnabledConnector;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcConnector extends TransactionEnabledConnector {

    private long pollingFrequency = 0;
    private Object dataSource;
    private String dataSourceJndiName;
    private Context jndiContext;
    private String jndiInitialFactory;
    private String jndiProviderUrl;
    private Map providerProperties;
	private Map queries;
	
	/* (non-Javadoc)
	 * @see org.mule.umo.provider.UMOConnector#getProtocol()
	 */
	public String getProtocol() {
		return "jdbc";
	}

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
    	String[] params = getReadAndAckStatements(endpoint.getEndpointURI(), endpoint);
        return getServiceDescriptor().createMessageReceiver(this, component, endpoint, params);
    }
	
    protected void initJndiContext() throws NamingException
    {
        if (this.jndiContext == null) {
            Hashtable props = new Hashtable();
            if (this.jndiInitialFactory != null) {
                props.put(Context.INITIAL_CONTEXT_FACTORY, this.jndiInitialFactory);
            }
            if (this.jndiProviderUrl != null) {
                props.put(Context.PROVIDER_URL, jndiProviderUrl);
            }
            if (this.providerProperties != null) {
                props.putAll(this.providerProperties);
            }
            this.jndiContext = new InitialContext(props);
        }
    }

    protected void createDataSource() throws InitialisationException, NamingException
    {
        Object temp =  this.jndiContext.lookup(this.dataSourceJndiName);
        if (temp instanceof DataSource || temp instanceof XADataSource) {
            dataSource = temp;
        } else {
            throw new InitialisationException("No Connection factory was found for name: " + this.dataSourceJndiName);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#create(java.util.HashMap)
     */
    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        try
        {
            //If we have a dataSource, there is no need to initialise
            //the JndiContext
            if (dataSource == null) {
                initJndiContext();
                createDataSource();
            }
        } catch (Exception e) {
            throw new InitialisationException("Failed to create Jdbc Connector: " + e.getMessage(), e);
        }
    }

    public String[] getReadAndAckStatements(UMOEndpointURI endpointUri, UMOEndpoint endpoint) {
		String str;
		// Find read statement
    	String readStmt = null;
		if ((str = endpointUri.getParams().getProperty("sql")) != null) {
    		readStmt = str;
    	} else {
    		readStmt = endpointUri.getAddress();
    	}
		// Find ack statement
		String ackStmt = null;
		if ((str = (String) endpointUri.getParams().getProperty("ack")) != null) {
			ackStmt = str;
			if ((str = getQuery(endpoint, ackStmt)) != null) {
				ackStmt = str;
			}
		} else {
			ackStmt = readStmt + ".ack";
			if ((str = (String) getQuery(endpoint, ackStmt)) != null) {
				ackStmt = str;
			} else {
				ackStmt = null;
			}
		}
		// Translate both using queries map
		if ((str = (String) getQuery(endpoint, readStmt)) != null) {
    		readStmt = str;
    	}
		if (readStmt == null) {
			throw new IllegalArgumentException("Read statement should not be null");
		}
		if (!"select".equalsIgnoreCase(readStmt.substring(0, 6))) {
			throw new IllegalArgumentException("Read statement should be a select sql statement");
		}
		if (ackStmt != null) {
			if (!"insert".equalsIgnoreCase(ackStmt.substring(0, 6)) &&
				!"update".equalsIgnoreCase(ackStmt.substring(0, 6)) &&
				!"delete".equalsIgnoreCase(ackStmt.substring(0, 6))) {
				throw new IllegalArgumentException("Ack statement should be an insert / update / delete sql statement");
			}
		}
    	return new String[] { readStmt, ackStmt };
    }
    
	public String getQuery(UMOEndpoint endpoint, String stmt) {
		Object query = null;
		if (endpoint != null && endpoint.getProperties() != null) {
			Object queries = endpoint.getProperties().get("queries");
			if (queries instanceof Map) {
				query = ((Map) queries).get(stmt);
			}
		}
		if (query == null) {
			if (this.queries != null) {
				query = this.queries.get(stmt);
			}
		}
		return query == null ? null : query.toString();
	}
	
	/**
	 * @return Returns the dataSource.
	 */
	public Object getDataSource() {
		return dataSource;
	}
	/**
	 * @param dataSource The dataSource to set.
	 */
	public void setDataSource(Object dataSource) {
		if (dataSource instanceof DataSource || dataSource instanceof XADataSource) {
			this.dataSource = dataSource;
		} else {
			throw new IllegalArgumentException("dataSource should be an instance of DataSource of XADataSource");
		}
	}
	/**
	 * @return Returns the pollingFrequency.
	 */
	public long getPollingFrequency() {
		return pollingFrequency;
	}
	/**
	 * @param pollingFrequency The pollingFrequency to set.
	 */
	public void setPollingFrequency(long pollingFrequency) {
		this.pollingFrequency = pollingFrequency;
	}
	/**
	 * @return Returns the queries.
	 */
	public Map getQueries() {
		return queries;
	}
	/**
	 * @param queries The queries to set.
	 */
	public void setQueries(Map queries) {
		this.queries = queries;
	}
	/**
	 * @return Returns the dataSourceJndiName.
	 */
	public String getDataSourceJndiName() {
		return dataSourceJndiName;
	}
	/**
	 * @param dataSourceJndiName The dataSourceJndiName to set.
	 */
	public void setDataSourceJndiName(String dataSourceJndiName) {
		this.dataSourceJndiName = dataSourceJndiName;
	}
	/**
	 * @return Returns the jndiContext.
	 */
	public Context getJndiContext() {
		return jndiContext;
	}
	/**
	 * @param jndiContext The jndiContext to set.
	 */
	public void setJndiContext(Context jndiContext) {
		this.jndiContext = jndiContext;
	}
	/**
	 * @return Returns the jndiInitialFactory.
	 */
	public String getJndiInitialFactory() {
		return jndiInitialFactory;
	}
	/**
	 * @param jndiInitialFactory The jndiInitialFactory to set.
	 */
	public void setJndiInitialFactory(String jndiInitialFactory) {
		this.jndiInitialFactory = jndiInitialFactory;
	}
	/**
	 * @return Returns the jndiProviderUrl.
	 */
	public String getJndiProviderUrl() {
		return jndiProviderUrl;
	}
	/**
	 * @param jndiProviderUrl The jndiProviderUrl to set.
	 */
	public void setJndiProviderUrl(String jndiProviderUrl) {
		this.jndiProviderUrl = jndiProviderUrl;
	}
	/**
	 * @return Returns the providerProperties.
	 */
	public Map getProviderProperties() {
		return providerProperties;
	}
	/**
	 * @param providerProperties The providerProperties to set.
	 */
	public void setProviderProperties(Map providerProperties) {
		this.providerProperties = providerProperties;
	}

	/* (non-Javadoc)
	 * @see org.mule.providers.TransactionEnabledConnector#getSession(org.mule.umo.endpoint.UMOEndpoint)
	 */
	public Object getSession(UMOEndpoint endpoint) throws Exception {
		if (endpoint.getTransactionConfig() != null &&
			endpoint.getTransactionConfig().getFactory() instanceof XaTransactionFactory) {
			if (dataSource instanceof XADataSource) {
				return ((XADataSource) dataSource).getXAConnection();
			} else {
				throw new IllegalStateException("dataSource should be an instance of XADataSource");
			}
		} else {
			if (dataSource instanceof DataSource) {
				Connection con = ((DataSource) dataSource).getConnection();
				con.setAutoCommit(false);
				return con;
			} else {
				throw new IllegalStateException("dataSource should be an instance of DataSource");
			}
		}
	}

	/**
	 * Parse the given statement filling the parameter list and return
	 * the ready to use statement.
	 * 
	 * @param stmt
	 * @param params
	 * @return
	 */
	public static String parseStatement(String stmt, List params) {
		if (stmt == null) {
			return stmt;
		}
		Pattern p = Pattern.compile("\\$\\{[^\\}]*\\}");
		Matcher m = p.matcher(stmt);
		StringBuffer sb = new StringBuffer();
	    while (m.find()) {
	    	String key = m.group();
	    	m.appendReplacement(sb, "?");
	    	params.add(key);
	    }
	    m.appendTail(sb);
	    return sb.toString();
	}
	
	public static Object[] getParams(UMOEndpointURI uri, List paramNames, Object root) throws Exception {
		Object[] params = new Object[paramNames.size()];
		for (int i = 0; i < paramNames.size(); i++) {
			String param = (String) paramNames.get(i);
			String name  = param.substring(2, param.length() - 1);
			Object value = null;
			if ("NOW".equals(name)) {
				value = new Timestamp(Calendar.getInstance().getTimeInMillis());
			} else {
				try {
					value = BeanUtils.getProperty(root, name);
				} catch (Exception ignored) {
				}
			}
			if (value == null) {
				value = uri.getParams().getProperty(name);
			}
			if (value == null) {
				throw new IllegalArgumentException("Can not retrieve argument " + name);
			}
			params[i] = value;
		}
		return params;
	}
	
}
