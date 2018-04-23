import 'dart:async';

import 'package:awesome_dev/api/tags.dart' as tagsApi;
import 'package:awesome_dev/config/application.dart';
import 'package:fluro/fluro.dart';
import 'package:flutter/material.dart';

enum IndicatorType { overscroll, refresh }

class Tags extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => new TagsState();
}

class TagsState extends State<Tags> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      new GlobalKey<RefreshIndicatorState>();

  final _tags = <String>[];
  DateTime _tagsLastUpdate;

  @override
  void initState() {
    super.initState();
    _fetchTags();
  }

  Future<Null> _fetchTags() async {
    _refreshIndicatorKey.currentState.show();
    if (_tags.isNotEmpty &&
        _tagsLastUpdate != null &&
        _tagsLastUpdate.day == new DateTime.now().day) {
      //Same day
    } else {
      final tagsClient = new tagsApi.TagsClient();
      try {
        final tags = await tagsClient.getTags();
        setState(() {
          _tags.clear();
          _tags.addAll(tags);
          _tags.sort();
        });
        _tagsLastUpdate = new DateTime.now();
      } on Exception catch (e) {
        Scaffold.of(context).showSnackBar(new SnackBar(
              content: new Text("Internal Error: ${e.toString()}"),
            ));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return new RefreshIndicator(
      key: _refreshIndicatorKey,
      onRefresh: _fetchTags,
      child: new ListView.builder(
          padding: new EdgeInsets.all(8.0),
//          itemExtent: 20.0,
          itemCount: _tags.length,
          itemBuilder: (BuildContext context, int index) {
            return new GestureDetector(
                onTap: () => Application.router.navigateTo(context, "/tags/${_tags[index]}",
                    transition: TransitionType.fadeIn),
                child: new Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    new Padding(
                        padding: new EdgeInsets.all(10.0),
                        child: new Text(_tags[index])),
                    new Divider(
                        height: 10.0, color: Theme.of(context).primaryColor),
                  ],
                ));
          }),
    );
  }
}
