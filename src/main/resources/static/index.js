var app = new Vue({
    el: '#app',
    data: {
        searchQuery : '',
        currentPage : 0,
        pageCount : 0,
        gifs : [],
    },
    methods : {
        sendRequest : function (limit, page) {
            this.currentPage = page
            var query = app.searchQuery == '' ? '' : '&query=' + app.searchQuery
            var offset = page * limit
            $.get('/v1/search?limit=' + limit + "&offset=" + offset + query, function(response) {
                app.gifs = response.gifs
                app.pageCount = Math.ceil(response.count / response.limit)
                $('.chips-placeholder').chips({
                    placeholder: 'Tag hinzufügen',
                    secondaryPlaceholder: '+Tag',
                    onChipAdd : function() {
                        var chips = $('#' + this.el.id + " .chip").text().replace("<i class=\"material-icons close\">close</i>", "")
                        console.log(chips.split(" "))
                        console.log(this)
                        console.log(this.el.id)
                    }
                });
            })
        },
        sendKeywords : function(tweetId, keywords) {

        },
        getImageData : function(url) {
            return url + "/data"
        },
        getImageFilename : function(url) {
            return url.replace('/v1/gifs/', '') + ".mp4"
        },
        getTweetId : function(url) {
            return url.replace('/v1/gifs/', '')
        },
        nextPage : function() {
            return app.currentPage + 1 % app.pageCount
        },
        prevPage : function() {
            return app.currentPage == 0 ? 0 : app.currentPage - 1;
        },
        onEnter : function(gif) {
            var tweetId = app.getTweetId(gif.url)
            console.log(tweetId)
            var newChip = $('#' + tweetId).val()
            console.log(newChip)
            gif.keywords.push(newChip)
            $.ajax({
                type: "POST",
                url: "/v1/gifs/" + tweetId + "/keywords",
                data: JSON.stringify(gif.keywords),
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                success: function(data){console.log(data);},
                failure: function(errMsg) {
                    console.log(errorMsg)
                }
            });
            $('#' + tweetId).val("")
        },
        onDelete : function(gif, keyword) {
            var tweetId = app.getTweetId(gif.url)
            console.log(tweetId)
            var newChip = $('#' + tweetId).val()
            console.log(newChip)
            gif.keywords.splice( gif.keywords.indexOf(keyword), 1 );
            $.ajax({
                type: "POST",
                url: "/v1/gifs/" + tweetId + "/keywords",
                data: JSON.stringify(gif.keywords),
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                success: function(data){console.log(data);},
                failure: function(errMsg) {
                    console.log(errorMsg)
                }
            });
            $('#' + tweetId).val("")
        }
    }
});

app.searchQuery = ""
app.sendRequest(20, 0)
