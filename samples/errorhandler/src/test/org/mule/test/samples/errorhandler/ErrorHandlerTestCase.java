/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.test.samples.errorhandler;

import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.samples.errorhandler.ExceptionBean;
import org.mule.samples.errorhandler.exceptions.BusinessException;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.umo.lifecycle.FatalException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.Utility;

import java.io.IOException;

/**
 * 
 * <code>ErrorHandlerTestCase</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ErrorHandlerTestCase {

    
    public static void generateTestData(String targetDir) throws IOException, TransformerException
    {
        if (!(targetDir.endsWith("/") || targetDir.endsWith("\\")))
        {
            targetDir += "/"; 
        }

        ObjectToXml trans = new ObjectToXml();
        MuleException exception =
            new MuleException(Message.createStaticMessage("Some default exception"));
        FatalException fatal = new FatalException(Message.createStaticMessage("Some fatal exception"), new IOException("Some IO exception"));
        BusinessException business = new BusinessException("Some business exception");
        
        ExceptionBean bean = new ExceptionBean(exception);
        String xml = (String) trans.transform(bean);
        Utility.stringToFile(targetDir + "MuleException.xml", xml);
        
        bean = new ExceptionBean(fatal);
        xml = (String) trans.transform(bean);
        Utility.stringToFile(targetDir + "FatalException.xml", xml);
        
        bean = new ExceptionBean(business);
        xml = (String) trans.transform(bean);
        Utility.stringToFile(targetDir + "BusinesException.xml", xml);
       }


    public static void main(String[] args)
    {

        String path = (args.length >= 1 ? args[0] : "C:\\dev\\projects\\mule\\test-data\\samples\\errorhandler\\exceptions");
        try
        {
            generateTestData(path); }
        catch (Exception e)
        {
            e.printStackTrace(); 
        }

    }
}
