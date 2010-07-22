
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
        SimpleService simpleService = ConstructBuilders.buildSimpleService()
            .named("test-simple-service")
            .receivingOn("test://foo")
            .transformingInboundRequestsWith(Collections.singleton(new StringAppendTransformer("bar")))
            .serving(EchoComponent.class)
            .in(muleContext);

        assertEquals("test-simple-service", simpleService.getName());
        assertEquals(EchoComponent.class,
            ((AbstractJavaComponent) simpleService.getComponent()).getObjectType());
    }
}
