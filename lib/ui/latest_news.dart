import 'package:awesome_dev/ui/widgets/article_card.dart';
import 'package:flutter/material.dart';
import 'package:awesome_dev/api/articles.dart';

class LatestNews extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => new LatestNewsState();
}

class LatestNewsState extends State<LatestNews> {
  final _recentArticles = <Article>[];

  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _fetchArticles(context);
  }

  _fetchArticles(BuildContext context) async {
    setState(() {
      _isLoading = true;
    });
    final articlesClient = new ArticlesClient();
    try {
      final recentArticles = await articlesClient.getRecentArticles();
      setState(() {
        _recentArticles.clear();
        _recentArticles.addAll(recentArticles);
        _isLoading = false;
      });
    } on Exception catch (e) {
      Scaffold.of(context).showSnackBar(new SnackBar(
            content: new Text("Internal Error: ${e.toString()}"),
          ));
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return new Container(
      padding: new EdgeInsets.all(8.0),
      child: new Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: <Widget>[
          _isLoading ? new CircularProgressIndicator() : new Container(),
          new Expanded(
            child: new ListView.builder(
              padding: new EdgeInsets.all(8.0),
              itemCount: _recentArticles.length,
              itemBuilder: (BuildContext context, int index) {
                return new ArticleCard(
                  article: _recentArticles[index],
                  onCardClick: () {
//                      Navigator.of(context).push(
//                          new FadeRoute(
//                            builder: (BuildContext context) => new BookNotesPage(_items[index]),
//                            settings: new RouteSettings(name: '/notes', isInitialRoute: false),
//                          ));
                  },
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
    );
  }
}
