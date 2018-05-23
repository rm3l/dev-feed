import 'package:intl/intl.dart';
import 'package:awesome_dev/api/articles.dart';
import 'package:awesome_dev/config/application.dart';
import 'package:fluro/fluro.dart';
import 'package:flutter/material.dart';
import 'package:flutter_custom_tabs/flutter_custom_tabs.dart';
import 'package:meta/meta.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:transparent_image/transparent_image.dart';

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
  State<StatefulWidget> createState() => ArticleWidgetState();
}

class ArticleWidgetState extends State<ArticleWidget> {
  void _launchURL() async {
    try {
      await launch(
        widget.article.url,
        option: CustomTabsOption(
          toolbarColor: Theme.of(context).primaryColor,
          enableDefaultShare: true,
          enableUrlBarHiding: true,
          showPageTitle: true,
        ),
      );
    } catch (e) {
      // An exception is thrown if browser app is not installed on Android device.
      debugPrint(e.toString());
      Scaffold.of(context).showSnackBar(SnackBar(
          content: Text(
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
        tagsWidgets.add(Container(
            margin: const EdgeInsets.only(right: 3.0),
            child: ActionChip(
              avatar: CircleAvatar(
                backgroundColor: Colors.blueGrey,
                child: Text(tag.substring(1, 2)),
              ),
              label: Text(
                tag,
                textAlign: TextAlign.left,
              ),
              backgroundColor: Theme.of(context).buttonColor,
              onPressed: () => Application.router.navigateTo(
                  context, "/tags/$tag",
                  transition: TransitionType.fadeIn),
            )));
      }
    }

    final placeHolderImage = MemoryImage(kTransparentImage);
    Widget articleOverviewImageWidget;
    final articleOverviewImageWidgetWidth = 110.0;
    final articleOverviewImageWidgetHeight = 60.0;
    if (widget.article.parsed != null &&
        widget.article.parsed.image != null &&
        widget.article.parsed.image.isNotEmpty) {
      articleOverviewImageWidget = FadeInImage(
          placeholder: placeHolderImage,
          image: NetworkImage(widget.article.parsed.image),
          width: articleOverviewImageWidgetWidth,
          height: articleOverviewImageWidgetHeight);
    } else if (widget.article.screenshot != null &&
        widget.article.screenshot.dataBytes != null &&
        widget.article.screenshot.dataBytes.isNotEmpty) {
      articleOverviewImageWidget = FadeInImage(
          placeholder: placeHolderImage,
          image: MemoryImage(widget.article.screenshot.dataBytes),
          width: articleOverviewImageWidgetWidth,
          height: articleOverviewImageWidgetHeight);
    } else {
      articleOverviewImageWidget = null;
    }

    return GestureDetector(
        onTap: widget.onCardClick != null ? widget.onCardClick : _launchURL,
        child: Container(
            padding: const EdgeInsets.only(top: 8.0),
            child: Column(
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Expanded(
                      child: Align(
                        alignment: Alignment.topLeft,
                        child: Column(
                          children: <Widget>[
                            Align(
                                alignment: Alignment.topLeft,
                                child: Text(widget.article.title,
                                    textAlign: TextAlign.left,
                                    style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                        fontSize: 15.0))),
                            Padding(padding: const EdgeInsets.all(3.0)),
                            Align(
                              alignment: Alignment.topLeft,
                              child: Text(widget.article.domain,
                                  style: const TextStyle(color: Colors.black54),
                                  textAlign: TextAlign.left),
                            ),
                            Padding(padding: const EdgeInsets.all(3.0)),
                            Align(
                              alignment: Alignment.topLeft,
                              child: Text(
                                  new DateFormat.yMMMd().format(
                                      new DateTime.fromMillisecondsSinceEpoch(
                                          widget.article.timestamp,
                                          isUtc: true)),
                                  style: const TextStyle(color: Colors.black38),
                                  textAlign: TextAlign.left),
                            )
                          ],
                        ),
                      ),
                    ),
                    Stack(
                      children: <Widget>[
                        articleOverviewImageWidget != null
                            ? Hero(
//                                child: Stack(
//                                  children: <Widget>[
//                                    CircularProgressIndicator(),
//                                    articleOverviewImageWidget
//                                  ],
//                                ),
                                child: articleOverviewImageWidget,
                                tag: widget.article.id,
                              )
                            : Container(),
                        IconButton(
                            icon: widget.article.starred
                                ? Icon(Icons.favorite)
                                : Icon(Icons.favorite_border),
                            color: widget.article.starred ? Colors.red : null,
                            onPressed: _handleStarClick,
                            alignment: Alignment.topRight,
                            padding: const EdgeInsets.only(left: 85.0))
                      ],
                    )
                  ],
                ),

                Padding(padding: const EdgeInsets.all(3.0)),

                //Tags
                new Container(
                  height: 33.0,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    physics: const BouncingScrollPhysics(),
                    children: tagsWidgets,
                  ),
                ),

                Padding(padding: const EdgeInsets.all(3.0)),

                Divider(height: 10.0, color: Theme.of(context).primaryColor)
              ],
            )));
  }
}
