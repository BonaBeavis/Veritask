@(implicit request: RequestHeader)

var veritask = function() {

// Localize jQuery variable
    var jQuery;
    /******** Load jQuery if not present *********/
    if (window.jQuery === undefined || window.jQuery.fn.jquery !== '2.1.4') {
        var script_tag = document.createElement('script');
        script_tag.setAttribute("type", "text/javascript");
        script_tag.setAttribute("src", '@routes.Assets.versioned("lib/jquery/jquery.js").absoluteURL'
        );
        if (script_tag.readyState) {
            script_tag.onreadystatechange = function () { // For old versions of IE
                if (this.readyState == 'complete' || this.readyState == 'loaded') {
                    scriptLoadHandler();
                }
            };
        } else {
            script_tag.onload = scriptLoadHandler;
        }
        // Try to find the head, otherwise default to the documentElement
        (document.getElementsByTagName("head")[0] || document.documentElement).appendChild(script_tag);
    } else {
        // The jQuery version on the window is the one we want to use
        jQuery = window.jQuery;
        main();
    }

    /******** Called once jQuery has loaded ******/
    function scriptLoadHandler() {
        // Restore $ and window.jQuery to their previous values and store the
        // new jQuery in our local jQuery variable
        jQuery = window.jQuery.noConflict(true);
        // Call our main function
        main();
    }

    /******** Our main function ********/
    function main() {
        jQuery(document).ready(function ($) {
            /******* Load CSS *******/
            var css_link = $("<link>", {
                rel: "stylesheet",
                type: "text/css",
                href: '@routes.Assets.versioned("lib/bootstrap/css/bootstrap.css").absoluteURL'
            })
            css_link.appendTo('head');
            jQuery('#example-widget-container').hide();
            /******* Load HTML *******/
            laodJsRender();
            loadWidgetHTML();
        });
    }

    function laodJsRender() {
       jQuery.getScript('@routes.Assets.versioned("lib/jsrender/jsrender.js").absoluteURL', function () {
       });
    }

    function loadWidgetHTML() {
        jQuery.get('@routes.Application.widgetHTML.absoluteURL', function (data) {
            jQuery('#example-widget-container').append(data);
        });
    }

    var task;
    function getTask() {
        //jQuery('#example-widget-container').show();
        // jQuery.getJSON('ATHIERHINroutes.Application.getTask.absoluteURL', function(data) {
        //     task = data;
        // });
    }

    function lazyGetTemplate(name) {
        // If the named remote template is not yet loaded and compiled
        // as a named template, fetch it. In either case, return a promise
        // (already resolved, if the template has already been loaded)
        var deferred = jQuery.Deferred();
        if (jQuery.templates[name]) {
            deferred.resolve();
        } else {
            jQuery.getScript(
                    "//www.jsviews.com/samples/resources/templates/"
                    + name + ".js")
                .then(function() {
                    if (jQuery.templates[name]) {
                        deferred.resolve();
                    } else {
                        alert("Script: \"" + name + ".js\" failed to load");
                        deferred.reject();
                    }
                });
        }
        return deferred.promise();
    }

    function showTask() {
        jQuery.getJSON('@routes.Tasksets.getTask.absoluteURL', function(data) {
            task = data;
            var myTmpl = window.jsrender.templates("<label>Name:</label> {{:subjectAttributes.attribute}} <img src='{{:subjectAttributes.attribute}}' alt='some_text'>");
            var html = myTmpl.render(task);
            jQuery('#example-widget-container').html(html);
            jQuery('#example-widget-container').show();
        });
    }

    function commitVerification() {

    }

    return {
        showTask: showTask
    };
}(); // We call our anonymous function immediately
