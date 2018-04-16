import 'package:awesome_dev/ui/widgets/article_widget.dart';
import 'package:flutter/material.dart';
import 'package:awesome_dev/api/articles.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum IndicatorType { overscroll, refresh }

class LatestNews extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => new LatestNewsState();
}

class LatestNewsState extends State<LatestNews> {

  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey = new GlobalKey<RefreshIndicatorState>();

  final _recentArticles = <Article>[];

  @override
  void initState() {
    super.initState();
    _fetchArticles();
  }

  _fetchArticles() async {
    _refreshIndicatorKey.currentState.show();
    final articlesClient = new ArticlesClient();
    try {
      final recentArticles = await articlesClient.getRecentArticles();
      final prefs = await SharedPreferences.getInstance();
      final favorites = prefs.getStringList("favs") ?? [];
      for (var article in recentArticles) {
        final String favoriteData = "{"
            "\"title\" : \"${article.title}\","
            "\"url\" : \"${article.url}\""
            "}";
        article.starred = favorites.contains(favoriteData);
      }

      setState(() {
        _recentArticles.clear();
        _recentArticles.addAll(recentArticles);
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
                itemCount: _recentArticles.length,
                itemBuilder: (BuildContext context, int index) {
                  return new ArticleWidget(
                    article: _recentArticles[index],
//                    onCardClick: () {
//  //                      Navigator.of(context).push(
//  //                          new FadeRoute(
//  //                            builder: (BuildContext context) => new BookNotesPage(_items[index]),
//  //                            settings: new RouteSettings(name: '/notes', isInitialRoute: false),
//  //                          ));
//                    },
                    onStarClick: () {
                      setState(() {
                        _recentArticles[index].starred =
                            !_recentArticles[index].starred;
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
