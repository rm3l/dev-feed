import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:meta/meta.dart';
import 'package:awesome_dev/api/articles.dart';

class ArticleCard extends StatefulWidget {
  ArticleCard({
    this.article,
    @required this.onCardClick,
    @required this.onStarClick,
  });

  final Article article;

  final VoidCallback onCardClick;
  final VoidCallback onStarClick;

  @override
  State<StatefulWidget> createState() => new ArticleCardState();
}

class ArticleCardState extends State<ArticleCard> {

  final _biggerFont = const TextStyle(fontSize: 18.0);

  @override
  Widget build(BuildContext context) {
    return new GestureDetector(
      onTap: widget.onCardClick,
      child: new Card(
          child: new Container(
        height: 150.0,
        child: new Padding(
            padding: new EdgeInsets.all(8.0),
            child: new Row(
              children: <Widget>[
                widget.article.screenshot != null &&
                        widget.article.screenshot.data != null
                    ? new Hero(
                        child: new Image.memory(
                          base64.decode(widget.article.screenshot.data),
                          width: 150.0,
                        ),
                        tag: widget.article.id,
                      )
                    : new Container(),
                new Expanded(
                  child: new Stack(
                    children: <Widget>[
                      new Align(
                        child: new Padding(
                          child: new Text(widget.article.title, maxLines: 3,
                              style: _biggerFont),
                          padding: new EdgeInsets.all(8.0),
                        ),
                        alignment: Alignment.center,
                      ),
                      new Align(
                        child: new IconButton(
                          icon: widget.article.starred
                              ? new Icon(Icons.favorite)
                              : new Icon(Icons.favorite_border),
                          color: widget.article.starred ? Colors.red : null,
                          onPressed: widget.onStarClick,
                        ),
                        alignment: Alignment.topRight,
                      ),
                    ],
                  ),
                ),
              ],
            )),
      )),
    );
  }
}
