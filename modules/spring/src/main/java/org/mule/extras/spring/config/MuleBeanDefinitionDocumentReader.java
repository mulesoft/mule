/*
 * $Id:MuleBeanDefinitionDocumentReader.java 7693 2007-07-31 20:39:11Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.config;

import java.io.IOException;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;
import org.w3c.dom.Element;

/**
 * By default, an <import resource="file.xml"/> statement will assume the imported resource's location is relative
 * to the current file.  Override to try loading the resource from the classpath as well.
 */
public class MuleBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader
{
    /**
     * By default, an <import resource="file.xml"/> statement will assume the imported resource's location is relative
     * to the current file.  Override this method to try loading the resource from the classpath as well.
     * <p/>
     * Note: only the section labelled "Customized for Mule" below has been changed, the rest is copy-pasted from the
     * parent (Spring) class.
     */
    //@Override
    protected void importBeanDefinitionResource(Element ele)
    {
        String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
        if (!StringUtils.hasText(location))
        {
            getReaderContext().error("Resource location must not be empty", ele);
            return;
        }

        // Resolve system properties: e.g. "${user.dir}"
        location = SystemPropertyUtils.resolvePlaceholders(location);

        if (ResourcePatternUtils.isUrl(location))
        {
            try
            {
                int importCount = getReaderContext().getReader().loadBeanDefinitions(location);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
                }
            }
            catch (BeanDefinitionStoreException ex)
            {
                getReaderContext().error(
                        "Failed to import bean definitions from URL location [" + location + "]", ele, ex);
            }
        }
        else
        {
            // No URL -> considering resource location as relative to the current file.
            try
            {
                Resource relativeResource = getReaderContext().getResource().createRelative(location);
                int importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
                }
            }
            ////////////////////////////////////////////////////////////////////////////////////////////
            // Customized for Mule
            ////////////////////////////////////////////////////////////////////////////////////////////
            catch (IOException ex)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Invalid relative resource location [" + location + "] to import bean definitions from, will try loading from classpath");
                }
                Resource classpathResource = new ClassPathResource(location);
                int importCount = getReaderContext().getReader().loadBeanDefinitions(classpathResource);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Imported " + importCount + " bean definitions from classpath resource [" + location + "]");
                }
            }
            catch (BeanDefinitionStoreException ex)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Failed to import bean definitions from relative location [" + location + "], will try loading from classpath");
                }
                Resource classpathResource = new ClassPathResource(location);
                int importCount = getReaderContext().getReader().loadBeanDefinitions(classpathResource);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Imported " + importCount + " bean definitions from classpath resource [" + location + "]");
                }
            }
            ////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////
        }

        getReaderContext().fireImportProcessed(location, extractSource(ele));
    }
}


