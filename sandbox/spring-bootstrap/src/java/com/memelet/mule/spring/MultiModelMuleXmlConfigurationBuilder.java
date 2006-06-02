package com.memelet.mule.spring;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.mule.MuleManager;
import org.mule.config.ConfigurationException;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.impl.MuleModel;
import org.mule.umo.model.UMOModel;
import org.xml.sax.Attributes;

/**
 * @author <a href="mailto:groups1@memelet.com">Barry Kaplan</a>
 */
class MultiModelMuleXmlConfigurationBuilder extends MuleXmlConfigurationBuilder {
    
    public MultiModelMuleXmlConfigurationBuilder() throws ConfigurationException {
        super();        
    }

    /**
     * This method was copied from super. The only change was to in the addRule surrounded
     * by the BEGIN/END CHANGE block, where we first check if a model already exists before
     * creating a new one.
     */
    @Override
    protected void addModelRules(Digester digester, String path) throws ConfigurationException {
        // Create Model
        path += "/model";

        // memelet: BEGIN CHANGE
        /* TODO For mule-1.1.1+
        digester.addRule(path, new Rule() {
            @Override
            public void begin(String string, String string1, Attributes attributes) throws Exception {
                UMOModel model = MuleManager.getInstance().getModel();
                if (model == null) {
                    String modelType = attributes.getValue("type");
                    if(modelType==null) {
                        modelType = MuleManager.getConfiguration().getModelType();
                    }
                    model = ModelFactory.createModel(modelType);
                }
                digester.push(model);
            }
        });
        */
        digester.addRule(path, new Rule() {
            @Override
            public void begin(String string, String string1, Attributes attributes) throws Exception {
                UMOModel model = MuleManager.getInstance().getModel();
                if (model == null) {
                    model = new MuleModel();
                }
                digester.push(model);
            }
        });
        // memelet: END CHANGE

        addSetPropertiesRule(path, digester);

        digester.addSetRoot(path, "setModel");

        // Create endpointUri resolver
        digester.addObjectCreate(path + "/entry-point-resolver", DEFAULT_ENTRY_POINT_RESOLVER, "className");
        addSetPropertiesRule(path + "/entry-point-resolver", digester);

        digester.addSetNext(path + "/entry-point-resolver", "setEntryPointResolver");

        // Create lifecycle adapter
        digester.addObjectCreate(path + "/component-lifecycle-adapter-factory", DEFAULT_LIFECYCLE_ADAPTER, "className");
        addSetPropertiesRule(path, digester);
        digester.addSetNext(path + "/component-lifecycle-adapter-factory", "setLifecycleAdapterFactory");

        // Pool factory
        addPoolingProfileRules(digester, path);

        // Exception strategy
        addExceptionStrategyRules(digester, path);

        // Add Components
        addMuleDescriptorRules(digester, path);        
    }

}
