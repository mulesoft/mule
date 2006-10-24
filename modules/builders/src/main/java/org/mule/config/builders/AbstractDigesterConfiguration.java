/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.ConfigurationException;
import org.mule.config.MuleDtdResolver;
import org.mule.config.MuleProperties;
import org.mule.config.ReaderResource;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.container.MuleContainerContext;
import org.mule.umo.UMOFilter;
import org.mule.util.IOUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A base classs for configuration schemes that use digester to parse the documents.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractDigesterConfiguration
{
    public static final String DEFAULT_CONTAINER_CONTEXT = MuleContainerContext.class.getName();
    public static final String FILTER_INTERFACE = UMOFilter.class.getName();

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected Digester digester;
    protected List containerReferences = new ArrayList();
    protected String configEncoding;

    protected AbstractDigesterConfiguration(boolean validate, String dtd)
    {
        // This is a hack to stop Digester spitting out unnecessary warnings
        // when there is a customer error handler registered
        digester = new Digester()
        {
            public void warning(SAXParseException e) throws SAXException
            {
                if (errorHandler != null)
                {
                    errorHandler.warning(e);
                }
            }
        };

        configEncoding = System.getProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY,
            MuleManager.getConfiguration().getEncoding());

        digester.setValidating(validate);
        digester.setEntityResolver(new MuleDtdResolver(dtd));

        digester.setErrorHandler(new ErrorHandler()
        {
            public void error(SAXParseException exception) throws SAXException
            {
                logger.error(exception.getMessage(), exception);
                throw new SAXException(exception);
            }

            public void fatalError(SAXParseException exception) throws SAXException
            {
                logger.fatal(exception.getMessage(), exception);
                throw new SAXException(exception);
            }

            public void warning(SAXParseException exception)
            {
                logger.warn(exception.getMessage());
            }
        });
    }

    // protected ReaderResource[] parseResources(String configResources) throws
    // ConfigurationException {
    // String[] resources = Utility.split(configResources, ",");
    // MuleManager.getConfiguration().setConfigResources(resources);
    // ReaderResource[] readers = new ReaderResource[resources.length];
    // for (int i = 0; i < resources.length; i++) {
    // try {
    // readers[i] = new ReaderResource(resources[i].trim(),
    // new InputStreamReader(loadConfig(resources[i].trim()), "UTF-8"));
    // } catch (UnsupportedEncodingException e) {
    // throw new ConfigurationException(e);
    // }
    // }
    // return readers;
    // }

    protected Object process(ReaderResource[] configResources) throws ConfigurationException
    {
        Object result = null;
        Reader configResource;
        for (int i = 0; i < configResources.length; i++)
        {
            try
            {
                configResource = configResources[i].getReader();
                result = digester.parse(configResource);
            }
            catch (Exception e)
            {
                throw new ConfigurationException(new Message(Messages.FAILED_TO_PARSE_CONFIG_RESOURCE_X,
                    configResources[i].getDescription()), e);
            }
        }

        return result;
    }

    /**
     * Attempt to load a configuration resource from the file system, classpath, or
     * as a URL, in that order.
     * 
     * @param configResource Mule configuration resources
     * @return an InputStream to the resource
     * @throws ConfigurationException if the resource could not be loaded by any
     *             means
     */
    protected InputStream loadConfig(String configResource) throws ConfigurationException
    {
        InputStream is = null;
        try
        {
            is = IOUtils.getResourceAsStream(configResource, getClass());
        }
        catch (IOException e)
        {
            throw new ConfigurationException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE,
                configResource), e);
        }

        if (is != null)
        {
            return is;
        }
        else
        {
            throw new ConfigurationException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE,
                configResource));
        }
    }

    public abstract String getRootName();

    protected void addContainerContextRules(String path, String setterMethod, int parentIndex)
        throws ConfigurationException
    {
        // Create container Context
        digester.addObjectCreate(path, DEFAULT_CONTAINER_CONTEXT, "className");
        digester.addSetProperties(path, "name", "name");
        addMulePropertiesRule(path, digester);

        // Set the container on the parent object
        digester.addSetNext(path, setterMethod);
    }

    protected void addServerPropertiesRules(String path, String setterMethod, int parentIndex)
    {
        // Set environment properties
        int i = path.lastIndexOf("/");
        addMulePropertiesRule(path.substring(0, i), digester, setterMethod, path.substring(i + 1));
    }

    protected void addSetPropertiesRule(String path, Digester digester)
    {
        digester.addRule(path, new MuleSetPropertiesRule());
    }

    protected void addSetPropertiesRule(String path, Digester digester, String[] s1, String[] s2)
    {
        digester.addRule(path, new MuleSetPropertiesRule(s1, s2));
    }

    protected void addMulePropertiesRule(String path, Digester digester)
    {
        digester.addRuleSet(new MulePropertiesRuleSet(path, containerReferences));
    }

    protected void addMulePropertiesRule(String path, Digester digester, String propertiesSetter)
    {
        digester.addRuleSet(new MulePropertiesRuleSet(path, propertiesSetter, containerReferences));
    }

    protected void addMulePropertiesRule(String path,
                                         Digester digester,
                                         String propertiesSetter,
                                         String parentElement)
    {
        digester.addRuleSet(new MulePropertiesRuleSet(path, propertiesSetter, containerReferences,
            parentElement));
    }

    protected void addFilterRules(Digester digester, String path) throws ConfigurationException
    {
        // three levels
        addSingleFilterRule(digester, path);
        path += "/filter";
        addFilterGroupRule(digester, path);

        addFilterGroupRule(digester, path + "/left-filter");
        addFilterGroupRule(digester, path + "/right-filter");
        addFilterGroupRule(digester, path + "/filter");

        addFilterGroupRule(digester, path + "/left-filter/left-filter");
        addFilterGroupRule(digester, path + "/left-filter/right-filter");
        addFilterGroupRule(digester, path + "/left-filter/filter");

        addFilterGroupRule(digester, path + "/right-filter/left-filter");
        addFilterGroupRule(digester, path + "/right-filter/right-filter");
        addFilterGroupRule(digester, path + "/right-filter/filter");

        addFilterGroupRule(digester, path + "/filter/left-filter");
        addFilterGroupRule(digester, path + "/filter/right-filter");
        addFilterGroupRule(digester, path + "/filter/filter");

        // digester.addSetNext(path, "setFilter");
    }

    protected void addFilterGroupRule(Digester digester, String path) throws ConfigurationException
    {
        addLeftFilterRule(digester, path);
        addRightFilterRule(digester, path);
        addSingleFilterRule(digester, path);
    }

    protected void addLeftFilterRule(Digester digester, String path) throws ConfigurationException
    {
        path += "/left-filter";
        digester.addObjectCreate(path, FILTER_INTERFACE, "className");
        addSetPropertiesRule(path, digester);
        addMulePropertiesRule(path, digester);
        digester.addSetNext(path, "setLeftFilter");
    }

    protected void addRightFilterRule(Digester digester, String path) throws ConfigurationException
    {
        path += "/right-filter";
        digester.addObjectCreate(path, FILTER_INTERFACE, "className");
        addSetPropertiesRule(path, digester);
        addMulePropertiesRule(path, digester);
        digester.addSetNext(path, "setRightFilter");
    }

    protected void addSingleFilterRule(Digester digester, String path) throws ConfigurationException
    {
        path += "/filter";
        digester.addObjectCreate(path, FILTER_INTERFACE, "className");
        addSetPropertiesRule(path, digester);
        addMulePropertiesRule(path, digester);
        digester.addSetNext(path, "setFilter");
    }
}
