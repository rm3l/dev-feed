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

import 'package:dev_feed/api/articles.dart';
import 'package:dev_feed/ui/widgets/article_widget.dart';
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
      final recentArticles = await articlesClient.getRecentArticles();
      final prefs = await SharedPreferences.getInstance();
      final favorites = prefs.getStringList("favs") ?? [];
      for (var article in recentArticles) {
        article.starred =
            favorites.contains(article.toSharedPreferencesString());
      }
      final recentArticlesFiltered =
          ArticlesClient.searchInArticles(recentArticles, _search);
      setState(() {
        _initialDisplay = false;
        _articles = recentArticles;
        _searchInputVisible = _articles.isNotEmpty;
        _articlesFiltered = recentArticlesFiltered;
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
      final widget = Stack(
        children: <Widget>[
          Center(
            child: CircularProgressIndicator(),
          ),
          Center(
              child:
                  Text("Please hold on - loading the most recent articles..."))
        ],
      );
      _fetchArticles();
      return widget;
    }

    if (_errorOnLoad == null &&
        _search == null &&
        (_articlesFiltered == null || _articlesFiltered.isEmpty)) {
      //No error, but no article fetched
      return RefreshIndicator(
          key: _refreshIndicatorKey,
          onRefresh: _fetchArticles,
          child: ListView(
            children: <Widget>[
              Container(
                  height: MediaQuery.of(context).size.height -
                      kToolbarHeight -
                      kBottomNavigationBarHeight,
                  child: Center(
                    child: const Text(
                      "No recent article found at this time",
                      style: TextStyle(fontSize: 20.0),
                    ),
                  )),
            ],
          ));
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
                            final recentArticlesFiltered =
                                ArticlesClient.searchInArticles(
                                    _articles, criteria);
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
                      onStarClick: () {
                        setState(() {
                          _articlesFiltered[index].starred =
                              !_articlesFiltered[index].starred;
                        });
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
