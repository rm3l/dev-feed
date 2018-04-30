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
  State<StatefulWidget> createState() => new FavoriteNewsState();
}

class FavoriteNewsState extends State<FavoriteNews> {
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

  Future<List<Article>> _loadArticles() async {
    _refreshIndicatorKey.currentState.show();
    final articlesClient = new ArticlesClient();
    final prefs = await SharedPreferences.getInstance();
    final favorites = prefs.getStringList("favs") ?? <String>[];
    final articlesToLookup = <Article>[];
    for (var fav in favorites) {
      final Map<String, dynamic> map = json.decode(fav);
      articlesToLookup
          .add(new Article(map['title'].toString(), map['url'].toString()));
    }
    final favoriteArticles =
        await articlesClient.getFavoriteArticles(articlesToLookup);
    for (var article in favoriteArticles) {
      article.starred = true;
    }
    return favoriteArticles;
  }

  Future<Null> _fetchArticles() async {
    try {
      final favoriteArticles = await _loadArticles();
      final favoriteArticlesFiltered =
          ArticlesClient.searchInArticles(favoriteArticles, _search);
      setState(() {
        _articles = favoriteArticles;
        _searchInputVisible = _articles.isNotEmpty;
        _articlesFiltered = favoriteArticlesFiltered;
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
                            final recentArticlesFiltered = ArticlesClient
                                .searchInArticles(_articles, criteria);
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
