import 'dart:async';

import 'package:awesome_dev/api/api.dart';

/*
    id: ID!
    date: String!
    title: String!
    description: String
    url: String!
    domain: String!
    tags: [String]
    screenshot: Screenshot
 */
class Article {
  String id;

  String date;

  String title;

  String description;

  String url;

  String domain;

  List<String> tags;

  ArticleLinkScreenshot screenshot;

  Article(this.id, this.title, this.url, this.date, this.description,
      this.domain, this.tags, this.screenshot);

  Article.fromJson(Map<String, dynamic> json)
      : id = json['id'],
        date = json['date'],
        title = json['title'],
        url = json['url'],
        description = json['description'],
        domain = json['domain'],
        tags = json['tags'],
        screenshot = new ArticleLinkScreenshot.fromJson(json['screenshot']);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
          other is Article &&
              runtimeType == other.runtimeType &&
              title == other.title &&
              url == other.url;

  @override
  int get hashCode =>
      title.hashCode ^
      url.hashCode;
}

/*
data: String
    height: Int
    width: Int
    mimeType: String
 */
class ArticleLinkScreenshot {
  String mimeType;

  int width;

  int height;

  String data;

  ArticleLinkScreenshot(this.data, this.width, this.height, this.mimeType);

  ArticleLinkScreenshot.fromJson(Map<String, dynamic> json)
      : data = json['data'],
        width = json['width'],
        height = json['height'],
        mimeType = json['mimeType'];

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

  Future<List<Article>> getRecentArticles() async {
    final String query =
        "query { \n "
        " recentArticles { \n "
        "   id \n "
        "   date \n "
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
    var map = await issueGraphQLQuery(query);
    final dataMap = map["data"];
    var recentArticlesList = dataMap["recentArticles"];
    if (recentArticlesList == null) {
      throw new StateError(
          'No content');
    }
    List<Article> result = [];
    for (var recentArticle in recentArticlesList) {
      result.add(new Article.fromJson(recentArticle));
    }
    return result;
  }
}
