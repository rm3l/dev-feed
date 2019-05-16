import 'dart:async';

import 'package:awesome_dev/api/articles.dart';
import 'package:awesome_dev/ui/widgets/article_widget.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class Search extends StatefulWidget {
  const Search();

  @override
  State<StatefulWidget> createState() => _SearchState();
}

class _SearchState extends State<Search> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      GlobalKey<RefreshIndicatorState>();

  bool _initialDisplay = true;
  List<Article> _articles;

  final _searchInputController = TextEditingController();

  String _search;
  Exception _errorOnLoad;

  Future<Null> _fetchArticles() async {
    try {
      if (_search == null || _search.isEmpty) {
        setState(() {
          _initialDisplay = false;
          _articles = null;
          _errorOnLoad = null;
        });
      } else {
        final articlesClient = ArticlesClient();
        final recentArticles = await articlesClient.getAllArticles(
            filter: ArticleFilter(search: _search));
        final prefs = await SharedPreferences.getInstance();
        final favorites = prefs.getStringList("favs") ?? [];
        for (var article in recentArticles) {
          article.starred =
              favorites.contains(article.toSharedPreferencesString());
        }
        setState(() {
          _initialDisplay = false;
          _articles = recentArticles;
          _errorOnLoad = null;
        });
      }
    } on Exception catch (e) {
      setState(() {
        _initialDisplay = false;
        _errorOnLoad = e;
        _articles = null;
      });
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
                          enabled: true,
                          decoration: InputDecoration(
                              border: const UnderlineInputBorder(),
                              hintText: 'Search...'),
                          onSubmitted: (String criteria) {
                            _search = criteria;
                            _refreshIndicatorKey.currentState.show();
                          },
                        ),
                        FlatButton(
                            onPressed: () {
                              _searchInputController.clear();
                              setState(() {
                                _initialDisplay = true;
                                _search = null;
                                _articles = null;
                              });
                            },
                            child: Icon(Icons.clear))
                      ],
                    ),
                  )),
              _initialDisplay
                  ? Container()
                  : Container(
                      child: Expanded(
                          child: Scrollbar(
                              child: ListView.builder(
                        padding: EdgeInsets.all(8.0),
                        itemCount: _errorOnLoad != null
                            ? 1
                            : (_articles == null || _articles.isEmpty)
                                ? 1
                                : _articles.length,
                        itemBuilder: (BuildContext context, int index) {
                          if (_errorOnLoad != null) {
                            return Center(
                              child: Text(
                                "Internal error: ${_errorOnLoad.toString()}",
                                style: TextStyle(
                                    color: Colors.red, fontSize: 20.0),
                              ),
                            );
                          }
                          if (_articles == null || _articles.isEmpty) {
                            return Center(
                              child: Text(
                                "No article found",
                                style: TextStyle(fontSize: 20.0),
                              ),
                            );
                          }
                          return ArticleWidget(
                            article: _articles[index],
                            onStarClick: () {
                              setState(() {
                                _initialDisplay = false;
                                _articles[index].starred =
                                    !_articles[index].starred;
                              });
                              //                      Repository.get().updateBook(_items[index]);
                            },
                          );
                        },
                      ))),
                    ),
            ],
          ),
        ));
  }
}
