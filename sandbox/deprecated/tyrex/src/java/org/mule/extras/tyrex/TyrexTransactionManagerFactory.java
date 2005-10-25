/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.extras.tyrex;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.manager.UMOTransactionManagerFactory;
import tyrex.tm.RecoveryException;
import tyrex.tm.TransactionDomain;
import tyrex.tm.impl.DomainConfig;
import tyrex.tm.impl.TransactionDomainImpl;

import javax.transaction.TransactionManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * <code>TyrexTransactionManagerFactory</code> is an implementation of <i>UMOTransactionManagerFactory</i>
 * that creates a Tyrex Transaction Manager for use by the Mule Manager
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TyrexTransactionManagerFactory implements UMOTransactionManagerFactory
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(TyrexTransactionManagerFactory.class);

    private String name = "DefaultMuleTM";
    private int timeout = 36000;
    private int maximumTransactions = 50;
    private String domainConfigFile;
    private InputStream domainConfig;
    private TransactionDomain domain;

    /* (non-Javadoc)
     * @see org.mule.transaction.TransactionManagerFactory#create(java.util.HashMap)
   */
    public TransactionManager create() throws Exception
    {
        if (domainConfig != null)
        {
            domain = TransactionDomain.createDomain(domainConfig);
        }
        else
        {
            DomainConfig config = new DomainConfig();
            config.setName(name);
            config.setMaximum(maximumTransactions);
            config.setTimeout(timeout);

            domain = new TransactionDomainImpl(config);
        }

        //Recover all resources used in the domain
        try
        {
            domain.recover();
        }
        catch (RecoveryException exception)
        {
            while (exception != null)
            {
                logger.error("Recovery error: " + exception);
                exception = exception.getNextException();
            }
        }
        return domain.getTransactionManager();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public int getMaximumTransactions()
    {
        return maximumTransactions;
    }

    public void setMaximumTransactions(int maximumTransactions)
    {
        this.maximumTransactions = maximumTransactions;
    }

    public String getDomainConfigFile()
    {
        return domainConfigFile;
    }

    public void setDomainConfigFile(String domainConfigFile) throws FileNotFoundException
    {
        this.domainConfigFile = domainConfigFile;
        domainConfig = new FileInputStream(new File(domainConfigFile));
    }

    public void setDomainConfig(InputStream domainConfig)
    {
        this.domainConfig = domainConfig;
    }

    public TransactionDomain getTransactionDomain()
    {
        return domain;
    }
}
