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
*
*/
package org.mule.extras.pxe.transformers;

import com.fs.pxe.bpel.capi.CompilationException;
import com.fs.pxe.bpel.compiler.BpelC;
import com.fs.pxe.sfwk.deployment.ExplodedSarFile;
import com.fs.pxe.sfwk.deployment.SystemDeploymentBundle;
import com.fs.pxe.sfwk.deployment.som.SystemDescriptor;
import com.fs.pxe.sfwk.deployment.som.sax.SystemDescriptorFactory;
import com.fs.pxe.sfwk.rr.RrException;
import com.fs.pxe.sfwk.rr.wsdl.WsdlCacheRr;
import com.fs.utils.TempFileManager;
import com.fs.utils.sax.LoggingErrorHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractSDBTransformer extends AbstractTransformer {
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public AbstractSDBTransformer() {
        setReturnClass(SystemDeploymentBundle.class);
    }


    public Object doTransform(Object src, String encoding) throws TransformerException {

        try {
            return trans(src, getEntryIterator(src));
        } catch (Exception e) {
            throw new TransformerException(this, e);
        }
    }

    protected abstract Iterator getEntryIterator(Object src) throws Exception;

    protected abstract BundleEntry getBundleEntry(Object source, Object artifact) throws Exception;

    protected SystemDeploymentBundle trans(Object source, Iterator iterator) throws RrException, CompilationException, IOException, SAXException, URISyntaxException, TransformerException {
        HashMap resources = new HashMap();
        HashMap wsdls = new HashMap();
        ArrayList bpels = new ArrayList();
        ArrayList sysDescs = new ArrayList();

        while (iterator.hasNext()) {
            Object entry = iterator.next();
            BundleEntry be = null;
            try {
                be = getBundleEntry(source, entry);
            } catch (Exception e) {
                throw new TransformerException(this, e);
            }

            /*
             * Ignore the manifest and other meta crap.
             */
            if (be.getName().startsWith("META-INF/")) {
                continue;
            }
            if (be.isDirectory()) {
                String msg = "Expected flat archive structure with all resources in the root; resources may overlap, resulting in unexpected results.";
                logger.error(msg);
                // Shouldn't we be throwing an exception?!?
                return null;
            }


            String name = be.getName();
            String url = be.getUrl();

            if (name.toLowerCase().endsWith(".wsdl")) {
                logger.info("Found WSDL with name " + name);
                wsdls.put(name, url);
            } else if (name.toLowerCase().endsWith(".bpel")) {
                logger.info("Found BPEL process with name " + name);
                bpels.add(url);
            } else if (name.toLowerCase().equals("pxe-system.xml")) {
                logger.info("Found PXE system descriptor with name " + name);
                sysDescs.add(url);
            } else {
                resources.put(name, url);
            }
        }

        if (wsdls.size() == 0) {
            logger.error("No WSDL found; at least one *.wsdl file must be included.");
        } else if (wsdls.size() != 1) {
            if (wsdls.get("main.wsdl") == null) {
                logger.error("Archive contains multiple WSDL files but none named \"main.wsdl\".");
                return null;
            }
        }

        if (bpels.size() > 1) {
            logger.error("Archive must contain at most one *.bpel file.");
            return null;
        }

        if (sysDescs.size() != 1) {
            logger.error("Expected exactly one system descriptor in the archive; " +
                    "instead, found " + sysDescs.size() + ".");
            return null;
        }


        URL descUrl = new URL((String) sysDescs.get(0));

        /*
         * Get either the only one or get the one called "main.wsdl".
         */
        URL wsdlUrl = new URL((String) (wsdls.size() != 1 ?
                (wsdls.get("main.wsdl")) : (wsdls.get(wsdls.keySet().iterator().next()))));

        URL bpelUrl = new URL((String) bpels.get(0));

        /*
         * Actually build the system at this point.
         */
        LoggingErrorHandler leh = new LoggingErrorHandler(logger);
        SystemDescriptor sd = SystemDescriptorFactory.parseDescriptor(descUrl, leh, null, true);

        sd.setWsdlUri(new URI(wsdlUrl.toExternalForm()));
        File tmpBar = TempFileManager.getTemporaryFile("bpel-compile");

        WsdlCacheRr cacheGenerator = new WsdlCacheRr();
        cacheGenerator.addWSDL(wsdlUrl);
        if (!cacheGenerator.containsResource("file:main.wsdl")) {
            cacheGenerator.aliasUri(wsdlUrl.toExternalForm(), "file:main.wsdl");
        }
        ExplodedSarFile sf = new ExplodedSarFile();
        sf.setCommonResourceRepository(cacheGenerator);

        BpelC bc = BpelC.newBpelCompiler();

        bc.setProcessWSDL(new URI(wsdlUrl.toExternalForm()));
        bc.setOutputFile(tmpBar);
        bc.compile(bpelUrl);

        sf.setSystemDescriptor(sd);
        sf.addResource("a.cbp", tmpBar);

        for (Iterator it = resources.keySet().iterator(); it.hasNext();) {
            String name = (String) it.next();
            sf.addResource(name, new URL((String) resources.get(name)));
        }

        sf.validate();
        return sf;

    }

    class BundleEntry {
        private String name;
        private String url;
        private boolean directory;

        public BundleEntry(String name, String url, boolean directory) {
            this.name = name;
            this.directory = directory;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public boolean isDirectory() {
            return directory;
        }
    }
}
