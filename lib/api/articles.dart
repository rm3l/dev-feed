import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:awesome_dev/api/api.dart';

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

  bool starred = false;

  Article(this.title, this.url,
      {this.id,
      this.timestamp,
      this.description,
      this.domain,
      this.tags,
      this.screenshot});

  Article.fromJson(Map<String, dynamic> json)
      : id = json['id'],
        timestamp = json['timestamp'] as int,
        title = json['title'],
        url = json['url'],
        description = json['description'],
        domain = json['domain'],
        tags = convertTags(json['tags']),
        screenshot = new ArticleLinkScreenshot.fromJson(json['screenshot']);

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

  ArticleLinkScreenshot(
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

class ArticlesClient {
//  LoadingCache<String,List<Article>> _recentArticlesCache;

  static final ArticlesClient _singleton = new ArticlesClient._internal();

  //Leveraging Dart Factory constructors to build singletons
  factory ArticlesClient() {
    return _singleton;
  }

  ArticlesClient._internal() {
//    _recentArticlesCache = new LoadingCache<String,List<Article>>(
//        _retrieveData,
//        maximumSize: 1,
//        expiresAfterWrite: const Duration(days: 1)
//    );
  }

//  Future<List<Article>> _retrieveData(final String key) {
//    if (key == "recentArticles") {
//      return _fetchRecentArticles();
//    } else {
//      throw new UnsupportedError("operation not supported: $key");
//    }
//  }

  static List<Article> searchInArticles(
      List<Article> allArticles, String search) {
    if (allArticles == null) {
      return new List<Article>(0);
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
      throw new StateError('No content');
    }
    List<Article> result = [];
    for (var recentArticle in recentArticlesList) {
      result.add(new Article.fromJson(recentArticle));
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
        "   screenshot { \n "
        "       height \n "
        "       width \n "
        "       mimeType \n "
        "       data \n "
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
        "   screenshot { \n "
        "       height \n "
        "       width \n "
        "       mimeType \n "
        "       data \n "
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
        "   screenshot { \n "
        "       height \n "
        "       width \n "
        "       mimeType \n "
        "       data \n "
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
        "   screenshot { \n "
        "       height \n "
        "       width \n "
        "       mimeType \n "
        "       data \n "
        "   } \n "
        " } \n "
        "}";
    return _getArticles(query, "articles");
  }

  Future<List<Article>> getAllArticles() async {
    final String query = "query { \n "
        " articles { \n "
        "   id \n "
        "   timestamp \n "
        "   title \n "
        "   description \n "
        "   url \n "
        "   domain \n "
        "   tags \n "
        "   screenshot { \n "
        "       height \n "
        "       width \n "
        "       mimeType \n "
        "       data \n "
        "   } \n "
        " } \n "
        "}";
    return _getArticles(query, "articles");
  }
}
