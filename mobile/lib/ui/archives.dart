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
import 'package:flutter_calendar/flutter_calendar.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum IndicatorType { overscroll, refresh }

class ArticleArchives extends StatefulWidget {
  const ArticleArchives();

  @override
  State<StatefulWidget> createState() => ArticleArchivesState();
}

class ArticleArchivesState extends State<ArticleArchives> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      GlobalKey<RefreshIndicatorState>();

  bool _initialDisplay = true;
  List<Article> _articles;
  DateTime _currentDate = DateTime.now();
  Exception _errorOnLoad;

  Future<Null> _onRefresh() async => _fetchArticles(_currentDate);

  Future<Null> _fetchArticles(DateTime date) async {
    final articlesClient = ArticlesClient();
    try {
      final dateSlug = "${date.year.toString()}-"
          "${date.month.toString().padLeft(2, '0')}-"
          "${date.day.toString().padLeft(2, '0')}";
      final filteredArticles =
          await articlesClient.getArticlesForDate(dateSlug);
      final prefs = await SharedPreferences.getInstance();
      final favorites = prefs.getStringList("favs") ?? [];
      for (var article in filteredArticles) {
        article.starred =
            favorites.contains(article.toSharedPreferencesString());
      }
      setState(() {
        _initialDisplay = false;
        _currentDate = date;
        _articles = filteredArticles;
        _errorOnLoad = null;
      });
    } on Exception catch (e) {
      setState(() {
        _initialDisplay = false;
        _currentDate = date;
        _articles = null;
        _errorOnLoad = e;
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
          Center(child: Text("Please hold on - loading articles..."))
        ],
      );
      _onRefresh();
      return widget;
    }

    return RefreshIndicator(
      key: _refreshIndicatorKey,
      onRefresh: _onRefresh,
      child: Container(
        child: Column(
          children: <Widget>[
            Align(
              alignment: Alignment.topCenter,
              child: Calendar(
                isExpandable: true,
                onDateSelected: (selectedDateTime) {
                  _currentDate = selectedDateTime;
                  _refreshIndicatorKey.currentState.show();
                },
              ),
            ),
            Container(
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
                      style: TextStyle(color: Colors.red, fontSize: 20.0),
                    ),
                  );
                }
                if (_articles == null || _articles.isEmpty) {
                  return Center(
                    child: Text(
                      "No article found at this time",
                      style: TextStyle(fontSize: 20.0),
                    ),
                  );
                }
                return ArticleWidget(
                  article: _articles[index],
                  onStarClick: () {
                    setState(() {
                      _articles[index].starred = !_articles[index].starred;
                    });
                  },
                );
              },
            )))),
          ],
        ),
      ),
    );
  }
}
