import 'dart:async';
import 'dart:convert' show json;

import 'package:awesome_dev/api/articles.dart';
import 'package:awesome_dev/ui/widgets/article_widget.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum IndicatorType { overscroll, refresh }

class FavoriteNews extends StatefulWidget {
  const FavoriteNews();

  @override
  State<StatefulWidget> createState() => FavoriteNewsState();
}

class FavoriteNewsState extends State<FavoriteNews> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      GlobalKey<RefreshIndicatorState>();

  bool _initialDisplay = true;
  List<Article> _articles;
  List<Article> _articlesFiltered;

  final _searchInputController = TextEditingController();

  String _search;
  bool _searchInputVisible = false;
  Exception _errorOnLoad;

  Future<Null> _fetchArticles() async {
    try {
      final articlesClient = ArticlesClient();
      final prefs = await SharedPreferences.getInstance();
      final favorites = prefs.getStringList("favs") ?? <String>[];
      final articlesToLookup = <Article>[];
      for (var fav in favorites) {
        final Map<String, dynamic> map = json.decode(fav);
        articlesToLookup
            .add(Article(map['title'].toString(), map['url'].toString()));
      }
      final favoriteArticles =
          await articlesClient.getFavoriteArticles(articlesToLookup);
      for (var article in favoriteArticles) {
        article.starred = true;
      }
      final favoriteArticlesFiltered =
          ArticlesClient.searchInArticles(favoriteArticles, _search);
      setState(() {
        _initialDisplay = false;
        _articles = favoriteArticles;
        _searchInputVisible = _articles.isNotEmpty;
        _articlesFiltered = favoriteArticlesFiltered;
        _errorOnLoad = null;
      });
    } on Exception catch (e) {
      setState(() {
        _initialDisplay = false;
        _errorOnLoad = e;
        _articles = null;
        _searchInputVisible = false;
        _articlesFiltered = null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_initialDisplay) {
      final widget = Center(child: CircularProgressIndicator());
      _fetchArticles();
      return widget;
    }
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
                    child: Scrollbar(
                        child: ListView.builder(
                  padding: EdgeInsets.all(8.0),
                  itemCount:
                      _errorOnLoad != null ? 1 : _articlesFiltered?.length ?? 0,
                  itemBuilder: (BuildContext context, int index) {
                    if (_errorOnLoad != null) {
                      return Center(
                        child: Text(
                          "Internal error: ${_errorOnLoad.toString()}",
                          style: TextStyle(color: Colors.red, fontSize: 20.0),
                        ),
                      );
                    }
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
                ))),
              ),
            ],
          ),
        ));
  }
}
