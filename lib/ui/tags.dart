import 'dart:async';

import 'package:awesome_dev/api/tags.dart' as tagsApi;
import 'package:awesome_dev/config/application.dart';
import 'package:fluro/fluro.dart';
import 'package:flutter/material.dart';

enum IndicatorType { overscroll, refresh }

class Tags extends StatefulWidget {
  const Tags();

  @override
  State<StatefulWidget> createState() => TagsState();
}

class TagsState extends State<Tags> {
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      GlobalKey<RefreshIndicatorState>();

  bool _initialDisplay = true;
  List<String> _tags;
  List<String> _tagsFiltered;

  final _searchInputController = TextEditingController();

  String _search;
  bool _searchInputVisible = false;

  List<String> _searchInTags(List<String> allTags, String search) {
    if (allTags == null) {
      return List<String>(0);
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
      final tagsClient = tagsApi.TagsClient();
      final allTags = await tagsClient.getTags();
      final tagsFiltered = _searchInTags(allTags, _search);
      setState(() {
        _initialDisplay = false;
        _tags = allTags;
        _searchInputVisible = _tags.isNotEmpty;
        _tagsFiltered = tagsFiltered;
      });
    } on Exception catch (e) {
      Scaffold.of(context).showSnackBar(SnackBar(
            content: Text("Internal Error: ${e.toString()}"),
          ));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_initialDisplay) {
      final widget = Center(child: CircularProgressIndicator());
      _fetchTags();
      return widget;
    }
    return RefreshIndicator(
        key: _refreshIndicatorKey,
        onRefresh: _fetchTags,
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
                            final tagsFiltered = _searchInTags(_tags, criteria);
                            setState(() {
                              _search = criteria;
                              _tagsFiltered = tagsFiltered;
                            });
                          },
                        ),
                        FlatButton(
                            onPressed: () {
                              _searchInputController.clear();
                              setState(() {
                                _search = null;
                                _tagsFiltered = _tags;
                              });
                            },
                            child: Icon(Icons.clear))
                      ],
                    ),
                  )),
              Container(
                child: Expanded(
                    child: ListView.builder(
                  padding: EdgeInsets.all(8.0),
                  itemCount: _tagsFiltered?.length ?? 0,
                  itemBuilder: (BuildContext context, int index) {
                    return GestureDetector(
                        onTap: () => Application.router.navigateTo(
                            context, "/tags/${_tagsFiltered[index]}",
                            transition: TransitionType.fadeIn),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Padding(
                                padding: EdgeInsets.all(10.0),
                                child: Chip(label: Text(_tagsFiltered[index]))),
                            Divider(
                                height: 10.0,
                                color: Theme.of(context).primaryColor),
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
