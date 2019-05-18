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
  Exception _errorOnLoad;

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
        _errorOnLoad = null;
      });
    } on Exception catch (e) {
      setState(() {
        _initialDisplay = false;
        _tags = null;
        _searchInputVisible = false;
        _tagsFiltered = null;
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
          Center(child: Text("Please hold on - loading articles tags..."))
        ],
      );
      _fetchTags();
      return widget;
    }
    if (_errorOnLoad == null &&
        _search == null &&
        (_tagsFiltered == null || _tagsFiltered.isEmpty)) {
      //No error, but no article fetched
      return RefreshIndicator(
          key: _refreshIndicatorKey,
          onRefresh: _fetchTags,
          child: ListView(
            children: <Widget>[
              Container(
                  height: MediaQuery.of(context).size.height -
                      kToolbarHeight -
                      kBottomNavigationBarHeight,
                  child: Center(
                    child: const Text(
                      "No tag found at this time",
                      style: TextStyle(fontSize: 20.0),
                    ),
                  )),
            ],
          ));
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
                    child: Scrollbar(
                        child: ListView.builder(
                  padding: EdgeInsets.all(8.0),
                  itemCount:
                      _errorOnLoad != null ? 1 : _tagsFiltered?.length ?? 0,
                  itemBuilder: (BuildContext context, int index) {
                    if (_errorOnLoad != null) {
                      return Center(
                        child: Text(
                          "Internal error: ${_errorOnLoad.toString()}",
                          style: TextStyle(color: Colors.red, fontSize: 20.0),
                        ),
                      );
                    }

                    final tag = _tagsFiltered[index];
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Padding(
                            padding: EdgeInsets.all(10.0),
                            child: ActionChip(
                              avatar: CircleAvatar(
                                backgroundColor: Colors.blueGrey,
                                child: Text(tag.substring(1, 2)),
                              ),
                              label: Text(tag,
                                  textAlign: TextAlign.left,
                                  style: TextStyle(fontSize: 16.0)),
                              backgroundColor: Theme.of(context).buttonColor,
                              onPressed: () => Application.router.navigateTo(
                                  context, "/tags/$tag",
                                  transition: TransitionType.fadeIn),
                            )),
                        Divider(
                            height: 10.0,
                            color: Theme.of(context).primaryColor),
                      ],
                    );
                  },
                ))),
              ),
            ],
          ),
        ));
  }
}
