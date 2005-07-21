/*
 * Created on Dec 13, 2004
 *
 */
package org.mule.jbi.engines.pxe;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
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

/**
 * Utility class that compiles JAR files contianing source artifacts such
 * as .bpel process definitions, .wsdl files, and the like into proper
 * system archives (SARs).
 */
class JarToSarConverter {

  static final Log __log = LogFactory.getLog(JarToSarConverter.class);

  /**
   * Compile the specified JAR file into a {@link SystemDeploymentBundle}.
   * Currently, this method expects to find exactly one BPEL process, one
   * PXE deployment descriptor (named <code>pxe-system.xml</code>) and one
   * or more WSDL files (if more than one, the "root" WSDL should be named
   * <code>main.wsdl</code>).  
   *
   * @param jar JAR file containing source artifacts
   *
   * @throws IOException
   * @throws URISyntaxException
   * @throws SAXException
   * @throws RrException
   * @throws ZipException
   * 
   */
  static SystemDeploymentBundle convert(File jar) throws RrException, CompilationException, IOException, SAXException, URISyntaxException  {
    HashMap resources = new HashMap();
    HashMap wsdls = new HashMap();
    ArrayList bpels = new ArrayList();
    ArrayList sysDescs = new ArrayList();
    
    ZipFile zip = new ZipFile(jar);
    try {      
      for (Enumeration e = zip.entries();e.hasMoreElements();) {
        ZipEntry entry = (ZipEntry)e.nextElement();
        /*
         * Ignore the manifest and other meta crap.
         */
        if (entry.getName().startsWith("META-INF/")) {
          continue;
        }
        if (entry.isDirectory()) {
          String msg = "Expected flat archive structure with all resources in the root; resources may overlap, resulting in unexpected results.";
          __log.error(msg);
          // Shouldn't we be throwing an exception?!?
          return null;
        }
  
        String name = entry.getName(); 
        String url = "jar:" + jar.toURL().toExternalForm() + "!/" + name;
        
        if(name.toLowerCase().endsWith(".wsdl")){
          __log.info("Found WSDL with name " + name);
          wsdls.put(name,url);
        }else if(name.toLowerCase().endsWith(".bpel")){
           __log.info("Found BPEL process with name " + name);
           bpels.add(url);         
        }else if(name.toLowerCase().equals("pxe-system.xml")){
          __log.info("Found PXE system descriptor with name " + name);
          sysDescs.add(url);
        } else {
          resources.put(name,url);
        }
      }
    } finally {     
      zip.close();
    }
    
    if (wsdls.size() == 0) {
      __log.error("No WSDL found in archive " + jar.getName() + 
          "; at least one *.wsdl file must be included.");
    } else if (wsdls.size() != 1) {
      if (wsdls.get("main.wsdl") == null) {
        __log.error("Archive contains multiple WSDL files but none named \"main.wsdl\".");
        return null;
      }
    }
    
    if (bpels.size() > 1) {
      __log.error("Archive must contain at most one *.bpel file.");
      return null;
    }
    
    if (sysDescs.size() != 1) {
      __log.error("Expected exactly one system descriptor in the archive; " +
          "instead, found " + sysDescs.size() + ".");
      return null;
    }
    
    
    URL descUrl = new URL((String) sysDescs.get(0));
    
    /*
     * Get either the only one or get the one called "main.wsdl".
     */
    URL wsdlUrl = new URL((String) (wsdls.size()!=1 ? 
        (wsdls.get("main.wsdl")):(wsdls.get(wsdls.keySet().iterator().next()))));
    
    URL bpelUrl = new URL((String) bpels.get(0));
    
    /*
     * Actually build the system at this point.
     */
    LoggingErrorHandler leh = new LoggingErrorHandler(__log);
    SystemDescriptor sd = SystemDescriptorFactory.parseDescriptor(descUrl, leh, null, true);
    
    sd.setWsdlUri(new URI(wsdlUrl.toExternalForm()));
    File tmpBar = TempFileManager.getTemporaryFile("bpel-compile");

    WsdlCacheRr cacheGenerator = new WsdlCacheRr();
    cacheGenerator.addWSDL(wsdlUrl);
    if (!cacheGenerator.containsResource("file:main.wsdl")) {
      cacheGenerator.aliasUri(wsdlUrl.toExternalForm(),"file:main.wsdl");
    }
    ExplodedSarFile sf = new ExplodedSarFile();
    sf.setCommonResourceRepository(cacheGenerator);
    
    BpelC bc = BpelC.newBpelCompiler();

    bc.setProcessWSDL(new URI(wsdlUrl.toExternalForm()));
    bc.setOutputFile(tmpBar);
    bc.compile(bpelUrl); 
    
    sf.setSystemDescriptor(sd);
    sf.addResource("a.cbp", tmpBar);
    
    for (Iterator it = resources.keySet().iterator();it.hasNext();) {
      String name = (String) it.next();
      sf.addResource(name,new URL((String) resources.get(name)));
    }
    
    sf.validate();
    return sf;

  }
}
