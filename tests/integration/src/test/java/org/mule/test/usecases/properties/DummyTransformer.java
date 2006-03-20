
package org.mule.test.usecases.properties;

import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

public class DummyTransformer extends AbstractEventAwareTransformer
{

    public Object transform(Object src, String encoding, UMOEventContext context)
            throws TransformerException
    {
        System.out.println("org.mule.test.usecases.props.DummyTransformer");

        PropsComponent.assertEquals("param1", context.getMessage().getProperty("stringParam"));
        PropsComponent.assertEquals(PropsComponent.testObjectProperty,
                context.getMessage().getProperty("objectParam"));

        System.out.println("org.mule.test.usecases.props.DummyTransformer done.");
        return src;
    }
}
