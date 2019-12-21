$(document).ready(function(){
    sendRequest(20, 0, true)
});



function sendRequest(limit, offset, newRequest) {
    var query = $('#query').val()
    if(newRequest) {
        $('#paging').html("")
    }
    console.log(query)
    $.get('/v1/search?limit=' + limit + "&offset=" + (offset * limit) + (query == "" ? "" : "&query=" + query), function(data) {
        $('#gifs').html("")
        var gifs = data.gifs
        var pages = data.count / data.limit
        if(newRequest) {
            for(var page = 0; page <= pages ; page++) {
                var active = offset == page ? "active" : "waves-effect"
                if(newRequest)
                    $('#paging').append("<li class='page " + active + "' id='page_" + page + "'><a href='javascript:sendRequest(20, " + page + ", false)'>" + page + "</a></li>")
            }
        }
        gifs.forEach(function(item, index) {
            $('#gifs').append("<div class=\"card\">\n" +
                "        <div class=\"card-content\">" +
                "           <video autoplay controls loop playsinline=\"\" style='width: 100%;max-width: 500px' preload='auto' src=\"" + item.url + "/data\" type=\"video/mp4\"></video><br/>" +
                "        </div>\n" +
                "        <div class=\"card-action\">\n" +
                "          <a href='" + item.url + "/data' download='" + item.url.replace("/v1/gifs/", "")  + ".mp4'><i class='fa fa-download'></i></a>" +
                "           <a href='" + item.tweetUrl + "' title='Link zum Tweet' target=\"_blank\"><i class='fa fa-twitter'></i></a>" +
                "        </div>\n" +
                "      </div>")
        })
    })
}
