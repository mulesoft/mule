package org.springmodules.workflow.jbpm31;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

/**
 * This class is an ugly workaround for the issue described in
 * http://forum.springframework.org/showthread.php?p=87189
 */
public class LocalJbpmConfigurationFactoryBeanWorkaround  extends LocalJbpmConfigurationFactoryBean{

    public void setBeanFactory(BeanFactory arg0) throws BeansException {
        // Disable parent method
    }

    public void setBeanName(String arg0) {
        // Disable parent method
    }
}
