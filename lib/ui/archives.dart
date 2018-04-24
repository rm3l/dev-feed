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

  final _articles = <Article>[];

  List<Article> _allArticles;

  DateTime _selectedDate = DateTime.now();

  @override
  void initState() {
    super.initState();
    _fetchArticles();
  }

  Future<Null> _fetchArticles() async {
    _refreshIndicatorKey.currentState.show();
    final articlesClient = new ArticlesClient();
    try {
      if (_allArticles == null) {
        _allArticles = await articlesClient.getAllArticles();
      }
      final prefs = await SharedPreferences.getInstance();
      final favorites = prefs.getStringList("favs") ?? [];
      final filteredArticles = _allArticles
          .where((article) =>
              //TODO Find a better way to represent dates in GraphQL responses, e.g., using timestamps
              article.date ==
              ("${_weekdayToHuman(_selectedDate.weekday)} "
                  "${_monthToHuman(_selectedDate.month)} "
                  "${_selectedDate.day} "
                  "00:00:00 UTC "
                  "${_selectedDate.year}"))
          .toList();
      for (var article in filteredArticles) {
        article.starred =
            favorites.contains(article.toSharedPreferencesString());
      }
      setState(() {
        _articles.clear();
        _articles.addAll(filteredArticles);
      });
    } on Exception catch (e) {
      Scaffold.of(context).showSnackBar(new SnackBar(
            content: new Text("Internal Error: ${e.toString()}"),
          ));
    }
  }

  String _weekdayToHuman(int weekday) {
    switch (weekday) {
      case 1:
        return "Mon";
      case 2:
        return "Tue";
      case 3:
        return "Wed";
      case 4:
        return "Thu";
      case 5:
        return "Fri";
      case 6:
        return "Sat";
      case 7:
        return "Sun";
      default:
        throw new StateError("unkwnon weekday: $weekday");
    }
  }

  String _monthToHuman(int month) {
    switch (month) {
      case 1:
        return "Jan";
      case 2:
        return "Feb";
      case 3:
        return "Mar";
      case 4:
        return "Apr";
      case 5:
        return "May";
      case 6:
        return "Jun";
      case 7:
        return "Jul";
      case 8:
        return "Aug";
      case 9:
        return "Sep";
      case 10:
        return "Oct";
      case 11:
        return "Nov";
      case 12:
        return "Dec";
      default:
        throw new StateError("unkwnon month: $month");
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
              child: new Calendar(
                isExpandable: true,
                onDateSelected: (selectedDateTime) {
                  setState(() {
                    _selectedDate = selectedDateTime;
                  });
                  _fetchArticles();
                },
              ),
            ),
            new Container(
                child: new Expanded(
                    child: new ListView.builder(
              padding: new EdgeInsets.all(8.0),
              itemCount: _articles.length,
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
