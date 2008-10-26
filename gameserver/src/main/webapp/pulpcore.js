/*
	Copyright (c) 2008, Interactive Pulp, LLC
	All rights reserved.
	
	Redistribution and use in source and binary forms, with or without 
	modification, are permitted provided that the following conditions are met:

		* Redistributions of source code must retain the above copyright 
		  notice, this list of conditions and the following disclaimer.
		* Redistributions in binary form must reproduce the above copyright 
		  notice, this list of conditions and the following disclaimer in the 
		  documentation and/or other materials provided with the distribution.
		* Neither the name of Interactive Pulp, LLC nor the names of its 
		  contributors may be used to endorse or promote products derived from 
		  this software without specific prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
	AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
	IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
	ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
	LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
	CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
	SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
	INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
	ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
	POSSIBILITY OF SUCH DAMAGE.
*/

// PulpCore 0.11

// Global functions accessed via LiveConnect

function pulpcore_getCookie(name) {
	name = name + "=";
	
	var i;
	if (document.cookie.substring(0, name.length) == name) {
		i = name.length;
	}
	else {
		i = document.cookie.indexOf('; ' + name);
		if (i == -1) {
			return null;
		}
		i += name.length + 2;
	}
	
	var endIndex = document.cookie.indexOf('; ', i);
	if (endIndex == -1) {
		endIndex = document.cookie.length;
	}
	
	return unescape(document.cookie.substring(i, endIndex));
}

function pulpcore_setCookie(name, value, expireDate, path, domain, secure) {
	var expires = new Date();
	
	if (expireDate === null) {
		// Expires in 90 days
		expires.setTime(expires.getTime() + (24 * 60 * 60 * 1000) * 90);
	}
	else {
		expires.setTime(expireDate);
	}
	
	document.cookie = 
		name + "=" + escape(value) +
		"; expires=" + expires.toGMTString() +
		((path) ? "; path=" + path : "") +
		((domain) ? "; domain=" + domain : "") +
		((secure) ? "; secure" : "");
}

function pulpcore_deleteCookie(name, path, domain) {
	document.cookie = name + "=" + 
		((path) ? "; path=" + path : "") +
		((domain) ? "; domain=" + domain : "") +
		"; expires=Thu, 01-Jan-70 00:00:01 GMT";
}

function pulpcore_appletLoaded() {
	pulpCoreObject.hideSplash();
	setTimeout(pulpCoreObject.showObject, 50);
}

// Internal PulpCore code

var pulpCoreObject = {
	
	// Max time (milliseconds) to show the splash gif; after this time the applet is shown.
	// Usually the applet calls pulpcore_appletLoaded() before the timeout.
	splashTimeout: 15000,
	
	// Arguments for Java 6u10 (plugin2)
	javaArguments: "-Dsun.awt.noerasebackground=true -Djnlp.packEnabled=true",
	
	// The minimum JRE version
	requiredJRE: "1.4",
	
	// The minimum JRE version, formatted for IE
	ieRequiredJRE: "1,4,0,0",
	
	// The URL to the CAB of the latest JRE (for IE)
	// See http://java.sun.com/javase/6/docs/technotes/guides/deployment/deployment-guide/autodl-files.html
	// First is for Windows9x, second is For XP/2000/Vista/etc.
	getJavaCAB: [ 
		"http://java.sun.com/update/1.5.0/jinstall-1_5_0_11-windows-i586.cab",
		"http://java.sun.com/update/1.6.0/jinstall-6-windows-i586.cab"
	],
	
	// The URL of the XPI of the latest JRE (for Windows+Firefox)
	// Currently using the unsigned version (-jc) since the signed version in 6u3 is broken
	// "http://java.sun.com/update/1.6.0/jre-6-windows-i586.xpi"
	// "http://java.com/jre-install.xpi"
	getJavaXPI: [
		"http://java.sun.com/update/1.5.0/jre-1_5_0_11-windows-i586.xpi",
		"http://java.sun.com/update/1.6.0/jre-6-windows-i586-jc.xpi"
	],
	
	// The URL to the page to visit to install Java
	getJavaURL: "http://java.sun.com/webapps/getjava/BrowserRedirect?host=java.com" +
		'&returnPage=' + document.location,
		
	// Special URL for Google Chrome (requires 6u10)
	getJavaChromeURL: "http://java.sun.com/javase/downloads/ea.jsp",
		
	deploymentToolkitMimeType: 'application/npruntime-scriptable-plugin;DeploymentToolkit',
		
	// The Applet HTML (inserted after a delay)
	appletHTML: "",
	appletInserted: false,
	
	// Text defaults
	
	getRestartMessage: function() {
		return window.pulpcore_text_restart ||
			"Java installed! To play, you may need to restart your browser.";
	},
	
	getInstallMessage: function() {
		return window.pulpcore_text_install ||
			"To play, install Java now.";
	},
	
	getChromeInstallMessage: function() {
		return window.pulpcore_text_install_chrome ||
			"To play in Google Chrome, install Java 6 update 10.";
	},
	
	// Gets the codebase from the document URL
	getCodeBase: function() {
		var codeBase = document.URL;
		if (codeBase.length <= 7 || codeBase.substr(0, 7) != "http://") {
			return "";
		}
		if (codeBase.charAt(codeBase.length - 1) != '/') {
			var index = codeBase.lastIndexOf('/');
			// Don't include the http://
			if (index > 7) {
				codeBase = codeBase.substring(0, index + 1);
			}
			else {
				codeBase += '/';
			}
		}
		return codeBase;
	},
	
	write: function() {
		pulpCoreObject.detectBrowser();
		pulpCoreObject.initDeploymentToolkit();
		
		document.write(pulpCoreObject.getObjectHTML());
	},
	
	initDeploymentToolkit: function() {
        if (pulpCoreObject.browserName == "Explorer") {
            document.write('<' + 
                'object classid="clsid:CAFEEFAC-DEC7-0000-0000-ABCDEFFEDCBA" ' +
                'id="deploymentToolkit" width="0" height="0">' +
                '<' + '/' + 'object' + '>');
        } 
		else if (pulpCoreObject.browserIsMozillaFamily) {
            if (navigator.mimeTypes !== null) {
				var mimeType = pulpCoreObject.deploymentToolkitMimeType;
				for (var i = 0; i < navigator.mimeTypes.length; i++) {
					if (navigator.mimeTypes[i].type == mimeType) {
						if (navigator.mimeTypes[i].enabledPlugin) {
							document.write('<' + 
								'embed id="deploymentToolkit" type="' + 
								mimeType + '" hidden="true" />');
						}
                    }
                }
            }
        }
    },
	
    isPlugin2: function() {
		// Chrome can only run plugin2, but can't detect it?
		if (pulpCoreObject.browserName == "Chrome") {
			return true;
		}
		var deploymentToolkit = document.getElementById('deploymentToolkit');
        if (deploymentToolkit !== null) {
            try {
                return deploymentToolkit.isPlugin2();
            } 
			catch (err) {
                // Fall through
            }
        }
        return false;
    },
	
	hideSplash: function() {
		var splash = document.getElementById('pulpcore_splash');
		splash.style.display = "none";
		splash.style.visibility = "hidden";
	},
	
	showObject: function() {
		if (pulpCoreObject.browserIsMozillaFamily) {
			var spacer = document.getElementById('pulpcore_spacer');
			spacer.style.display = "none";
			spacer.style.visibility = "hidden";
		}
		var gameContainer = document.getElementById('pulpcore_game');
		gameContainer.style.display = "block";
		gameContainer.style.visibility = "visible";
		if (pulpCoreObject.browserName == "Explorer") {
			gameContainer.style.position = "static";
		}
	},
	
	splashLoaded: function(splash) {
		// Prevent this call from occuring again
		// (IE will continue to call onLoad() if the splash loops.)
		if (splash !== null) {
			splash.onload = "";
		}
		pulpCoreObject.insertApplet();
	},
	
	insertApplet: function() {
		if (pulpCoreObject.appletInserted) {
			return;
		}
		var gameContainer = document.getElementById('pulpcore_game');
		if (gameContainer === null) {
			setTimeout(pulpCoreObject.insertApplet, 500);
		}
		else {
			pulpCoreObject.appletInserted = true;
			gameContainer.innerHTML = pulpCoreObject.appletHTML;
			setTimeout(pulpcore_appletLoaded, 
				navigator.javaEnabled() ? pulpCoreObject.splashTimeout : 10);
		}
	},
		
	getObjectHTML: function() {
		pulpCoreObject.appletHTML = "";
		
		if (!pulpCoreObject.isAcceptableJRE()) {
			return pulpCoreObject.installLatestJRE();
		}
		
		var splashHTML;
		
		// The splash image to show during JRE boot and jar loading
		var splash =  window.pulpcore_splash || "splash.gif";
	
		// Applet attributes and parameters
		var code	 = window.pulpcore_class || "pulpcore.platform.applet.CoreApplet.class";
		var width	 = window.pulpcore_width || 640;
		var height	 = window.pulpcore_height || 480;
		var archive	 = window.pulpcore_archive || "project.jar";
		var bgcolor	 = window.pulpcore_bgcolor || "#000000";
		var fgcolor	 = window.pulpcore_fgcolor || "#aaaaaa";
		var scene	 = window.pulpcore_scene || "";
		var assets	 = window.pulpcore_assets || "";
		var params	 = window.pulpcore_params || { };
		var codebase = window.pulpcore_codebase || pulpCoreObject.getCodeBase();
		var name     = window.pulpcore_name || params.name || "";
		
		if (name === "") {
			// Use the archive name
			var index = archive.indexOf('.');
			var index2 = archive.indexOf('-');
			if (index2 != -1 && index2 < index) {
				index = index2;
			}
			if (index == -1) {
				name = archive;
			}
			else {
				name = archive.substring(0, index);
			}
		}
		
		// Create the object tag parameters
		var objectParams =
			'  <param name="code" value="' + code + '" />\n' +
			'  <param name="archive" value="' + archive + '" />\n' +
			'  <param name="name" value="' + name + '" />\n' +
			'  <param name="mayscript" value="true" />\n' +
			'  <param name="scriptable" value="true" />\n' +
			'  <param name="boxbgcolor" value="' + bgcolor + '" />\n' +
			'  <param name="boxfgcolor" value="' + fgcolor + '" />\n' +
			'  <param name="boxmessage" value="" />\n' +
			'  <param name="codebase_lookup" value="false" />\n' +
			'  <param name="pulpcore_browser_name" value="' + pulpCoreObject.browserName + '" />\n' +
			'  <param name="pulpcore_browser_version" value="' + pulpCoreObject.browserVersion + '" />\n';
			
		// For Java 6u10 plugin2
		if (pulpCoreObject.isPlugin2()) {
			var args = pulpCoreObject.javaArguments;
			
			// NOTE: fastest performance of the software renderer on Windows is by
			// disabling both D3D and BufferStrategy
			// See http://bugs.sun.com/view_bug.do?bug_id=6652116
			if (pulpCoreObject.osName == "Windows") {
				args += " -Dsun.java2d.d3d=false";
				objectParams += '  <param name="pulpcore_use_bufferstrategy" value="false" />\n';
			}
			
			objectParams += 
				'  <param name="boxborder" value="false" />\n' +
				'  <param name="image" value="' + splash + '" />\n' +
				'  <param name="centerimage" value="true" />\n' +
				'  <param name="separate_jvm" value="true" />\n' +
				'  <param name="java_arguments" value="' + args + '" />\n';
		}

		// For the PulpCore app
		if (codebase.length > 0) {
			objectParams += '  <param name="codebase" value="' + codebase + '" />\n';
		}
		if (scene.length > 0) {
			objectParams += '  <param name="scene" value="' + scene + '" />\n';
		}		
		if (assets.length > 0) {
			objectParams += '  <param name="assets" value="' + assets + '" />\n';
		}
		for (var i in params) {
			if (i !== "name") {
				objectParams += '  <param name="' + i + '" value="' + params[i] + '" />\n';
			}
		}
		objectParams += '  ' + pulpCoreObject.getInstallHTML();
		
		// Create the Object tag. 
		if (pulpCoreObject.browserName == "Explorer") {
			var extraAttributes = '';
			var cabURL = pulpCoreObject.getJavaCAB[pulpCoreObject.osIsOldWindows ? 0 : 1];
			if (pulpCoreObject.compareVersions(pulpCoreObject.browserVersion, "7") < 0 && 
				parent.frames.length > 0) 
			{
				// On IE6 and older, if the site is externally framed, LiveConnect will not work.
				// However, IE can use onfocus to emulate the appletLoaded() behavior
				extraAttributes = '  onfocus="pulpcore_appletLoaded();"\n';
			}
			// Use the <object> tag instead of <applet>. IE users without Java can get the 
			// latest JRE without leaving the page or restarting the browser.
			pulpCoreObject.appletHTML = 
				'<object id="pulpcore_object"\n' + 
				'  classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"\n' +
				'  codebase="' + cabURL + '#Version=' + pulpCoreObject.ieRequiredJRE + '"\n' +
				extraAttributes +
				'  width="' + width + '" height="' + height + '">\n' + 
				objectParams +
				'</object>';
			// Explorer has special code for centering the splash. Also, the game is
			// positioned far left to avoid flicker when the applet is displayed.
			splashHTML =
				'<div id="pulpcore_splash"\n' + 
				'  style="width: ' + width + 'px; ' + 
				'height: ' + height + 'px; position: relative; overflow: hidden; ' +
				'text-align: center">\n' +
				'  <div style="position: absolute; top: 50%; left: 50%;">\n' +
				'    <img alt="" src="' + splash + '"\n' + 
				'    onload="pulpCoreObject.splashLoaded(this)"\n' + 
				'    style="position: relative; top: -50%; left: -50%;" />\n' +
				'  </div>\n' +
				'</div><div id="pulpcore_game" style="position: relative; left: -3000px;"></div>\n';
		}
		else {
			if (pulpCoreObject.osName == "Windows" &&
				pulpCoreObject.browserName == "Safari" && 
				pulpCoreObject.compareVersions(pulpCoreObject.browserVersion, "522.11") >= 0)
			{
				// Known versions: 522.11, 522.12, 522.15, 523.12
				// Safari 3 beta on Windows doesn't recognize the archive param when
				// the <object> tag is used. For now, use the <applet> tag.
				// See http://joliclic.free.fr/html/object-tag/en/object-java.html
				// LiveConnect also does not work.
				pulpCoreObject.appletHTML =
				'<applet id="pulpcore_object"\n' + 
				'  codebase="' + codebase + '"\n' +
				'  code="' + code + '"\n' +
				'  archive="' + archive + '"\n' +
				'  width="' + width + '"\n' +
				'  height="' + height + '" mayscript="true">\n' + 
				objectParams +
				'</applet>';
			}
			else {
				// Firefox, Safari, Opera, Mozilla, etc.
				// Note: the minimum version string is ignored on the Mozilla family
				pulpCoreObject.appletHTML =
				'<object id="pulpcore_object"\n' + 
				'  classid="java:' + code + '"\n' +
				'  type="application/x-java-applet;version=' + pulpCoreObject.requiredJRE + '"\n' + 
				'  width="' + width + '" height="' + height + '">\n' + 
				objectParams +
				'</object>';
			}
			var spacer = "";
			if (pulpCoreObject.browserIsMozillaFamily) {
				// Prevents white flash on Firefox
				spacer = '<div id="pulpcore_spacer" style="height: 100%">&nbsp;</div>\n';
			}
			splashHTML =
				'<div id="pulpcore_splash"\n' + 
				'  style="width: ' + width + 'px; ' + 
				'height: ' + height + 'px; text-align: center; ' + 
				'display: table-cell; vertical-align: middle">\n' +
				'  <img alt="" src="' + splash + '"\n' +
				'  onload="pulpCoreObject.splashLoaded(this)" />\n' + 
				'</div>\n' +
				spacer +
				'<div id="pulpcore_game" style="visibility: hidden"></div>\n';
		}
		
		// In case splash.onLoad() is never called
		setTimeout(pulpCoreObject.insertApplet, 1000);
		
		return '<div style="margin: auto; overflow: hidden; text-align: left; ' + 
			'width: ' + width + 'px; height: ' + height + 'px; ' + 
			'background: ' + bgcolor + '">\n' +
			splashHTML +
			'</div>\n';
	},
	
	
	// JRE detection
	
	
	/**
		Returns true if the installed JRE is Java 1.4 or newer. 
	*/
	isAcceptableJRE: function() {
		var version;
		var i;
		
		if (pulpCoreObject.browserName == "Explorer") {
			// IE can install via the CAB
			return true;
		}
		else if (pulpCoreObject.browserName == "Safari" && pulpCoreObject.osName == "Windows") {
			// Can't detect the version - let Safari on Windows handle it
			return true;
		}
		else if (pulpCoreObject.browserName == "Safari" && 
			navigator.plugins && navigator.plugins.length) 
		{
			for (i = 0; i < navigator.plugins.length; i++) {
				var s = navigator.plugins[i].description;
				// Based on code from the Deployment Toolkit
				if (s.search(/^Java Switchable Plug-in/) != -1) {
					return true;
				}
				
				var m = s.match(/^Java (1\.4\.2|1\.5|1\.6|1\.7).* Plug-in/);
				if (m !== null) {
					version = m[1];
					if (pulpCoreObject.isAcceptableJREVersion(version)) {
						return true;
					}
				}
			}
			return false;
		}
		else if (navigator.mimeTypes && navigator.mimeTypes.length &&
			pulpCoreObject.browserIsMozillaFamily)
		{
			if (pulpcore_getCookie("javaRecentlyInstalled") == "true") {
				return true;
			}
			version = pulpCoreObject.getHighestInstalledJavaViaMimeTypes();
			return pulpCoreObject.isAcceptableJREVersion(version);
		}
		else if (pulpCoreObject.browserName == "Chrome") {
			// Chrome requires 1.6.0_10 as the minimum. 
			// So, if the mime type is available, return true.
			if (navigator.mimeTypes && navigator.mimeTypes.length) {
				for (i = 0; i < navigator.mimeTypes.length; i++) {
					if (navigator.mimeTypes[i].type == "application/x-java-applet") {
						return true;
                    }
                }
			}
			return false;
		}
		else {
			// Couldn't detect - let the browser handle it
			return true;
		}
	},
	
	getHighestInstalledJavaViaMimeTypes: function() {
		var version = "0.0";
		var mimeType = "application/x-java-applet;version=";
		for (var i = 0; i < navigator.mimeTypes.length; i++) {
			var s = navigator.mimeTypes[i].type;
			if (s.substr(0, mimeType.length) == mimeType) {
				var testVersion = s.substr(mimeType.length);
				if (pulpCoreObject.compareVersions(testVersion, version) == 1) {
					version = testVersion;
				}
			}
		}
		return version;
	},
	
	isAcceptableJREVersion: function(version) {
		var result = pulpCoreObject.compareVersions(version, pulpCoreObject.requiredJRE);
		return (result >= 0);
	},
	
	/**
		Compares two versions in the form "x.x.x".
		
		Returns 1 if versionA is greater than versionB, -1 if versionA is less than versionB,
		and 0 if versionA is equal to versionB
	*/
	compareVersions: function(versionA, versionB) {
		// Make sure both versions are strings
		versionA += '';
		versionB += '';
		
		var a = versionA.split('.');
		var b = versionB.split('.');
		var len = Math.max(a.length, b.length);
		for (var i = 0; i < len; i++) {
			if (i >= a.length) {
				a[i] = 0;
			}
			if (i >= b.length) {
				b[i] = 0;
			}
			if (a[i] > b[i]) {
				return 1;
			}
			if (a[i] < b[i]) {
				return -1;
			}
		}
	
		return 0;
	},
	
	installLatestJRE: function() {
		if (pulpCoreObject.shouldInstallXPI()) {
			pulpCoreObject.installXPI();
		}
		return pulpCoreObject.getInstallHTML();
	},
	
	getInstallHTML: function() {
		var extraAttributes = '';
		if (pulpCoreObject.shouldInstallXPI()) {
			extraAttributes = ' onclick="pulpCoreObject.installXPI();return false;"';
		}
		if (pulpCoreObject.browserName == "Chrome") {
			return '<p id="pulpcore_install" style="text-align: center">' +
				'<a href="' + pulpCoreObject.getJavaChromeURL + '">' + 
				pulpCoreObject.getChromeInstallMessage() + '</a></p>\n';
		}
		else {
			return '<p id="pulpcore_install" style="text-align: center">' +
				'<a href="' + pulpCoreObject.getJavaURL + '"' + extraAttributes + '>' + 
				pulpCoreObject.getInstallMessage() + '</a></p>\n';
		}
	},
	
	shouldInstallXPI: function() {
		return pulpCoreObject.browserIsMozillaFamily && pulpCoreObject.osName == "Windows" &&
			InstallTrigger && InstallTrigger.enabled();
	},
	
	installXPI: function() {
		var xpiURL = pulpCoreObject.getJavaXPI[pulpCoreObject.osIsOldWindows ? 0 : 1];
		// Note: The user needs to allow the domain to install the plugin.
		var xpi = { "Java Plug-in": xpiURL }; 
		InstallTrigger.install(xpi, pulpCoreObject.installXPIComplete);
	},
	
	installXPIComplete: function(url, result) {
		var success = (result === 0);
		if (success) {
			// Set a session-only cookie (since Firefox doesn't refresh mime types)
			document.cookie = "javaRecentlyInstalled=true; path=/";
			
			var version = pulpCoreObject.getHighestInstalledJavaViaMimeTypes().split('.');
			if (version[0] == "1" && version[1] == "3") {
				// If Java 1.3 is previously installed, ask them to restart their browser.
				// Java 1.3 seems to "take over" and not allow the browser to use Java 6 until
				// Firefox is restarted. 
				// TODO: re-evaluate if JRE 1.5 is defined as the minimum. Does JRE 1.4 also
				// "take over"?
				var install = document.getElementById('pulpcore_install');
				install.innerHTML = pulpCoreObject.getRestartMessage();
			}
			else {
				// If no Java previously installed, automagically start the game 
				// (by reloading the page)
				location.href = document.location;
			}
		}
	},
	
	
	// Browser detection
	// Based on a script from Peter-Paul Koch at QuirksMode:
	// http://www.quirksmode.org/js/detect.html
	// Up-to-date as of September 7, 2008
	
	versionSearchString: "",
	browserName: "",
	browserVersion: "",
	browserIsMozillaFamily: false,
	osName: "",
	osIsOldWindows: false,
	
	detectBrowser: function() {
		pulpCoreObject.browserName = 
			pulpCoreObject.searchString(pulpCoreObject.dataBrowser) || 
			"An unknown browser";
		pulpCoreObject.browserVersion =
			pulpCoreObject.searchVersion(navigator.userAgent) || 
			pulpCoreObject.searchVersion(navigator.appVersion) ||
			"an unknown version";
		pulpCoreObject.osName =
			pulpCoreObject.searchString(pulpCoreObject.dataOS) || 
			"an unknown OS";
		pulpCoreObject.browserIsMozillaFamily =
			pulpCoreObject.browserName == "Netscape" || 
			pulpCoreObject.browserName == "Mozilla" || 
			pulpCoreObject.browserName == "Firefox";
			
		if (pulpCoreObject.osName == "Windows") {
			var ua = navigator.userAgent.toLowerCase();
			if (ua.search(/win98/) != -1 ||
				ua.search(/windows\s98/) != -1 ||
				ua.search(/windows\sme/) != -1 ||
				ua.search(/windows\s95/) != -1 ||
				ua.search(/win95/) != -1 ||
				ua.search(/nt\s4\.0/) != -1 || 
				ua.search(/nt4\.0/) != -1)
			{
				pulpCoreObject.osIsOldWindows = true;
			}
		}
	},

	searchString: function(data) {
		for (var i = 0; i < data.length; i++)	{
			var dataString = data[i].string;
			var dataProp = data[i].prop;
			pulpCoreObject.versionSearchString = data[i].versionSearch || data[i].identity;
			if (dataString) {
				if (dataString.indexOf(data[i].subString) != -1) {
					return data[i].identity;
				}
			}
			else if (dataProp) {
				return data[i].identity;
			}
		}
	},
	
	searchVersion: function(dataString) {
		var index = dataString.indexOf(pulpCoreObject.versionSearchString);
		if (index == -1) {
			return;
		}
		return parseFloat(dataString.substring(index+pulpCoreObject.versionSearchString.length+1));
	},
	
	dataBrowser: [
		{
			string: navigator.userAgent,
			subString: "Chrome",
			identity: "Chrome"
		},
		{
			string: navigator.userAgent,
			subString: "OmniWeb",
			versionSearch: "OmniWeb/",
			identity: "OmniWeb"
		},
		{
			string: navigator.vendor,
			subString: "Apple",
			identity: "Safari"
		},
		{
			prop: window.opera,
			identity: "Opera"
		},
		{
			string: navigator.vendor,
			subString: "iCab",
			identity: "iCab"
		},
		{
			string: navigator.vendor,
			subString: "KDE",
			identity: "Konqueror"
		},
		{
			string: navigator.userAgent,
			subString: "Firefox",
			identity: "Firefox"
		},
		{
			string: navigator.vendor,
			subString: "Camino",
			identity: "Camino"
		},
		{	// for newer Netscapes (6+)
			string: navigator.userAgent,
			subString: "Netscape",
			identity: "Netscape"
		},
		{
			string: navigator.userAgent,
			subString: "MSIE",
			identity: "Explorer",
			versionSearch: "MSIE"
		},
		{
			string: navigator.userAgent,
			subString: "Gecko",
			identity: "Mozilla",
			versionSearch: "rv"
		},
		{	// for older Netscapes (4-)
			string: navigator.userAgent,
			subString: "Mozilla",
			identity: "Netscape",
			versionSearch: "Mozilla"
		}
	],
	dataOS: [
		{
			string: navigator.platform,
			subString: "Win",
			identity: "Windows"
		},
		{
			string: navigator.platform,
			subString: "Mac",
			identity: "Mac"
		},
		{
			string: navigator.platform,
			subString: "Linux",
			identity: "Linux"
		}
	]
};

pulpCoreObject.write();
