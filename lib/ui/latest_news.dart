import 'dart:async';

import 'package:awesome_dev/api/articles.dart';
import 'package:awesome_dev/ui/widgets/article_widget.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum IndicatorType { overscroll, refresh }

class LatestNews extends StatefulWidget {
  final String search;

  LatestNews({this.search});

  @override
  State<StatefulWidget> createState() => new LatestNewsState();
}

class LatestNewsState extends State<LatestNews> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      new GlobalKey<RefreshIndicatorState>();

  List<Article> _recentArticles;

  @override
  void initState() {
    super.initState();
    _fetchArticles();
  }

  Future<Null> _fetchArticles() async {
    _refreshIndicatorKey.currentState.show();
    final articlesClient = new ArticlesClient();
    try {
      final recentArticlesAll = await articlesClient.getRecentArticles();
      var recentArticles = recentArticlesAll;
      if (widget.search != null && widget.search.isNotEmpty) {
        final searchLowerCase = widget.search.toLowerCase();
        recentArticles = recentArticlesAll.where((article) {
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
      final prefs = await SharedPreferences.getInstance();
      final favorites = prefs.getStringList("favs") ?? [];
      for (var article in recentArticles) {
        article.starred =
            favorites.contains(article.toSharedPreferencesString());
      }
      setState(() {
        _recentArticles = recentArticles;
      });
    } on Exception catch (e) {
      Scaffold.of(context).showSnackBar(new SnackBar(
            content: new Text("Internal Error: ${e.toString()}"),
          ));
    }
  }

  @override
  Widget build(BuildContext context) {
    return new RefreshIndicator(
        key: _refreshIndicatorKey,
        onRefresh: _fetchArticles,
        child: new Container(
          padding: new EdgeInsets.all(8.0),
          child: new Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: <Widget>[
              new Expanded(
                child: new ListView.builder(
                  padding: new EdgeInsets.all(8.0),
                  itemCount: _recentArticles?.length ?? 0,
                  itemBuilder: (BuildContext context, int index) {
                    return new ArticleWidget(
                      article: _recentArticles[index],
//                    onCardClick: () {
//  //                      Navigator.of(context).push(
//  //                          new FadeRoute(
//  //                            builder: (BuildContext context) => new BookNotesPage(_items[index]),
//  //                            settings: new RouteSettings(name: '/notes', isInitialRoute: false),
//  //                          ));
//                    },
                      onStarClick: () {
                        setState(() {
                          _recentArticles[index].starred =
                              !_recentArticles[index].starred;
                        });
                        //                      Repository.get().updateBook(_items[index]);
                      },
                    );
                  },
                ),
              ),
            ],
          ),
        ));
  }
}
