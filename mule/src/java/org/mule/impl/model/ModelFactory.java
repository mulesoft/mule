/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.impl.model;

import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.model.UMOModel;
import org.mule.util.BeanUtils;
import org.mule.util.ClassHelper;
import org.mule.util.SpiHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Will locate the model service in  META-INF/service using the model type as the
 * key. Then construct the model
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ModelFactory {

    public static final String MODEL_SERVICE_PATH = "org/mule/models";

    public static UMOModel createModel(String type) throws ModelServiceNotFoundException {
        String location = SpiHelper.SERVICE_ROOT + MODEL_SERVICE_PATH;
        InputStream is = SpiHelper.findServiceDescriptor(MODEL_SERVICE_PATH, type, ConnectorFactory.class);
        try {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String clazz = props.getProperty("model");
                try {
                    UMOModel model = (UMOModel) ClassHelper.instanciateClass(clazz, ClassHelper.NO_ARGS, ModelFactory.class );
                    BeanUtils.populateWithoutFail(model, props, false);
                    return model;
                } catch (Exception e) {
                    throw new ModelServiceNotFoundException(location + "/" + type, e);
                }
            } else {
                throw new ModelServiceNotFoundException(location + "/" + type);
            }
        } catch (IOException e) {
            throw new ModelServiceNotFoundException(location + "/" + type, e);
        }
    }
}
