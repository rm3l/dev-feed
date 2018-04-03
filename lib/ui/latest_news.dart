import 'package:flutter/material.dart';
import 'package:awesome_dev/api/articles.dart';

class LatestNews extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => new LatestNewsState();
}

class LatestNewsState extends State<LatestNews> {
  final _recentArticles = <Article>[];

  final _savedArticles = new Set<Article>();

  final _biggerFont = const TextStyle(fontSize: 18.0);

  _fetchArticles(BuildContext context) async {
    final articlesClient = new ArticlesClient();
    try {
      final recentArticles = await articlesClient.getRecentArticles();
      _recentArticles.clear();
      _recentArticles.addAll(recentArticles);
    } on Exception catch (e) {
      Scaffold.of(context).showSnackBar(new SnackBar(
            content: new Text("Internal Error: ${e.toString()}"),
          ));
    }
  }

  @override
  Widget build(BuildContext context) {
    _fetchArticles(context);
    return new ListView.builder(
      padding: const EdgeInsets.all(16.0),
        itemCount: _recentArticles.length,
      itemBuilder: (context, i) {
//        if (i.isOdd) return new Divider();
        return _buildRow(_recentArticles[i]);
      },
    );
  }

  Widget _buildRow(Article article) {
    final alreadySaved = _savedArticles.contains(article);
    return new ListTile(
      title: new Text(
        article.title,
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
              _savedArticles.remove(article);
            } else {
              _savedArticles.add(article);
            }
          },
        );
      },
    );
  }
}
