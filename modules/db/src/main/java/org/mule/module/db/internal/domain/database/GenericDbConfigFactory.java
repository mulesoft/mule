/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import static org.mule.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import org.mule.AbstractAnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.NamedObject;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.db.internal.domain.connection.ConnectionCreationException;
import org.mule.module.db.internal.domain.connection.ConnectionFactory;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.connection.RetryConnectionFactory;
import org.mule.module.db.internal.domain.connection.SimpleConnectionFactory;
import org.mule.module.db.internal.domain.connection.TransactionalDbConnectionFactory;
import org.mule.module.db.internal.domain.transaction.TransactionCoordinationDbTransactionManager;
import org.mule.module.db.internal.domain.type.CompositeDbTypeManager;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.domain.type.JdbcTypes;
import org.mule.module.db.internal.domain.type.MappedStructResolvedDbType;
import org.mule.module.db.internal.domain.type.MetadataDbTypeManager;
import org.mule.module.db.internal.domain.type.StaticDbTypeManager;
import org.mule.retry.policies.NoRetryPolicyTemplate;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.namespace.QName;

/**
 * Creates {@link DbConfig} for generic data bases
 */
public class GenericDbConfigFactory implements ConfigurableDbConfigFactory
{

    private class AnnotatedConnectionFactory extends AbstractAnnotatedObject implements ConnectionFactory, NamedObject
    {

        private String name;
        private ConnectionFactory inner;

        public AnnotatedConnectionFactory(String name, ConnectionFactory inner, Map<QName, Object> annotations)
        {
            this.name = name;
            this.inner = inner;
            setAnnotations(annotations);
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public Connection create(DataSource dataSource) throws ConnectionCreationException
        {
            return inner.create(dataSource);
        }

    }

    private List<DbType> customDataTypes;
    private RetryPolicyTemplate retryPolicyTemplate;
    private MuleContext muleContext;

    @Override
    public DbConfig create(String name, Map<QName, Object> annotations, DataSource dataSource)
    {
        ConnectionFactory connectionFactory;

        SimpleConnectionFactory simpleConnectionFactory = new SimpleConnectionFactory(createTypeMapping());
        if (retryPolicyTemplate == null)
        {
            retryPolicyTemplate = getDefaultRetryPolicyTemplate();
        }
        if (retryPolicyTemplate == null)
        {
            connectionFactory = simpleConnectionFactory;
        }
        else
        {
            connectionFactory = new RetryConnectionFactory(retryPolicyTemplate, new AnnotatedConnectionFactory(name, simpleConnectionFactory, annotations));
        }

        DbTypeManager dbTypeManager = doCreateTypeManager();
        DbConnectionFactory dbConnectionFactory = createDbConnectionFactory(dataSource, connectionFactory, dbTypeManager);

        return doCreateDbConfig(dataSource, dbTypeManager, dbConnectionFactory, name);
    }

    /**
     * Creates the {@link DbConnectionFactory} to use on the created {@link DbConfig}
     *
     * @param dataSource datasource used on the DB config.
     * @param connectionFactory creates the connections delegates for the created factory.
     * @param dbTypeManager manages types provided on the created connections.
     * @return a non null instance.
     */
    protected DbConnectionFactory createDbConnectionFactory(DataSource dataSource, ConnectionFactory connectionFactory, DbTypeManager dbTypeManager)
    {
        return new TransactionalDbConnectionFactory(new TransactionCoordinationDbTransactionManager(), dbTypeManager, connectionFactory, dataSource);
    }

    private Map<String, Class<?>> createTypeMapping()
    {
        final Map<String, Class<?>> typeMapping = new HashMap<>();

        for (DbType dbType : customDataTypes)
        {
            if (dbType instanceof MappedStructResolvedDbType)
            {
                final MappedStructResolvedDbType structDbType = (MappedStructResolvedDbType) dbType;
                if (structDbType.getMappedClass() != null)
                {
                    typeMapping.put(structDbType.getName(), structDbType.getMappedClass());
                }
            }
        }

        return typeMapping;
    }

    protected DbConfig doCreateDbConfig(DataSource datasource, DbTypeManager dbTypeManager, DbConnectionFactory dbConnectionFactory, String name)
    {
        return new GenericDbConfig(datasource, name, dbTypeManager, dbConnectionFactory);
    }

    protected DbTypeManager doCreateTypeManager()
    {
        List<DbTypeManager> typeManagers = new ArrayList<>();

        typeManagers.add(new MetadataDbTypeManager());

        if (customDataTypes.size() > 0)
        {
            typeManagers.add(new StaticDbTypeManager(customDataTypes));
        }

        List<DbType> vendorDataTypes = getVendorDataTypes();
        if (vendorDataTypes.size() > 0)
        {
            typeManagers.add(new StaticDbTypeManager(vendorDataTypes));
        }

        typeManagers.add(new StaticDbTypeManager(JdbcTypes.types));

        return new CompositeDbTypeManager(typeManagers);
    }

    protected List<DbType> getVendorDataTypes()
    {
        return Collections.EMPTY_LIST;
    }

    public void setCustomDataTypes(List<DbType> customDataTypes)
    {
        this.customDataTypes = customDataTypes;
    }

    public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    protected RetryPolicyTemplate getDefaultRetryPolicyTemplate ()
    {
        RetryPolicyTemplate retryPolicyTemplate = muleContext.getRegistry().lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE);
        return retryPolicyTemplate instanceof NoRetryPolicyTemplate ? null : retryPolicyTemplate;
    }

}
