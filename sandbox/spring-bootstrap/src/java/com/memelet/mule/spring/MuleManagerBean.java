package com.memelet.mule.spring;

import java.io.IOException;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.impl.MuleModel;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.model.UMOModel;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:groups1@memelet.com">Barry Kaplan</a>
 */
public class MuleManagerBean implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener {

    private Resource[] configResources;
    private SpringContainerContext containerContext;
    private UMOManager muleManager;
    
    public void setConfigResources(Resource[] configResources) {
        this.configResources = configResources;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (value == null) {
            throw new  IllegalArgumentException(name + " property not set");
        }
    }

    public void destroy() throws Exception {
        if (muleManager != null) {
            muleManager.dispose();
            muleManager = null;
        }
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        containerContext = new SpringContainerContext(applicationContext);
    }

    private UMOManager createMuleManager() throws Exception {
        UMOManager muleManager = MuleManager.getInstance();
        muleManager.setContainerContext(containerContext);
        
        // TODO For mule 1.1.1+, we want to create the model with appropriate types and
        // other attributes
        UMOModel model = new MuleModel();
        muleManager.setModel(model);
        
        MuleXmlConfigurationBuilder builder = new MultiModelMuleXmlConfigurationBuilder();    
        String configFilenames = getConfigFilenames();
        builder.configure(configFilenames);
        
        return muleManager;
    }

    private String getConfigFilenames() {
        String[] result = new String[configResources.length];
        for (int i = 0; i < result.length; i++) {
            try {
                result[i] = configResources[i].getURL().getPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return StringUtils.arrayToCommaDelimitedString(result);
    }
    
    public void onApplicationEvent(ApplicationEvent event) {
        if (muleManager == null) {
            try {
                muleManager = createMuleManager();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}