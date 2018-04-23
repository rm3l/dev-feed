import 'package:awesome_dev/api/articles.dart';
import 'package:flutter/material.dart';
import 'package:flutter_custom_tabs/flutter_custom_tabs.dart';
import 'package:meta/meta.dart';
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
}

class ArticleWidgetState extends State<ArticleWidget> {
  void _launchURL() async {
    try {
      await launch(
        widget.article.url,
        option: new CustomTabsOption(
          toolbarColor: Theme.of(context).primaryColor,
          enableDefaultShare: true,
          enableUrlBarHiding: true,
          showPageTitle: true,
        ),
      );
    } catch (e) {
      // An exception is thrown if browser app is not installed on Android device.
      debugPrint(e.toString());
      Scaffold.of(context).showSnackBar(new SnackBar(
          content: new Text(
              'Could not launch URL ($this.article.url): ${e.toString()}')));
    }
  }

  void _handleStarClick() async {
    final prefs = await SharedPreferences.getInstance();
    final newFavoritesList = <String>[];
    final favorites = prefs.getStringList("favs") ?? <String>[];
    newFavoritesList.addAll(favorites);
    final String favoriteData = widget.article.toSharedPreferencesString();
    if (!widget.article.starred) {
      //previous state
      newFavoritesList.add(favoriteData);
    } else {
      newFavoritesList.remove(favoriteData);
    }
    prefs.setStringList("favs", newFavoritesList);
    if (widget.onStarClick != null) {
      widget.onStarClick();
    }
  }

  @override
  Widget build(BuildContext context) {
    final tagsWidgets = <Widget>[];
    if (widget.article.tags != null) {
      for (var tag in widget.article.tags) {
        tagsWidgets.add(new Expanded(child: new Text(tag)));
      }
    }

    return new GestureDetector(
        onTap: widget.onCardClick != null ? widget.onCardClick : _launchURL,
        child: new Container(
            padding: const EdgeInsets.only(top: 8.0),
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
                            new Padding(padding: const EdgeInsets.all(3.0)),
                            new Align(
                              alignment: Alignment.topLeft,
                              child: new Text(widget.article.domain,
                                  style: const TextStyle(color: Colors.black38),
                                  textAlign: TextAlign.left),
                            )
                          ],
                        ),
                      ),
                    ),
                    new Stack(
                      children: <Widget>[
                        widget.article.screenshot != null &&
                                widget.article.screenshot.dataBytes != null
                            ? new Hero(
                                child: new Image.memory(
                                  widget.article.screenshot.dataBytes,
                                  width: 110.0,
                                ),
                                tag: widget.article.id,
                              )
                            : new Container(),
                        new IconButton(
                            icon: widget.article.starred
                                ? new Icon(Icons.favorite)
                                : new Icon(Icons.favorite_border),
                            color: widget.article.starred ? Colors.red : null,
                            onPressed: _handleStarClick,
                            alignment: Alignment.topRight,
                            padding: const EdgeInsets.only(left: 85.0))
                      ],
                    )
                  ],
                ),

                new Padding(padding: const EdgeInsets.all(3.0)),

                //Tags
                new Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: tagsWidgets),

                new Padding(padding: const EdgeInsets.all(3.0)),

                new Divider(height: 10.0, color: Theme.of(context).primaryColor)
              ],
            )));
  }
}
