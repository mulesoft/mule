package com.memelet.mule.spring;

import java.io.Reader;

import org.mule.impl.container.AbstractContainerContext;
import org.mule.umo.manager.ObjectNotFoundException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author <a href="mailto:groups1@memelet.com">Barry Kaplan</a>
 */
class SpringContainerContext extends AbstractContainerContext {

    private final BeanFactory beanFactory;

    public SpringContainerContext(BeanFactory beanFactory) {
        super("spring");
        this.beanFactory = beanFactory;
    }

    @Override
    public void configure(Reader configuration) {
        // nothing to do
    }

    /**
     * Mostly copied from org.mule.extras.spring.SpringContainerContext
     */
    public Object getComponent(Object key) throws ObjectNotFoundException {
        if (key == null) {
            throw new ObjectNotFoundException("Component not found for null key");
        }

        if (key instanceof Class) {
            // We will assume that there should only be one object of
            // this class in the container for now
            // String[] names = getBeanFactory().getBeanDefinitionNames((Class)
            // key);
            // if (names == null || names.length == 0 || names.length > 1)
            // {
            throw new ObjectNotFoundException(
                    "The container is unable to build single instance of "
                            + ((Class) key).getName() + " number of instances found was: 0");
            // }
            // else
            // {
            // key = names[0];
            // }
        }
        try {
            return beanFactory.getBean(key.toString());
        } catch (BeansException e) {
            throw new ObjectNotFoundException("Component not found for key: " + key.toString(), e);
        }
    }

}
