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
package org.mule.transaction.lookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.manager.UMOTransactionManagerFactory;

import javax.transaction.TransactionManager;

import java.lang.reflect.Method;

/**
 * The code borrowed from Spring's org.springframework.transaction.jta.WebSphereTransactionManagerFactoryBean.
 * See the apache-2.0.license file in Mule's licenses folder for details.
 *
 * @see com.ibm.ws.Transaction.TransactionManagerFactory#getTransactionManager
 * @see com.ibm.ejs.jts.jta.JTSXA#getTransactionManager
 * @see com.ibm.ejs.jts.jta.TransactionManagerFactory#getTransactionManager

 */
public class WebsphereTransactionManagerLookupFactory implements UMOTransactionManagerFactory
{
    private static final String FACTORY_CLASS_5_1 = "com.ibm.ws.Transaction.TransactionManagerFactory";

    private static final String FACTORY_CLASS_5_0 = "com.ibm.ejs.jts.jta.TransactionManagerFactory";

    private static final String FACTORY_CLASS_4 = "com.ibm.ejs.jts.jta.JTSXA";

    private final Log logger = LogFactory.getLog(getClass());

    /**
     * This constructor retrieves the WebSphere TransactionManager factory class,
     * so we can get access to the JTA TransactionManager.
     */
    public TransactionManager create()
    {
        Class clazz;
        TransactionManager transactionManager;
        try {
            logger.debug("Trying WebSphere 5.1: " + FACTORY_CLASS_5_1);
            clazz = Class.forName(FACTORY_CLASS_5_1);
            logger.info("Found WebSphere 5.1: " + FACTORY_CLASS_5_1);
        }
        catch (ClassNotFoundException ex) {
            logger.debug("Could not find WebSphere 5.1 TransactionManager factory class", ex);
            try {
                logger.debug("Trying WebSphere 5.0: " + FACTORY_CLASS_5_0);
                clazz = Class.forName(FACTORY_CLASS_5_0);
                logger.info("Found WebSphere 5.0: " + FACTORY_CLASS_5_0);
            }
            catch (ClassNotFoundException ex2) {
                logger.debug("Could not find WebSphere 5.0 TransactionManager factory class", ex2);
                try {
                    logger.debug("Trying WebSphere 4: " + FACTORY_CLASS_4);
                    clazz = Class.forName(FACTORY_CLASS_4);
                    logger.info("Found WebSphere 4: " + FACTORY_CLASS_4);
                }
                catch (ClassNotFoundException ex3) {
                    logger.debug("Could not find WebSphere 4 TransactionManager factory class", ex3);
                    throw new RuntimeException(
                            "Couldn't find any WebSphere TransactionManager factory class, " +
                                    "neither for WebSphere version 5.1 nor 5.0 nor 4");
                }
            }
        }
        try {
            Method method = clazz.getMethod("getTransactionManager", null);
            transactionManager = (TransactionManager) method.invoke(null, null);
        }
        catch (Exception ex) {
            throw new RuntimeException(
                    "Found WebSphere TransactionManager factory class [" + clazz.getName() +
                            "], but couldn't invoke its static 'getTransactionManager' method", ex);
        }

        return transactionManager;
    }

}
