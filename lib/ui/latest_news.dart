import 'package:flutter/material.dart';
import 'dart:io';
import 'package:awesome_dev/api/articles.dart';
import 'package:awesome_dev/api/graphql.dart';

import 'package:http/http.dart';
import 'package:logging/logging.dart'; // Optional
import 'package:graphql_client/graphql_client.dart';
import 'package:graphql_client/graphql_dsl.dart';

class LatestNews extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => new LatestNewsState();

}

class LatestNewsState extends State<LatestNews> {

  final _suggestions = <String>[];

  final _saved = new Set<String>();

  final _biggerFont = const TextStyle(fontSize: 18.0);

  _fetchArticles(BuildContext context) async {
    final query = new ArticlesQuery("GetRecentArticles", "recentArticles");
    try {
      final queryRes = await graphQLClient.execute(
        query,
        variables: {},
        headers: {
          'User-Agent': 'org.rm3l.discoverdev.io',
        },
      );

      Scaffold.of(context).showSnackBar(new SnackBar(
        content: new Text(queryRes.viewer.title.toString()),
      ));

    } on GQLException catch (e) {
      print(e.message);
      print(e.gqlErrors);
    }
  }

  @override
  Widget build(BuildContext context) {
    _fetchArticles(context);
    return new ListView.builder(
        padding: const EdgeInsets.all(16.0),
    itemBuilder: (context, i) {
    if (i.isOdd) return new Divider();

    final index = i ~/ 2;
//    if (index >= _suggestions.length) {
//    _suggestions.addAll(generateWordPairs().take(10));
//    }
    return _buildRow(_suggestions[index]);
    },
    );
  }

  Widget _buildRow(String pair) {
    final alreadySaved = _saved.contains(pair);
    return new ListTile(
      title: new Text(
        "Hiya",
        style: _biggerFont,
      ),
      trailing: new Icon(
        alreadySaved ? Icons.favorite : Icons.favorite_border,
        color: alreadySaved ? Colors.red : null,
      ),
      onTap: () {
        setState(
              () {
            if (alreadySaved) {
              _saved.remove(pair);
            } else {
              _saved.add(pair);
            }
          },
        );
      },
    );
  }

}
