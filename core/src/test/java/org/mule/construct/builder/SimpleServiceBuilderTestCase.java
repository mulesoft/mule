
package org.mule.construct.builder;

import java.util.Collections;

import org.mule.component.AbstractJavaComponent;
import org.mule.component.simple.EchoComponent;
import org.mule.construct.SimpleService;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

public class SimpleServiceBuilderTestCase extends AbstractMuleTestCase
{
    public void testFullConfiguration() throws Exception
    {
        SimpleService simpleService = ConstructBuilders.newSimpleService()
            .name("test-simple-service")
            .inboundAddress("test://foo")
            .inboundTransformers(Collections.singleton(new StringAppendTransformer("bar")))
            .component(EchoComponent.class)
            .build(muleContext);

        assertEquals("test-simple-service", simpleService.getName());
        assertEquals(EchoComponent.class,
            ((AbstractJavaComponent) simpleService.getComponent()).getObjectType());
    }
}
