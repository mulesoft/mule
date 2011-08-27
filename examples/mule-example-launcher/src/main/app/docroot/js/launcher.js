    $.ajaxSetup({
          timeout: 1000,
          async: false,
    });

    function getUrlVars()
    {
        var vars = [], hash;
        var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');

        for(var i = 0; i < hashes.length; i++)
        {
            hash = hashes[i].split('=');
            vars.push(hash[0]);
            vars[hash[0]] = unescape(hash[1]);
        }

        return vars;
    }

    function getCheckedValue(radioObj) {
        if(!radioObj)
            return "";
        var radioLength = radioObj.length;
        if(radioLength == undefined)
            if(radioObj.checked)
                return radioObj.value;
            else
                return "";
        for(var i = 0; i < radioLength; i++) {
            if(radioObj[i].checked) {
                return radioObj[i].value;
            }
        }
        return "";
    }

    function doAjaxRequest(url, targetId)
    {
        jQuery.post(url, 'script',
                function(data, textStatus, xmlHttpRequest)
                {
                    $("#" + targetId).html(data);
                }, 'text');
        return true;
    }

    function getRemoteResource(exampleName, resourceName, handlerFunction)
    {
        jQuery.get('/examples/' + exampleName + '/' + resourceName, 'script', handlerFunction, 'text');
    }
    
    function getJsonResponse(data)
    {
        if(data)
        {
        	if(data.response)
    		{
        		return data.response;
    		}
        	else
        	{
                var obj = jQuery.parseJSON(data);
                if(obj)
            	{
                    return obj.response;
            	}
                else
                {
                	return '';
                }
        	}
        }
        return data;
    }
    
    function html2entity(str)
    {
        return str.replace(/[<>]/g,
            function(s){return (s == "<")? "&lt;" :"&gt;"});
    }