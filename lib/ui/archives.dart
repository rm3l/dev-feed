import 'dart:async';

import 'package:flutter/material.dart';
import 'package:awesome_dev/ui/widgets/article_widget.dart';

import 'package:awesome_dev/api/articles.dart';

import 'package:small_calendar/small_calendar.dart';

class ArticleArchives extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => new ArticleArchivesState();
}

class ArticleArchivesState extends State<ArticleArchives> {

  bool showWeekdayIndication = true;
  bool showTicks = true;
  SmallCalendarController smallCalendarController = createSmallCalendarController();

  final _articles = <Article>[];

  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _fetchArticles(context);
  }

  _fetchArticles(BuildContext context) async {
    setState(() {
      _isLoading = true;
    });
    final articlesClient = new ArticlesClient();
    try {
      //TODO Add pagination, based on offset and size
      final recentArticles = await articlesClient.getAllButRecentArticles();
      setState(() {
        _articles.clear();
        _articles.addAll(recentArticles);
        _isLoading = false;
      });
    } on Exception catch (e) {
      Scaffold.of(context).showSnackBar(new SnackBar(
        content: new Text("Internal Error: ${e.toString()}"),
      ));
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return new SmallCalendar(
      firstWeekday: DateTime.monday,
      controller: smallCalendarController,
      dayStyle: new DayStyleData(
        showTicks: showTicks,
        tick3Color: Colors.yellow[700],
      ),
      showWeekdayIndication: showWeekdayIndication,
      weekdayIndicationStyle: new WeekdayIndicationStyleData(
          backgroundColor: Theme.of(context).primaryColor),
      onDayPressed: (DateTime date) {
        Scaffold.of(context).showSnackBar(new SnackBar(
          content: new Text(
            "Pressed on ${date.year}.${date.month}.${date.day}",
          ),
        ));
      },
      onDisplayedMonthChanged: (int year, int month){
        print("Displaying $year.$month");
      },
    );
  }

}

SmallCalendarController createSmallCalendarController() {
  Future<bool> hasTick1Callback(DateTime date) async {
    if (date.day == 1 || date.day == 4 || date.day == 5) {
      return true;
    }

    return false;
  }

  Future<bool> hasTick2Callback(DateTime date) async {
    if (date.day == 2 || date.day == 4 || date.day == 5) {
      return true;
    }

    return false;
  }

  Future<bool> hasTick3Callback(DateTime date) async {
    if (date.day == 3 || date.day == 5) {
      return true;
    }

    return false;
  }

  return new SmallCalendarController(
    isSelectedCallback: (DateTime date) async {
      if (date.day == 10) {
        return true;
      }

      return false;
    },
    hasTick1Callback: hasTick1Callback,
    hasTick2Callback: hasTick2Callback,
    hasTick3Callback: hasTick3Callback,
  );
}