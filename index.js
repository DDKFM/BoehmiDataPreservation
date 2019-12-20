$(document).ready(function(){
    sendRequest(20, 0, true)
});



function sendRequest(limit, offset, newRequest) {
    var query = $('#query').val()
    if(newRequest) {
        $('#paging').html("")
        $('#paging').append("<li class=\"disabled\"><a href=\"#!\"><i class=\"material-icons\">chevron_left</i></a></li>")
        $('#paging').append("<li class=\"waves-effect\"><a href=\"#!\"><i class=\"material-icons\">chevron_right</i></a></li>")
    }
    console.log(query)
    $.get('/v1/search?limit=' + limit + "&offset=" + (offset * limit) + (query == "" ? "" : "&query=" + query), function(data) {
        $('#gifs').html("")
        var gifs = data.gifs
        var pages = data.count / data.limit
        for(var page = 0; page <= pages ; page++) {
            var active = offset == page ? "active" : "waves-effect"
            if(newRequest)
                $('#paging').append("<li class=\"" + active + "\"'><a href=\"javascript:sendRequest(20, " + page + ", false)\">" + page + "</a></li>")
        }
        gifs.forEach(function(item, index) {
            $('#gifs').append("<div class=\"card\">\n" +
                "        <div class=\"card-content\">" +
                "           <video autoplay loop playsinline=\"\" style='padding: 0 auto' preload=\"auto\" src=\"" + item.url + "/data\" type=\"video/mp4\"></video><br/>" +
                "        </div>\n" +
                "        <div class=\"card-action\">\n" +
                "          <a href='" + item.url + "/data' download='" + item.url.replace("/v1/gifs/", "")  + ".mp4'><i class=\"material-icons left\">cloud_download</i> Download</a>\n" +
                "        </div>\n" +
                "      </div>")
        })
    })
}
