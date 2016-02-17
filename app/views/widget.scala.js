@(implicit request: RequestHeader)

var veritask = function() {

// Localize jQuery variable
    var jQuery;
    /******** Load jQuery if not present *********/
    if (window.jQuery === undefined || window.jQuery.fn.jquery !== '2.1.4') {
        var script_tag = document.createElement('script');
        script_tag.setAttribute("type", "text/javascript");
        script_tag.setAttribute("src", '@routes.Assets.versioned("lib/jquery/jquery.js").absoluteURL'
        )
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
            loadWidgetHTML();
        });
    }

    function loadWidgetHTML() {
        jQuery.get('@routes.Application.widgetHTML.absoluteURL', function (data) {
            jQuery('#example-widget-container').append(data);
        });
    }

    function getTask() {
        jQuery('#example-widget-container').show();
        jQuery.getJSON('@routes.Application.getTask.absoluteURL', function (data) {
            jQuery('#example-widget-container').append(data.html);
        });
    }

    function commitVerification() {

    }

    return {
        getTask: getTask
    };
}(); // We call our anonymous function immediately
