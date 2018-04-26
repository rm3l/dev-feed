import 'dart:async';

import 'package:awesome_dev/api/articles.dart';
import 'package:awesome_dev/ui/widgets/article_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_calendar/flutter_calendar.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum IndicatorType { overscroll, refresh }

class ArticleArchives extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => new ArticleArchivesState();
}

class ArticleArchivesState extends State<ArticleArchives> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      new GlobalKey<RefreshIndicatorState>();

  List<Article> _articles;
  DateTime _currentDate = new DateTime.now();

  @override
  void initState() {
    super.initState();
    _onRefresh();
  }

  Future<Null> _onRefresh() async => _fetchArticles(_currentDate);

  Future<Null> _fetchArticles(DateTime date) async {
    _refreshIndicatorKey.currentState.show();
    final articlesClient = new ArticlesClient();
    try {
      final dateSlug = "${date.year.toString()}-"
          "${date.month.toString().padLeft(2,'0')}-"
          "${date.day.toString().padLeft(2,'0')}";
      final filteredArticles =
          await articlesClient.getArticlesForDate(dateSlug);
      final prefs = await SharedPreferences.getInstance();
      final favorites = prefs.getStringList("favs") ?? [];
      for (var article in filteredArticles) {
        article.starred =
            favorites.contains(article.toSharedPreferencesString());
      }
      setState(() {
        _articles = filteredArticles;
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
      onRefresh: _onRefresh,
      child: new Container(
        child: new Column(
          children: <Widget>[
            new Align(
              alignment: Alignment.topCenter,
              child: new Calendar(
                isExpandable: true,
                onDateSelected: (selectedDateTime) {
                  _currentDate = selectedDateTime;
                  _fetchArticles(selectedDateTime);
                },
              ),
            ),
            new Container(
                child: new Expanded(
                    child: new ListView.builder(
              padding: new EdgeInsets.all(8.0),
              itemCount: _articles?.length ?? 0,
              itemBuilder: (BuildContext context, int index) {
                return new ArticleWidget(
                  article: _articles[index],
//                    onCardClick: () {
//  //                      Navigator.of(context).push(
//  //                          new FadeRoute(
//  //                            builder: (BuildContext context) => new BookNotesPage(_items[index]),
//  //                            settings: new RouteSettings(name: '/notes', isInitialRoute: false),
//  //                          ));
//                    },
                  onStarClick: () {
                    setState(() {
                      _articles[index].starred = !_articles[index].starred;
                    });
                    //                      Repository.get().updateBook(_items[index]);
                  },
                );
              },
            ))),
          ],
        ),
      ),
    );
  }
}
