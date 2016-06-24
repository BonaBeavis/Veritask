@(implicit request: RequestHeader)

var veritask = function() {

    loadDependency('@routes.Assets.versioned("lib/jsrender/jsrender.js").absoluteURL');
    loadDependency('@routes.Assets.versioned("lib/node-uuid/uuid.js").absoluteURL');

    var jQuery;
    if (window.jQuery === undefined || window.jQuery.fn.jquery !== '2.1.4') {
        loadDependency(
            '@routes.Assets.versioned("lib/jquery/jquery.js").absoluteURL',
            function () {
                jQuery = window.jQuery.noConflict(true);
                main();
            }
        );
    } else {
        jQuery = window.jQuery;
        main();
    }

    function loadDependency(src, callback) {
        var s = document.createElement('script');
        s.async = true;
        s.src = src;
        s.onload = callback;
        document.body.appendChild(s);
    }

    /******** Our main function ********/
    function main() {

        jQuery(document).ready(function ($) {


            /******* Load CSS *******/
            var css_link = $("<link>", {
                rel: "stylesheet",
                type: "text/css",
                href: '@routes.Assets.versioned("lib/bootstrap/css/bootstrap.css").absoluteURL'
            });
            css_link.appendTo('head');

            jQuery('#veritask').hide();
            jQuery.get('@routes.Application.widgetHTML.absoluteURL', function (data) {
                jQuery('#veritask').append(data);
            });
        });
    }

    function challengeUser(user) {
        jQuery.getJSON('@routes.Tasksets.getTask("").absoluteURL' + user, function(data) {
            if (data !== null) {
                var templates;
                if (window.jsrender == null) {
                    templates = window.jQuery.templates;
                } else {
                    templates = window.jsrender.templates;
                }
                var myTmpl = templates(data.template);
                var html = myTmpl.render(data.task);
                jQuery('#vt-yes').click(function () {
                    postVerification(true, data)
                });
                jQuery('#vt-no').click(function () {
                    postVerification(false, data)
                });
                jQuery('#vt-unsure').click(function () {
                    postVerification(null, data)
                });
                jQuery('#vt-template').html(html);
                jQuery('#veritask').show();
            }
        });
    }

    function postVerification(answer, data) {
        var verification = { _id: uuid.v1(), verifier: data.verifier, task_id: data.task._id, value: answer };
        jQuery.ajax({
            url:'@routes.Tasksets.processVerificationPost.absoluteURL',
            method:"POST",
            data: JSON.stringify(verification),
            contentType:"application/json",
            dataType:"json",
            success: function(data) {
                console.log(data);
                jQuery('#vt-template').html(data);
                jQuery('#vt-template').show();
                jQuery('#veritask').hide();
            }
        });
    }

    return {
        challengeUser: challengeUser,
    };
}(); // We call our anonymous function immediately
