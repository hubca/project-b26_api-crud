$(function() {

    $('#side-menu').metisMenu();

});

//Loads the correct sidebar on window load,
//collapses the sidebar on window resize.
// Sets the min-height of #page-wrapper to window size
$(function() {
    $(window).bind("load resize", function() {
        topOffset = 50;
        width = (this.window.innerWidth > 0) ? this.window.innerWidth : this.screen.width;
        if (width < 768) {
            $('div.navbar-collapse').addClass('collapse');
            topOffset = 100; // 2-row-menu
        } else {
            $('div.navbar-collapse').removeClass('collapse');
        }

        height = ((this.window.innerHeight > 0) ? this.window.innerHeight : this.screen.height) - 1;
        height = height - topOffset;
        if (height < 1) height = 1;
        if (height > topOffset) {
            $("#page-wrapper").css("min-height", (height) + "px");
        }
    });



    $(function() {
         //alert(str.slice(0, -2));
        layoutSidebarNav();
        $("ul.nav-tabs li a").on("click", changeDocTabs);
    });

    // ensures that the current navigation is open on the sidebar menu if possible
    function layoutSidebarNav() {

        var url = window.location;

        var element = $('ul.nav a').filter(
            function() {

                //var thisUrlModify = this.href.replace("create", "update");
                //var thisUrl = this.href;
                //var shortenedUrl = thisUrl.indexOf("/index/") > -1 ? thisUrl.split("/index/")[0] : thisUrl;
                //alert("this.href" + this.href + "<br />" + "thisUrlModify.slice" + thisUrlModify.slice(0, -3));

                return this.href == url || url.href.indexOf(this.href) === 0;
                //|| url.href.indexOf(shortenedUrl) === 0; // todo - sidebar not remembering when using update active tab not 1
             })
            .addClass('active')
            .parent()
            .parent()
            .addClass('in')
            .parent();

        if(element.is('li')) {
            element.addClass('active');
        }

    }

    // change collection tab panel when form tab panel navigation is changed
    function changeDocTabs() {

        var id = $(this).attr('href').replace('#', '#d')
        $('.tab-content .docs.tab-pane').removeClass('active in')//.hide()
        $('.tab-content .docs'+id).addClass('active in')//.show()

    }

});