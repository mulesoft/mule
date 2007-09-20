/*
 * $Id:LocalJbpmConfigurationFactoryBeanWorkaround.java 5924 2007-04-07 21:12:55 +0000 (Sat, 07 Apr 2007) tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.springmodules.workflow.jbpm31;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

/**
 * This class is an ugly workaround for the issue described in http://forum.springframework.org/showthread.php?p=87189
 * http://opensource.atlassian.com/projects/spring/browse/MOD-266
 * <p/>
 * This was supposedly fixed in springmodules 0.7 but the problem still remains in 0.8!
 */
public class LocalJbpmConfigurationFactoryBeanWorkaround extends LocalJbpmConfigurationFactoryBean
{

    public void setBeanFactory(BeanFactory arg0) throws BeansException
    {
        // Disable parent method
    }

    public void setBeanName(String arg0)
    {
        // Disable parent method
    }
}
