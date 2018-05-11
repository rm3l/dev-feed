import 'dart:async';

import 'package:awesome_dev/api/articles.dart';
import 'package:awesome_dev/ui/widgets/article_widget.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum IndicatorType { overscroll, refresh }

class LatestNews extends StatefulWidget {
  const LatestNews();

  @override
  State<StatefulWidget> createState() => LatestNewsState();
}

class LatestNewsState extends State<LatestNews> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      GlobalKey<RefreshIndicatorState>();

  List<Article> _articles;
  List<Article> _articlesFiltered;

  final _searchInputController = TextEditingController();

  String _search;
  bool _searchInputVisible = false;

  @override
  void initState() {
    super.initState();
    _fetchArticles();
  }

  Future<List<Article>> _loadArticles() async {
    _refreshIndicatorKey.currentState.show();
    final articlesClient = ArticlesClient();
    final recentArticlesAll = await articlesClient.getRecentArticles();

    final prefs = await SharedPreferences.getInstance();
    final favorites = prefs.getStringList("favs") ?? [];
    for (var article in recentArticlesAll) {
      article.starred = favorites.contains(article.toSharedPreferencesString());
    }
    return recentArticlesAll;
  }

  Future<Null> _fetchArticles() async {
    try {
      final recentArticles = await _loadArticles();
      final recentArticlesFiltered =
          ArticlesClient.searchInArticles(recentArticles, _search);
      setState(() {
        _articles = recentArticles;
        _searchInputVisible = _articles.isNotEmpty;
        _articlesFiltered = recentArticlesFiltered;
      });
    } on Exception catch (e) {
      Scaffold.of(context).showSnackBar(SnackBar(
            content: Text("Internal Error: ${e.toString()}"),
          ));
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
        key: _refreshIndicatorKey,
        onRefresh: _fetchArticles,
        child: Container(
          child: Column(
            children: <Widget>[
              Align(
                  alignment: Alignment.topCenter,
                  child: Container(
                    padding: const EdgeInsets.only(left: 8.0),
                    child: Stack(
                      alignment: const Alignment(1.0, 1.0),
                      children: <Widget>[
                        TextField(
                          controller: _searchInputController,
                          enabled: _searchInputVisible,
                          decoration: InputDecoration(
                              border: const UnderlineInputBorder(),
                              hintText: 'Search...'),
                          onChanged: (String criteria) {
                            final recentArticlesFiltered = ArticlesClient
                                .searchInArticles(_articles, criteria);
                            setState(() {
                              _search = criteria;
                              _articlesFiltered = recentArticlesFiltered;
                            });
                          },
                        ),
                        FlatButton(
                            onPressed: () {
                              _searchInputController.clear();
                              setState(() {
                                _search = null;
                                _articlesFiltered = _articles;
                              });
                            },
                            child: Icon(Icons.clear))
                      ],
                    ),
                  )),
              Container(
                child: Expanded(
                    child: ListView.builder(
                  padding: EdgeInsets.all(8.0),
                  itemCount: _articlesFiltered?.length ?? 0,
                  itemBuilder: (BuildContext context, int index) {
                    return ArticleWidget(
                      article: _articlesFiltered[index],
//                    onCardClick: () {
//  //                      Navigator.of(context).push(
//  //                          FadeRoute(
//  //                            builder: (BuildContext context) => BookNotesPage(_items[index]),
//  //                            settings: RouteSettings(name: '/notes', isInitialRoute: false),
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
