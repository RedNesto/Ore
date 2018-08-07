/*
 * ==================================================
 *  _____             _
 * |     |___ ___    |_|___
 * |  |  |  _| -_|_  | |_ -|
 * |_____|_| |___|_|_| |___|
 *                 |___|
 *
 * By Walker Crouse (windy) and contributors
 * (C) SpongePowered 2016-2017 MIT License
 * https://github.com/SpongePowered/Ore
 *
 * Home page specific script
 *
 * ==================================================
 */

/*
 * ==================================================
 * =               External constants               =
 * ==================================================
 */

var PROJECTS_PER_CLICK = 50;
var CATEGORY_STRING = null;
var SORT_STRING = null;
var QUERY_STRING = null;

var NUM_SUFFIXES = ["", "k", "m"];
var currentlyLoaded = 0;

/*
 * ==================================================
 * =                Helper functions                =
 * ==================================================
 */

function abbreviateStat(stat) {
    stat = stat.toString().trim();
    if (parseInt(stat) < 1000) return stat;
    var suffix = NUM_SUFFIXES[Math.min(2, Math.floor(stat.length / 3))];
    return stat[0] + '.' + stat[1] + suffix;
}

/*
 * ==================================================
 * =                   Doc ready                    =
 * ==================================================
 */

$(function() {
    $('.project-table').find('tbody').find('.stat').each(function() {
        $(this).text(abbreviateStat($(this).text()));
    });

    $('.dismiss').click(function() {
        $('.search-header').fadeOut('slow');
        var url = '/';
        if (CATEGORY_STRING || SORT_STRING)
            url += '?';
        if (CATEGORY_STRING)
            url += 'categories=' + CATEGORY_STRING;
        if (SORT_STRING) {
            if (CATEGORY_STRING)
                url += '&';
            url += '&sort=' + SORT_STRING;
        }
        go(url);
    });

    // Initialize sorting selection
    $('.select-sort').on('change', function() {
        var url = '/?sort=' + $(this).find('option:selected').val();
        if (CATEGORY_STRING) url += '&categories=' + CATEGORY_STRING;
        go(url);
    });

    // Initialize more button
    $('.btn-more').click(function() {
        var ajaxUrl = '/api/projects?limit=' + PROJECTS_PER_CLICK + '&offset=' + currentlyLoaded;
        if (CATEGORY_STRING) ajaxUrl += '&categories=' + CATEGORY_STRING;
        if (SORT_STRING) ajaxUrl += '&sort=' + SORT_STRING;
        if (QUERY_STRING) ajaxUrl += '&q=' + QUERY_STRING;

        // Request more projects
        $('.btn-more').html('<i class="fa fa-spinner fa-spin"></i>');
        $.ajax({
            url: ajaxUrl,
            dataType: 'json',
            success: function(projects) {
                for (var i in projects) {
                    if (!projects.hasOwnProperty(i)) continue;
                    var project = projects[i];
                    var category = project.category;

                    // Add received project to table
                    var projectRow = $('#row-project').clone().removeAttr('id');
                    var nameCol = projectRow.find('.name');
                    projectRow.find('.category').find('i').attr('title', category.title).addClass(category.icon);
                    nameCol.find('strong').find('a').attr('href', project.href).text(project.name);
                    nameCol.find('i').attr('title', project.description).text(project.description);
                    projectRow.find('.author').find('a').attr('href', '/' + project.owner).text(project.owner);
                    projectRow.find('.views').text(abbreviateStat(project.views));
                    projectRow.find('.downloads').text(abbreviateStat(project.downloads));
                    projectRow.find('.stars').text(abbreviateStat(project.stars));
                    $('.project-table').find('tbody').append(projectRow);
                }
                currentlyLoaded += projects.length;
                $('.btn-more').html('<strong>More</strong>');
            }
        });
    });
});
