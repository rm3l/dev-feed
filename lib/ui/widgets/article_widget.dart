import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:meta/meta.dart';
import 'package:awesome_dev/api/articles.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ArticleWidget extends StatefulWidget {
  ArticleWidget({
    this.article,
    this.onCardClick,
    @required this.onStarClick,
  });

  final Article article;
  final VoidCallback onCardClick;
  final VoidCallback onStarClick;

  @override
  State<StatefulWidget> createState() => new ArticleWidgetState();

  _launchURL() async {
    if (await canLaunch(this.article.url)) {
      await launch(this.article.url);
    } else {
      throw 'Could not launch $this.article.url';
    }
  }

  _handleStarClick() async {
    final prefs = await SharedPreferences.getInstance();
    final newFavoritesList = [];
    final favorites = prefs.getStringList("favs") ?? [];
    newFavoritesList.addAll(favorites);
    final String favoriteData = article.toSharedPreferencesString();
    if (!article.starred) {
      //previous state
      newFavoritesList.add(favoriteData);
    } else {
      newFavoritesList.remove(favoriteData);
    }
    prefs.setStringList("favs", newFavoritesList);
    if (onStarClick != null) {
      onStarClick();
    }
  }
}

class ArticleWidgetState extends State<ArticleWidget> {
  @override
  Widget build(BuildContext context) {
    final tagsWidgets = <Widget>[];
    if (widget.article.tags != null) {
      for (var tag in widget.article.tags) {
        tagsWidgets.add(new Text(tag));
      }
    }

    return new GestureDetector(
        onTap:
            widget.onCardClick != null ? widget.onCardClick : widget._launchURL,
        child: new Container(
            padding: const EdgeInsets.all(8.0),
            child: new Column(
          children: <Widget>[
            new Row(
              children: <Widget>[
                new Expanded(
                  child: new Align(
                    alignment: Alignment.topLeft,
                    child: new Column(
                      children: <Widget>[
                        new Align(
                            alignment: Alignment.topLeft,
                            child: new Text(widget.article.title,
                                textAlign: TextAlign.left,
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 15.0))),
                        new Align(
                          alignment: Alignment.topLeft,
                          child: new Text(widget.article.domain,
                              textAlign: TextAlign.left),
                        )
                      ],
                    ),
                  ),
                ),
                widget.article.screenshot != null &&
                        widget.article.screenshot.data != null
                    ? new Hero(
                        child: new Image.memory(
                          base64.decode(widget.article.screenshot.data),
                          width: 110.0,
                        ),
                        tag: widget.article.id,
                      )
                    : new Container(),
              ],
            ),

            //Tags
            new Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: tagsWidgets),

            new Divider(height: 5.0, color: Colors.green),
          ],
        )));

//    return new GestureDetector(
//        onTap: widget.onCardClick != null ? widget.onCardClick : widget._launchURL,
//        child: new Column(children: <Widget>[
//          new Container(
//              padding: const EdgeInsets.all(8.0),
//              child: new Column(children: <Widget>[
//                new Container(
//                    padding: const EdgeInsets.only(bottom: 5.0),
//                    child: new Row(
//                      children: <Widget>[
//                        new Text(widget.article.domain),
//                        new Expanded(
//                            child: new Row(
//                          mainAxisAlignment: MainAxisAlignment.end,
//                          children: <Widget>[
//                            new IconButton(
//                              icon: widget.article.starred
//                                  ? new Icon(Icons.favorite)
//                                  : new Icon(Icons.favorite_border),
//                              color: widget.article.starred ? Colors.red : null,
//                              onPressed: widget._handleStarClick,
//                              alignment: Alignment.topRight,
//                            )
//                          ],
//                        ))
//                      ],
//                    )),
//                new Row(
//                  children: <Widget>[
//                    widget.article.screenshot != null &&
//                            widget.article.screenshot.data != null
//                        ? new Hero(
//                            child: new Image.memory(
//                              base64.decode(widget.article.screenshot.data),
//                              width: 150.0,
//                            ),
//                            tag: widget.article.id,
//                          )
//                        : new Container(),
//                    new Expanded(
//                      child: new Stack(
//                        children: <Widget>[
//                          new Align(
//                            child: new Padding(
//                              child: new Text(widget.article.title,
//                                  style: _biggerFont),
//                              padding: const EdgeInsets.all(8.0),
//                            ),
//                            alignment: Alignment.center,
//                          ),
//                        ],
//                      ),
//                    ),
//                  ],
//                ),
//                new Padding(
//                    padding: const EdgeInsets.all(5.0),
//                    child: new Align(
//                      alignment: Alignment.bottomLeft,
//                      child: new Row(
//                          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
//                          children: tagsWidgets),
//                    ))
//              ])),
//          new Divider(height: 5.0, color: Colors.green),
//        ]));
  }
}
