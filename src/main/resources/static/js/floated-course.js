(function ($, fuzzyhound) {
    function getImageUrl(url) {
        if (url && url !== '')
            return url;
        return 'https://zhcet-backend.firebaseapp.com/static/img/account.svg';
    }

    function itemTemplate(suggestion) {
        return [
            "<div class='suggestion'>",
            '<span><img src="' + getImageUrl(suggestion._item.userDetailsAvatarUrl) + '" class="avatar-img rounded-circle"/></span>',
            "<span class='pad-top name'>", fuzzyhound.get().highlight(suggestion.userName,"userName") ,"</span> ",
            "<span class='pad-top faculty-id'><i>(", fuzzyhound.get().highlight(suggestion.facultyId, "facultyId"), ")</i></span>",
            "<span class='pad-top department capsule p-small'>", suggestion._item.userDepartmentName, "</span> ",
            "</div>"
        ].join("");
    }

    function attachRemove() {
        $('.remove').click(function (event) {
            $(event.target).closest('.in-charge').remove();
        });
    }

    function addItem(template, item) {
        item.userDetailsAvatarUrl = getImageUrl(item.userDetailsAvatarUrl);
        $('.in-charge-container').append(tmpl(template, item));
        attachRemove();
    }

    var all = false;
    var department = $('#department').html();
    var index = ["facultyId", "userName"];
    var baseUrl = "/department/" + department + "/api/faculty";
    fuzzyhound.setSource(baseUrl, index);

    $(document).ready(function () {
        $('#faculty-modal').modal('show');

        var inChargeTemplate = $('#in-charge-template').html();

        // Unfloat Code
        var deleteCode = $('#delete-code');
        var deleteButton = $('#delete-button');
        var code = $('#course-code').val();
        deleteCode.on('keyup', function () {
            deleteButton.prop('disabled', deleteCode.val() !== code);
        });

        // Search In-Charge
        var searchBox = $('.incharges');
        searchBox.typeahead({
            highlight: false,
            minLength: 2
        }, {
            name: 'incharges',
            limit: 100,
            source: fuzzyhound.get(),
            display: "item.userName",
            templates: {
                suggestion: itemTemplate
            }
        }).on('typeahead:select', function (ev, suggestion) {
            addItem(inChargeTemplate, suggestion._item);
        });

        var toggle = $('#toggle-more');
        toggle.click(function () {
            all = !all;
            fuzzyhound.setSource(baseUrl + '?all=' + all, index);
            toggle.text(all ? 'Less' : 'More');
        });

        attachRemove();
    });
}(jQuery, fuzzyhound));