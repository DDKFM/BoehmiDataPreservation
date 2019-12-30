var router = new VueRouter({
    mode: 'history',
    routes: []
});

var app = new Vue({
    router,
    el: '#app',
    data: {
        searchQuery : '',
        currentPage : 0,
        pageCount : 0,
        gifs : [],
        limit : 10,
        favorites : [],
        parameters : {},
        showOnlyFavorites : false,
        keywords : {}
    },
    methods : {
        sendRequest : function (limit, page) {
            this.currentPage = page
            var query = app.searchQuery == '' ? '' : '&query=' + app.searchQuery
            var offset = page * limit
            var url = "/v1/search"
            var data = ''
            var method = "GET"
            if(this.showOnlyFavorites) {
                url = "/v1/searchByIds"
                data = JSON.stringify(app.favorites)
                method = "POST"
            }
            $.ajax({
                type: method,
                url: url + '?limit=' + limit + "&offset=" + offset + query,
                data: data,
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                success: function(data){
                    app.showGifs(data)

                },
                failure: function(errMsg) {
                    console.log(errorMsg)
                }
            });
        },
        showFavorites : function() {
            this.showOnlyFavorites = !this.showOnlyFavorites
            this.sendRequest(this.limit, 0)
        },
        showGifs : function(response) {
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
        },
        openModal : function(gif) {
            var tweetId = app.getTweetId(gif.url)
            $("#" + tweetId + "_modal").modal();
            $("#" + tweetId + "_modal").modal('open')
        },
        deleteGif : function(gif) {
            console.log("Löschen")
            var tweetId = app.getTweetId(gif.url)
            $.ajax({
                type: "DELETE",
                url: "/v1/gifs/" + tweetId,
                success: function(data){
                    console.log("Gif wurde gelöscht");
                    app.sendRequest(app.limit, app.currentPage)
                },
                failure: function(errMsg) {
                    console.log(errorMsg)
                }
            });
        },
        addAsFavorite : function(gif) {
            var tweetId = app.getTweetId(gif.url)
            if(app.isGifFavorite(gif)) {
                app.favorites.splice(app.favorites.indexOf(tweetId), 1)
            } else {
                app.favorites.push(tweetId)
            }
        },
        isGifFavorite : function(gif) {
            var tweetId = app.getTweetId(gif.url)
            return app.favorites.indexOf(tweetId) != -1
        },
        showKeywords : function() {
            $("#keywordsModal").modal();
            $("#keywordsModal").modal('open')
            $.get('/v1/keywords/top?top=10', function(data) {
                app.keywords = data
            })
        },
        searchForKeyword : function(keyword) {
            this.searchQuery = keyword
            this.sendRequest(this.limit, 0)
            $("#keywordsModal").modal('close')
        }
    },
    mounted() {
        console.log(this.$route)
        this.parameters = this.$route.query
        //console.log(parameters)
        if (localStorage.favorites) {
            this.favorites = JSON.parse(localStorage.favorites);
        }
    },
    watch: {
        favorites(favorites) {
            localStorage.favorites= JSON.stringify(this.favorites);
        }
    }
});

app.searchQuery = ""
app.sendRequest(app.limit, 0)
$('select').formSelect();
$('#menu').floatingActionButton();
