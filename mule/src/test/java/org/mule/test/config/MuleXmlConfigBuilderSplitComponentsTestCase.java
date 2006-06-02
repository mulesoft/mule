package org.mule.test.config;

import org.mule.MuleManager;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.model.UMOModel;

/**
 * Test for MULE-858
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class MuleXmlConfigBuilderSplitComponentsTestCase extends FunctionalTestCase {

    public MuleXmlConfigBuilderSplitComponentsTestCase() {
        super();
        setDisposeManagerPerSuite(true);
    }

    public String getConfigResources() {
        return "split-components-1.xml, split-components-2.xml, split-components-3.xml";
    }

    /**
     * Make sure all the components from all the config files have been created.
     */
    public void testSplitComponentsConfig() throws Exception {
        UMOModel model = MuleManager.getInstance().getModel();
        assertNotNull(model.getComponent("Component1"));
        assertNotNull(model.getComponent("Component2"));
        assertNotNull(model.getComponent("Component3"));
        assertNotNull(model.getComponent("Component4"));
        assertNotNull(model.getComponent("Component5"));
    }
}
