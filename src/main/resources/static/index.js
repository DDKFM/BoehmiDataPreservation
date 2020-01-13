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
        keywords : {},
        users : {},
        displayGifs : false
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
            } else if(app.searchQuery.startsWith("@")) {
                var actualName = app.searchQuery.replace('@', '')
                var actualUsername = app.users.find(user => user.name == actualName).username
                console.log(actualUsername)
                query = "&username=" + actualUsername
                url = "/v1/searchByUser"
            } else if(app.searchQuery.match("\\d{5,}")) {
                url = "/v1/searchByIds"
                data = JSON.stringify([app.searchQuery])
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
        getImageData : function(url, type) {
            return url + (type == "gif" ? "/gifdata" : "/data")
        },
        getImageFilename : function(url, type) {
            return url.replace('/v1/gifs/', '') + "." + type
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
        showAbout: function() {
            $("#aboutModal").modal();
            $("#aboutModal").modal('open')
        },
        searchForKeyword : function(keyword) {
            this.searchQuery = keyword
            this.sendRequest(this.limit, 0)
            $("#keywordsModal").modal('close')
        },
        fillAutocomplete : function() {
            $.get('/v1/users', function(response) {
                app.users = response
                var twitterUsers = {}
                app.users.forEach(user => {
                    twitterUsers['@' + user.name] = user.profileImage
                });
                console.log(twitterUsers)
                $('input.autocomplete').autocomplete({
                    data: twitterUsers,
                    minChars: 0,
                    onAutocomplete : function(e) {
                        console.log(e)
                        app.searchQuery = e
                        app.sendRequest(10, 0)
                    }
                }).focus(function () {
                    console.log('blub')
                        $(this).autocomplete('open')
                    });;
            })
        },
        pageBorder : function() {
            var pages = [0]
            var currentPage = app != undefined ? app.currentPage : 0
            var lastPage = app != undefined ? app.pageCount - 1 : 0
            range(11, currentPage - 5)
                .filter(i => i > 0)
                .filter(i => i < lastPage)
                .forEach(i => pages.push(i))
            pages.push(lastPage)
            return pages
        }
    },
    mounted() {
        this.parameters = this.$route.query
        //console.log(parameters)
        if (localStorage.favorites) {
            this.favorites = JSON.parse(localStorage.favorites);
        }
        if(localStorage.displayGifs) {
            this.displayGifs = localStorage.displayGifs == 'true'
        }
    },
    watch: {
        favorites(favorites) {
            localStorage.favorites= JSON.stringify(this.favorites);
        },
        displayGifs(displayGifs) {
            localStorage.displayGifs = this.displayGifs
        }
    }
});

function range(size, startAt = 0) {
    return [...Array(size).keys()].map(i => i + startAt);
}
app.searchQuery = ""
app.sendRequest(app.limit, 0)
$('select').formSelect();
$('#menu').floatingActionButton();
app.fillAutocomplete()
