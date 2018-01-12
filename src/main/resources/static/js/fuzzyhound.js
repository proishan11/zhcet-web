var fuzzyhound = (function ($, FuzzySearch) {
    var fuzzyhound = new FuzzySearch({output_limit: 6, output_map:"alias"});

    $.ajaxSetup({cache: true});
    function setSource(url, keys, loaded) {
        $.getJSON(url).then(function (response) {
            fuzzyhound.setOptions({
                source: response,
                keys: keys
            });
            if ($.isFunction(loaded))
                loaded(response);
        });
    }

    return {
        get: function () {
            return fuzzyhound;
        },
        setSource: setSource
    }
}(jQuery, FuzzySearch));