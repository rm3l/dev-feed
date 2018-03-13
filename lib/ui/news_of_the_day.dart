import 'package:flutter/material.dart';
import 'package:english_words/english_words.dart';
import 'package:html/parser.dart' show parse;
import 'package:html/dom.dart' as html_dom;
import 'dart:io';
import 'package:feedparser/feedparser.dart' as feed_parser;

class NewsOfTheDay extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => new NewsOfTheDayState();

}

class NewsOfTheDayState extends State<NewsOfTheDay> {

  final _suggestions = <WordPair>[];

  final _saved = new Set<WordPair>();

  final _biggerFont = const TextStyle(fontSize: 18.0);

  _fetchFeed(BuildContext context) async {
    var url = 'http://www.discoverdev.io/rss.xml';
    var httpClient = new HttpClient();
    String result;
    try {
      var request = await httpClient.getUrl(Uri.parse(url));
      var response = await request.close();
      if (response.statusCode == HttpStatus.OK) {
//        var json = await response.transform(UTF8.decoder).join();
//        var data = JSON.decode(json);
        result = response.toString();
        feed_parser.Feed feed = feed_parser.parse(result);
        Scaffold.of(context).showSnackBar(new SnackBar(
          content: new Text(feed.toString()),
        ));
      } else {
        result =
        'Error fetching RSS feed:\nHttp status ${response.statusCode}';
        Scaffold.of(context).showSnackBar(new SnackBar(
          content: new Text(result),
        ));
      }
    } catch (exception) {
      result = 'Failed fetching RSS feed: ${exception.toString()}';
      Scaffold.of(context).showSnackBar(new SnackBar(
        content: new Text(result),
      ));
    }


  }

  @override
  Widget build(BuildContext context) {
    _fetchFeed(context);
    return new ListView.builder(
        padding: const EdgeInsets.all(16.0),
    itemBuilder: (context, i) {
    if (i.isOdd) return new Divider();

    final index = i ~/ 2;
    if (index >= _suggestions.length) {
    _suggestions.addAll(generateWordPairs().take(10));
    }
    return _buildRow(_suggestions[index]);
    },
    );
  }

  Widget _buildRow(WordPair pair) {
    var document = parse(
        '<body>Hello world! <a href="www.html5rocks.com">${pair.asPascalCase}!');
//    print(document.outerHtml);
    final alreadySaved = _saved.contains(pair);
    return new ListTile(
      title: new Text(
        document.outerHtml,
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