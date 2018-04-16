import 'dart:convert' show json;
import 'package:awesome_dev/ui/widgets/article_widget.dart';
import 'package:flutter/material.dart';
import 'package:awesome_dev/api/articles.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum IndicatorType { overscroll, refresh }

class FavoriteNews extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => new FavoriteNewsState();
}

class FavoriteNewsState extends State<FavoriteNews> {

  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey = new GlobalKey<RefreshIndicatorState>();

  final _articles = <Article>[];

  @override
  void initState() {
    super.initState();
    _fetchArticles();
  }

  _fetchArticles() async {
    _refreshIndicatorKey.currentState.show();
    final articlesClient = new ArticlesClient();
    try {
      final prefs = await SharedPreferences.getInstance();
      final favorites = prefs.getStringList("favs") ?? [];
      final articlesToLookup = [];
      for (var fav in favorites) {
        final Map<String, dynamic> map = json.decode(fav);
        final article = new Article(map['title'], map['url']);
        article.starred = true;
        articlesToLookup.add(article);
      }
      final favoriteArticles = await articlesClient.getFavoriteArticles(articlesToLookup);
      setState(() {
        _articles.clear();
        _articles.addAll(favoriteArticles);
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
      onRefresh: _fetchArticles,
      child: new Container(
        padding: new EdgeInsets.all(8.0),
        child: new Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            new Expanded(
              child: new ListView.builder(
                padding: new EdgeInsets.all(8.0),
                itemCount: _articles.length,
                itemBuilder: (BuildContext context, int index) {
                  return new ArticleWidget(
                    article: _articles[index],
//                    onCardClick: () {
//  //                      Navigator.of(context).push(
//  //                          new FadeRoute(
//  //                            builder: (BuildContext context) => new BookNotesPage(_items[index]),
//  //                            settings: new RouteSettings(name: '/notes', isInitialRoute: false),
//  //                          ));
//                    },
                    onStarClick: () {
                      setState(() {
                        _articles[index].starred =
                            !_articles[index].starred;
                      });
  //                      Repository.get().updateBook(_items[index]);
                    },
                  );
                },
              ),
            ),
          ],
        ),
      )
    );
  }
}
