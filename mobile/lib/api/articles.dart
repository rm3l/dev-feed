//The MIT License (MIT)
//
//Copyright (c) 2019 Armel Soro
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.
import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dev_feed/api/api.dart';

/*
    id: ID!
    timestamp: Long!
    title: String!
    description: String
    url: String!
    domain: String!
    tags: [String]
    screenshot: Screenshot
 */
class Article {
  final String id;

  final int timestamp;

  final String title;

  final String description;

  final String url;

  final String domain;

  final List<String> tags;

  final ArticleLinkScreenshot screenshot;

  final ArticleParsed parsed;

  bool starred = false;

  Article(this.title, this.url,
      {this.id,
      this.timestamp,
      this.description,
      this.domain,
      this.tags,
      this.screenshot,
      this.parsed});

  Article.fromJson(Map<String, dynamic> json)
      : id = json['id'],
        timestamp = json['timestamp'] as int,
        title = json['title'],
        url = json['url'],
        description = json['description'],
        domain = json['domain'],
        tags = convertTags(json['tags']),
        screenshot = json['screenshot'] != null
            ? ArticleLinkScreenshot.fromJson(json['screenshot'])
            : null,
        parsed = json['parsed'] != null
            ? ArticleParsed.fromJson(json['parsed'])
            : null;

  static List<String> convertTags(List<dynamic> tags) {
    final result = <String>[];
    for (var tag in tags) {
      result.add(tag.toString());
    }
    return result;
  }

  String toSharedPreferencesString() => "{"
      "\"title\" : \"${this.title}\","
      "\"url\" : \"${this.url}\""
      "}";

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Article &&
          runtimeType == other.runtimeType &&
          title == other.title &&
          url == other.url;

  @override
  int get hashCode => title.hashCode ^ url.hashCode;
}

/*
data: String
    height: Int
    width: Int
    mimeType: String
 */
class ArticleLinkScreenshot {
  final String mimeType;

  final int width;

  final int height;

  final String data;

  final Uint8List dataBytes;

  const ArticleLinkScreenshot(
      {this.data, this.width, this.height, this.mimeType, this.dataBytes});

  ArticleLinkScreenshot.fromJson(Map<String, dynamic> json)
      : data = json['data'],
        width = json['width'],
        height = json['height'],
        mimeType = json['mimeType'],
        dataBytes = json['data'] != null
            ? base64.decode(json['data'].toString())
            : null;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ArticleLinkScreenshot &&
          runtimeType == other.runtimeType &&
          data == other.data;

  @override
  int get hashCode => data.hashCode;
}

/*
    url: ID!
    title: String
    author: String
    published: String
    image: String
    videos: [String]
    keywords: [String]
    description: String
    body: String
 */
class ArticleParsed {
  final String url;
  final String title;
  final String author;
  final String published;
  final String image;
  final String description;
  final String body;
  final List<String> videos;
  final List<String> keywords;

  const ArticleParsed(this.url,
      {this.title,
      this.author,
      this.published,
      this.image,
      this.description,
      this.body,
      this.videos,
      this.keywords});

  static List<String> convertToListString(List<dynamic> elements) {
    final result = <String>[];
    if (elements == null) {
      return result;
    }
    for (var element in elements) {
      if (element == null) {
        result.add(element.toString());
      }
    }
    return result;
  }

  ArticleParsed.fromJson(Map<String, dynamic> json)
      : url = json['url'],
        title = json['title'],
        author = json['author'],
        published = json['published'],
        image = json['image'],
        description = json['description'],
        body = json['body'],
        videos = convertToListString(json['videos']),
        keywords = convertToListString(json['keywords']);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ArticleParsed &&
          runtimeType == other.runtimeType &&
          url == other.url;

  @override
  int get hashCode => url.hashCode;
}

/*
from: String
    to: String
    search: String
    tags: [String]
    titles: [String]
    urls: [String]
 */
class ArticleFilter {
  final int from;
  final int to;
  final String search;
  final List<String> tags;
  final List<String> titles;
  final List<String> urls;

  const ArticleFilter(
      {this.from, this.to, this.search, this.tags, this.titles, this.urls});
}

class ArticlesClient {
//  LoadingCache<String,List<Article>> _recentArticlesCache;

  static final ArticlesClient _singleton = ArticlesClient._internal();

  //Leveraging Dart Factory constructors to build singletons
  factory ArticlesClient() {
    return _singleton;
  }

  ArticlesClient._internal() {
//    _recentArticlesCache = LoadingCache<String,List<Article>>(
//        _retrieveData,
//        maximumSize: 1,
//        expiresAfterWrite: const Duration(days: 1)
//    );
  }

//  Future<List<Article>> _retrieveData(final String key) {
//    if (key == "recentArticles") {
//      return _fetchRecentArticles();
//    } else {
//      throw UnsupportedError("operation not supported: $key");
//    }
//  }

  static List<Article> searchInArticles(
      List<Article> allArticles, String search) {
    if (allArticles == null) {
      return List<Article>(0);
    }
    var articlesFiltered = allArticles;
    if (search != null && search.isNotEmpty) {
      final searchLowerCase = search.toLowerCase();
      articlesFiltered = allArticles.where((article) {
        if (article.title.toLowerCase().contains(searchLowerCase)) {
          return true;
        }
        if (article.domain.toLowerCase().contains(searchLowerCase)) {
          return true;
        }
        if (article.tags != null &&
            article.tags
                .map((tag) => tag.toLowerCase())
                .any((tag) => tag.contains(searchLowerCase))) {
          return true;
        }
        return false;
      }).toList();
    }
    return articlesFiltered;
  }

  Future<List<Article>> _getArticles(
      String graphqlQuery, String queryKey) async {
    var map = await issueGraphQLQuery(graphqlQuery);
    final dataMap = map["data"];
    var recentArticlesList = dataMap[queryKey];
    if (recentArticlesList == null) {
      throw StateError('No content');
    }
    List<Article> result = [];
    for (var recentArticle in recentArticlesList) {
      result.add(Article.fromJson(recentArticle));
    }
    return result;
  }

  Future<List<Article>> getRecentArticles() async {
    final String query = "query { \n "
        " recentArticles { \n "
        "   id \n "
        "   timestamp \n "
        "   title \n "
        "   description \n "
        "   url \n "
        "   domain \n "
        "   tags \n "
//        "   screenshot { \n "
//        "       height \n "
//        "       width \n "
//        "       mimeType \n "
//        "       data \n "
//        "   } \n "
        "   parsed { \n "
        "       image \n "
        "       description \n "
//        "       body \n "
        "   } \n "
        " } \n "
        "}";
    return _getArticles(query, "recentArticles");
  }

  Future<List<Article>> getFavoriteArticles(
      List<Article> articlesToLookup) async {
    final urls =
        articlesToLookup.map((article) => "\"${article.url}\"").join(",");
    final String query = "query { \n "
        //" articles(filter: {titles: [$titles], urls: [$urls]}) { \n "
        " articles(filter: {urls: [$urls]}) { \n "
        "   id \n "
        "   timestamp \n "
        "   title \n "
        "   description \n "
        "   url \n "
        "   domain \n "
        "   tags \n "
//        "   screenshot { \n "
//        "       height \n "
//        "       width \n "
//        "       mimeType \n "
//        "       data \n "
//        "   } \n "
        "   parsed { \n "
        "       image \n "
        "       description \n "
//        "       body \n "
        "   } \n "
        " } \n "
        "}";
    return _getArticles(query, "articles");
  }

  Future<List<Article>> getAllButRecentArticles() async {
    final String query = "query { \n "
        " allButRecentArticles { \n "
        "   id \n "
        "   timestamp \n "
        "   title \n "
        "   description \n "
        "   url \n "
        "   domain \n "
        "   tags \n "
//        "   screenshot { \n "
//        "       height \n "
//        "       width \n "
//        "       mimeType \n "
//        "       data \n "
//        "   } \n "
        "   parsed { \n "
        "       image \n "
        "       description \n "
//        "       body \n "
        "   } \n "
        " } \n "
        "}";
    return _getArticles(query, "allButRecentArticles");
  }

  Future<List<Article>> getArticlesForDate(String date) async {
    final String query = "query { \n "
        " articles(filter: {from: \"$date\", to: \"$date\"}) { \n "
        "   id \n "
        "   timestamp \n "
        "   title \n "
        "   description \n "
        "   url \n "
        "   domain \n "
        "   tags \n "
//        "   screenshot { \n "
//        "       height \n "
//        "       width \n "
//        "       mimeType \n "
//        "       data \n "
//        "   } \n "
        "   parsed { \n "
        "       image \n "
        "       description \n "
//        "       body \n "
        "   } \n "
        " } \n "
        "}";
    return _getArticles(query, "articles");
  }

  Future<List<Article>> getAllArticles({ArticleFilter filter}) async {
    final filterElements = <String>[];
    if (filter != null) {
      if (filter.from != null) {
        filterElements.add("from: \"${filter.from.toString()}\"");
      }
      if (filter.to != null) {
        filterElements.add("to: \"${filter.to.toString()}\"");
      }
      if (filter.search != null) {
        filterElements.add("search: \"${filter.search}\"");
      }
      if (filter.tags != null) {
        filterElements
            .add("tags: [${filter.tags.map((tag) => "\"$tag\"").join(",")}]");
      }
      if (filter.titles != null) {
        filterElements.add(
            "titles: [${filter.titles.map((title) => "\"$title\"").join(",")}]");
      }
      if (filter.titles != null) {
        filterElements
            .add("urls: [${filter.urls.map((url) => "\"$url\"").join(",")}]");
      }
    }
    final String query = "query { \n "
        " articles "
        "${filterElements.isNotEmpty ? "(filter: { ${filterElements.join(", ")} }) " : ""}"
        "{ \n "
        "   id \n "
        "   timestamp \n "
        "   title \n "
        "   description \n "
        "   url \n "
        "   domain \n "
        "   tags \n "
//        "   screenshot { \n "
//        "       height \n "
//        "       width \n "
//        "       mimeType \n "
//        "       data \n "
//        "   } \n "
        "   parsed { \n "
        "       image \n "
        "       description \n "
//        "       body \n "
        "   } \n "
        " } \n "
        "}";
    return _getArticles(query, "articles");
  }
}
