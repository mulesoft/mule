/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

var _MULE_INCLUDE = {

  script: function(libraryName) {
    document.write('<script type="text/javascript" src="'+libraryName+'"></script>');
  },
  load: function() {
    var scriptTags = document.getElementsByTagName("script");
    for(var i=0;i<scriptTags.length;i++) {
      if(scriptTags[i].src && scriptTags[i].src.match(/mule\.js$/)) {
        var path = scriptTags[i].src.replace(/mule\.js$/,'');
        this.script(path + 'dojo/dojo.js');
        this.script(path + '_mule.js');
        break;
      }
    }
  }
}

_MULE_INCLUDE.load();
