import 'dart:async';

import 'package:awesome_dev/api/articles.dart';
import 'package:awesome_dev/ui/widgets/article_widget.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum IndicatorType { overscroll, refresh }

class LatestNews extends StatefulWidget {
  const LatestNews();

  @override
  State<StatefulWidget> createState() => new LatestNewsState();
}

class LatestNewsState extends State<LatestNews> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      new GlobalKey<RefreshIndicatorState>();

  List<Article> _articles;
  List<Article> _articlesFiltered;

  final _searchInputController = new TextEditingController();

  String _search;
  bool _searchInputVisible = false;

  @override
  void initState() {
    super.initState();
    _fetchArticles();
  }

  List<Article> _searchInArticles(
      List<Article> recentArticlesAll, String search) {
    if (recentArticlesAll == null) {
      return new List<Article>(0);
    }
    var recentArticlesFiltered = recentArticlesAll;
    if (search != null && search.isNotEmpty) {
      final searchLowerCase = search.toLowerCase();
      recentArticlesFiltered = recentArticlesAll.where((article) {
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
    return recentArticlesFiltered;
  }

  Future<List<Article>> _loadArticles() async {
    _refreshIndicatorKey.currentState.show();
    final articlesClient = new ArticlesClient();
    final recentArticlesAll = await articlesClient.getRecentArticles();

    final prefs = await SharedPreferences.getInstance();
    final favorites = prefs.getStringList("favs") ?? [];
    for (var article in recentArticlesAll) {
      article.starred = favorites.contains(article.toSharedPreferencesString());
    }
    return recentArticlesAll;
  }

  Future<Null> _fetchArticles() async {
    _refreshIndicatorKey.currentState.show();
    try {
      final recentArticles = await _loadArticles();
      final recentArticlesFiltered = _searchInArticles(recentArticles, _search);
      setState(() {
        _articles = recentArticles;
        _searchInputVisible = _articles.isNotEmpty;
        _articlesFiltered = recentArticlesFiltered;
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
          child: new Column(
            children: <Widget>[
              new Align(
                  alignment: Alignment.topCenter,
                  child: new Container(
                    padding: const EdgeInsets.only(left: 8.0),
                    child: new Stack(
                      alignment: const Alignment(1.0, 1.0),
                      children: <Widget>[
                        new TextField(
                          controller: _searchInputController,
                          enabled: _searchInputVisible,
                          decoration: new InputDecoration(
                              border: const UnderlineInputBorder(),
                              hintText: 'Search...'),
                          onChanged: (String criteria) {
                            final recentArticlesFiltered =
                                _searchInArticles(_articles, criteria);
                            setState(() {
                              _search = criteria;
                              _articlesFiltered = recentArticlesFiltered;
                            });
                          },
                        ),
                        new FlatButton(
                            onPressed: () {
                              _searchInputController.clear();
                              setState(() {
                                _search = null;
                                _articlesFiltered = _articles;
                              });
                            },
                            child: new Icon(Icons.clear))
                      ],
                    ),
                  )),
              new Container(
                child: new Expanded(
                    child: new ListView.builder(
                  padding: new EdgeInsets.all(8.0),
                  itemCount: _articlesFiltered?.length ?? 0,
                  itemBuilder: (BuildContext context, int index) {
                    return new ArticleWidget(
                      article: _articlesFiltered[index],
//                    onCardClick: () {
//  //                      Navigator.of(context).push(
//  //                          new FadeRoute(
//  //                            builder: (BuildContext context) => new BookNotesPage(_items[index]),
//  //                            settings: new RouteSettings(name: '/notes', isInitialRoute: false),
//  //                          ));
//                    },
                      onStarClick: () {
                        setState(() {
                          _articlesFiltered[index].starred =
                              !_articlesFiltered[index].starred;
                        });
                        //                      Repository.get().updateBook(_items[index]);
                      },
                    );
                  },
                )),
              ),
            ],
          ),
        ));
  }
}
