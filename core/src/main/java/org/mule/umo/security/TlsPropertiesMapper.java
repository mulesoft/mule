/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TlsPropertiesMapper
{

    private static final String TRUST_NAME_SUFFIX = ".ssl.trustStore";
    private static final String TRUST_TYPE_SUFFIX = ".ssl.trustStoreType";
    private static final String TRUST_PASSWORD_SUFFIX = ".ssl.trustStorePassword";
    private static final String TRUST_ALGORITHM_SUFFIX = ".ssl.trustManagerAlgorithm";

    private static final String KEY_NAME_SUFFIX = ".ssl.keyStore";
    private static final String KEY_TYPE_SUFFIX = ".ssl.keyStoreType";
    private static final String KEY_PASSWORD_SUFFIX = ".ssl.keyStorePassword";

    private Log logger = LogFactory.getLog(getClass());
    private String namespace;

    public TlsPropertiesMapper(String namespace)
    {
        this.namespace = namespace;
    }

    public void writeToProperties(Properties properties, TlsConfiguration configuration)
    {
        writeTrustStoreToProperties(properties, configuration);
        writeKeyStoreToProperties(properties, configuration);
    }

    public void readFromProperties(TlsConfiguration configuration, Properties properties) throws IOException
    {
        readTrustStoreFromProperties(configuration, properties);
        readKeyStoreFromProperties(configuration, properties);
    }

    private void writeTrustStoreToProperties(Properties properties, TlsConfiguration configuration)
    {
        String trustStoreName = configuration.getTrustStore();
        String trustStorePassword = configuration.getTrustStorePassword();

        if (null == trustStoreName && ! configuration.isExplicitTrustStoreOnly())
        {
            logger.info("Defaulting " + namespace + " trust store to client Key Store");
            trustStoreName = configuration.getClientKeyStore();
            trustStorePassword = configuration.getClientKeyStorePassword();
        }
        if (null != trustStoreName)
        {
            synchronized(properties)
            {
                setProperty(properties, TRUST_NAME_SUFFIX, trustStoreName);
                setProperty(properties, TRUST_TYPE_SUFFIX, configuration.getTrustStoreType());
                setProperty(properties, TRUST_PASSWORD_SUFFIX, trustStorePassword);
                setProperty(properties, TRUST_ALGORITHM_SUFFIX, configuration.getTrustManagerAlgorithm());
            }
            logger.debug("Set Trust Store: " + namespace + TRUST_NAME_SUFFIX + " = " + trustStoreName);
        }
    }

    private void readTrustStoreFromProperties(TlsConfiguration configuration, Properties properties) 
    throws IOException
    {
        configuration.setTrustStore(
            getProperty(properties, TRUST_NAME_SUFFIX, configuration.getTrustStore()));
        configuration.setTrustStoreType(
            getProperty(properties, TRUST_TYPE_SUFFIX, configuration.getTrustStoreType()));
        configuration.setTrustStorePassword(
            getProperty(properties, TRUST_PASSWORD_SUFFIX, configuration.getTrustStorePassword()));
        configuration.setTrustManagerAlgorithm(
            getProperty(properties, TRUST_ALGORITHM_SUFFIX, configuration.getTrustManagerAlgorithm()));
    }

    private void writeKeyStoreToProperties(Properties properties, TlsConfiguration configuration) 
    {
        if (null != configuration.getClientKeyStore())
        {
            synchronized (properties)
            {
                setProperty(properties, KEY_NAME_SUFFIX, configuration.getClientKeyStore());
                setProperty(properties, KEY_TYPE_SUFFIX, configuration.getClientKeyStoreType());
                setProperty(properties, KEY_PASSWORD_SUFFIX, configuration.getClientKeyStorePassword());
            }
            logger.info("Set Key Store: " + namespace + KEY_NAME_SUFFIX + " = " + configuration.getClientKeyStore());
        }
    }

    // note the asymmetry here.  this preserves the semantics of the original implementation.

    // originally, the "client" keystore data were written to system properties (only) and
    // used implicitly to construct sockets, while "non-client" keystore information was
    // used explicitly.

    // now we construct some of those implicit sockets explicitly (as part of avoiding global
    // configuration for tls across all transports).  in these cases we read the data needed
    // from (namespaced) proeprties.  if we read that information back into "non-client" keystore
    // data, even though it was written from "client" data, then we can use the same code in
    // TlsConfiguration to generate the sockets in both cases.

    private void readKeyStoreFromProperties(TlsConfiguration configuration, Properties properties) 
    throws IOException 
    {
        configuration.setKeyStore(
            getProperty(properties, KEY_NAME_SUFFIX, configuration.getKeyStore()));
        configuration.setKeystoreType(
            getProperty(properties, KEY_TYPE_SUFFIX, configuration.getKeystoreType()));
        configuration.setStorePassword(
            getProperty(properties, KEY_PASSWORD_SUFFIX, configuration.getStorePassword()));
    }


    private void setProperty(Properties properties, String suffix, String value)
    {
        if (null != value)
        {
            properties.setProperty(namespace + suffix, value);
        }
    }

    private String getProperty(Properties properties, String suffix, String deflt)
    {
        String value = properties.getProperty(namespace + suffix);
        if (null == value)
        {
            return deflt;
        }
        else
        {
            return value;
        }
    }

}


