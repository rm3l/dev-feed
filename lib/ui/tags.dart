import 'dart:async';

import 'package:awesome_dev/api/tags.dart' as tagsApi;
import 'package:awesome_dev/config/application.dart';
import 'package:fluro/fluro.dart';
import 'package:flutter/material.dart';

enum IndicatorType { overscroll, refresh }

class Tags extends StatefulWidget {
  const Tags();

  @override
  State<StatefulWidget> createState() => new TagsState();
}

class TagsState extends State<Tags> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      new GlobalKey<RefreshIndicatorState>();

  List<String> _tags;
  List<String> _tagsFiltered;

  final _searchInputController = new TextEditingController();

  String _search;
  bool _searchInputVisible = false;

  @override
  void initState() {
    super.initState();
    _fetchTags();
  }

  Future<List<String>> _loadTags() async {
    _refreshIndicatorKey.currentState.show();
    final tagsClient = new tagsApi.TagsClient();
    final tags = await tagsClient.getTags();
    return tags;
  }

  List<String> _searchInTags(List<String> allTags, String search) {
    if (allTags == null) {
      return new List<String>(0);
    }
    var tagsFiltered = allTags;
    if (search != null && search.isNotEmpty) {
      final searchLowerCase = search.toLowerCase();
      tagsFiltered = allTags
        .where((tag) => tag.toLowerCase().contains(searchLowerCase))
        .toList();
    }
    return tagsFiltered;
  }

  Future<Null> _fetchTags() async {
    try {
      final allTags = await _loadTags();
      final tagsFiltered = _searchInTags(allTags, _search);
      setState(() {
        _tags = allTags;
        _searchInputVisible = _tags.isNotEmpty;
        _tagsFiltered = tagsFiltered;
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
        onRefresh: _fetchTags,
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
                            final tagsFiltered = _searchInTags(_tags, criteria);
                            setState(() {
                              _search = criteria;
                              _tagsFiltered = tagsFiltered;
                            });
                          },
                        ),
                        new FlatButton(
                            onPressed: () {
                              _searchInputController.clear();
                              setState(() {
                                _search = null;
                                _tagsFiltered = _tags;
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
                  itemCount: _tagsFiltered.length,
                  itemBuilder: (BuildContext context, int index) {
                    return new GestureDetector(
                        onTap: () => Application.router.navigateTo(
                            context, "/tags/${_tagsFiltered[index]}",
                            transition: TransitionType.fadeIn),
                        child: new Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            new Padding(
                                padding: new EdgeInsets.all(10.0),
                                child: new Text(_tagsFiltered[index])),
                            new Divider(
                                height: 10.0, color: Theme.of(context).primaryColor),
                          ],
                        ));
                  },
                )),
              ),
            ],
          ),
        ));
  }
}
