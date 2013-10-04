/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transaction.lookup;

import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The code borrowed from Spring's
 * org.springframework.transaction.jta.WebSphereTransactionManagerFactoryBean. See
 * the apache-2.0.license file in Mule's licenses folder for details.
 *
 * @see com.ibm.ws.Transaction.TransactionManagerFactory#getTransactionManager
 * @see com.ibm.ejs.jts.jta.JTSXA#getTransactionManager
 * @see com.ibm.ejs.jts.jta.TransactionManagerFactory#getTransactionManager
 */
public class WebsphereTransactionManagerLookupFactory implements TransactionManagerFactory
{
    private static final String FACTORY_CLASS_5_1_AND_ABOVE = "com.ibm.ws.Transaction.TransactionManagerFactory";

    private static final String FACTORY_CLASS_5_0 = "com.ibm.ejs.jts.jta.TransactionManagerFactory";

    private static final String FACTORY_CLASS_4 = "com.ibm.ejs.jts.jta.JTSXA";

    private final Log logger = LogFactory.getLog(getClass());

    /**
     * This constructor retrieves the WebSphere TransactionManager factory class, so
     * we can get access to the JTA TransactionManager.
     */
    public TransactionManager create(MuleConfiguration config)
    {
        Class<?> clazz;
        TransactionManager transactionManager;
        try
        {
            logger.debug("Trying WebSphere 5.1+: " + FACTORY_CLASS_5_1_AND_ABOVE);
            clazz = ClassUtils.loadClass(FACTORY_CLASS_5_1_AND_ABOVE, this.getClass());
            logger.info("Found WebSphere 5.1+: " + FACTORY_CLASS_5_1_AND_ABOVE);
        }
        catch (ClassNotFoundException ex)
        {
            logger.debug("Could not find WebSphere 5.1+ TransactionManager factory class", ex);
            try
            {
                logger.debug("Trying WebSphere 5.0: " + FACTORY_CLASS_5_0);
                clazz = ClassUtils.loadClass(FACTORY_CLASS_5_0, this.getClass());
                logger.info("Found WebSphere 5.0: " + FACTORY_CLASS_5_0);
            }
            catch (ClassNotFoundException ex2)
            {
                logger.debug("Could not find WebSphere 5.0 TransactionManager factory class", ex2);
                try
                {
                    logger.debug("Trying WebSphere 4: " + FACTORY_CLASS_4);
                    clazz = ClassUtils.loadClass(FACTORY_CLASS_4, this.getClass());
                    logger.info("Found WebSphere 4: " + FACTORY_CLASS_4);
                }
                catch (ClassNotFoundException ex3)
                {
                    logger.debug("Could not find WebSphere 4 TransactionManager factory class", ex3);
                    throw new MuleRuntimeException(
                        CoreMessages.createStaticMessage("Couldn't find any WebSphere TransactionManager factory class, "
                                                         + "neither for WebSphere version 5.1 nor 5.0 nor 4"),
                        ex);
                }
            }
        }
        try
        {
            Method method = clazz.getMethod("getTransactionManager", (Class[])null);
            transactionManager = (TransactionManager) method.invoke(null, (Object[])null);
        }
        catch (Exception ex)
        {
            throw new MuleRuntimeException(
                CoreMessages.createStaticMessage("Found WebSphere TransactionManager factory class ["
                                                 + clazz.getName()
                                                 + "], but couldn't invoke its static 'getTransactionManager' method"),
                ex);
        }

        return transactionManager;
    }
}
