/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config.builders;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;

/**
 * <code>TransformerReference</code> maintains a transformer reference.
 * Transformers are clones when they are looked up, if there are container
 * properties set on the transformer the clone will have an inconsistent state
 * if container properties have not been resolved. This class holds the
 * refernece and is invoked after the thcontainer properties are resolved
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TransformerReference
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(TransformerReference.class);

    private String propertyName;
    private String transformerName;
    private Object object;

    public TransformerReference(String propertyName, String transformerName, Object object)
    {
        this.propertyName = propertyName;
        this.transformerName = transformerName;
        this.object = object;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public String getTransformerName()
    {
        return transformerName;
    }

    public Object getObject()
    {
        return object;
    }

    public void resolveTransformer() throws InitialisationException
    {
        UMOTransformer trans = null;
        try {
            trans = MuleObjectHelper.getTransformer(transformerName, " ");
            if (trans == null) {
                throw new InitialisationException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER, "Transformer '"
                        + transformerName + "'"), object);
            }
            logger.info("Setting transformer: " + transformerName + " on " + object.getClass().getName() + "."
                    + propertyName);

            BeanUtils.setProperty(object, propertyName, trans);
        } catch (InitialisationException e) {
            throw e;
        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.CANT_SET_PROP_X_ON_X_OF_TYPE_X,
                                                          propertyName,
                                                          (object!=null ? object.getClass().getName() : "null"),
                                                          (trans!=null ? trans.getClass().getName() : "null")), e, this);
        }
    }
}
