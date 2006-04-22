package org.mule.test.transformers;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

public class FailingTransformer extends AbstractTransformer {

    protected Object doTransform(Object src, String encoding) throws TransformerException {
        throw new TransformerException(this, new Exception("Wrapped test exception"));
    }
}
