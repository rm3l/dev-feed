import 'package:graphql_client/graphql_dsl.dart';

const API_ENDPOINT = 'http://tools.rm3l.org:9000/graphql';

/*
Example of query
query GetRecentArticles {
  recentArticles {
    date
    title
    description
    url
    domain
    tags
    screenshot {
      height
      width
      mimeType
      data
    }
  }
}
 */
class ArticlesQuery extends Object with ScalarCollection<ArticlesResolver>, Fields implements GQLOperation {

  String _queryName;
  String _queryResolverName;

  ArticlesResolver viewer;

  ArticlesQuery(String queryName, String queryResolverName) {
    this._queryName = queryName;
    this._queryResolverName = queryResolverName;
    this.viewer = new ArticlesResolver(queryResolverName);
  }

  @override
  GQLField clone() =>
      new ArticlesQuery(_queryName, _queryResolverName)..viewer = viewer.clone();

  @override
  String get name => _queryName;

  @override
  String get type => queryType;

  @override
  List<GQLField> get fields => [viewer];

}

class ArticlesResolver extends Object with Fields implements GQLField {

  DateResolver date = new DateResolver();
  TitleResolver title = new TitleResolver();
  DescriptionResolver description = new DescriptionResolver();
  UrlResolver url = new UrlResolver();
  DomainResolver domain = new DomainResolver();
  TagsResolver tags = new TagsResolver();
  ScreenshotResolver screenshot = new ScreenshotResolver();

  String _queryResolverName;

  ArticlesResolver(this._queryResolverName);

  @override
  GQLField clone() => new ArticlesResolver(this._queryResolverName)
    ..date = date.clone()
    ..title = title.clone()
    ..description = description.clone()
    ..url = url.clone()
    ..domain = domain.clone()
    ..tags = tags.clone()
    ..screenshot = screenshot.clone();

  @override
  String get name => _queryResolverName;

  @override
  List<GQLField> get fields => [date, title, description, url, domain, tags, screenshot];

}

class DateResolver extends Object with Scalar<String>, Fields implements GQLField {
  @override
  String get name => 'date';

  @override
  DateResolver clone() => new DateResolver();
}

class TitleResolver extends Object with Scalar<String>, Fields implements GQLField {
  @override
  String get name => 'title';

  @override
  TitleResolver clone() => new TitleResolver();
}

class DescriptionResolver extends Object with Scalar<String>, Fields implements GQLField {
  @override
  String get name => 'description';

  @override
  DescriptionResolver clone() => new DescriptionResolver();
}

class UrlResolver extends Object with Scalar<String>, Fields implements GQLField {
  @override
  String get name => 'url';

  @override
  UrlResolver clone() => new UrlResolver();
}

class DomainResolver extends Object with Scalar<String>, Fields implements GQLField {
  @override
  String get name => 'domain';

  @override
  DomainResolver clone() => new DomainResolver();
}

class TagsResolver extends Object with Scalar<List<String>>, Fields implements GQLField {
  @override
  String get name => 'tags';

  @override
  TagsResolver clone() => new TagsResolver();
}

class ScreenshotResolver extends Object with Fields implements GQLField {

  ScreenshotWidthResolver width = new ScreenshotWidthResolver();
  ScreenshotHeightResolver height = new ScreenshotHeightResolver();
  ScreenshotMimeTypeResolver mimeType = new ScreenshotMimeTypeResolver();
  ScreenshotDataResolver data = new ScreenshotDataResolver();

  @override
  GQLField clone() => new ScreenshotResolver()
    ..width = width.clone()
    ..height = height.clone()
    ..mimeType = mimeType.clone()
    ..data = data.clone();

  @override
  String get name => "screenshot";

  @override
  List<GQLField> get fields => [width, height, mimeType, data];

}

class ScreenshotWidthResolver extends Object with Scalar<num>, Fields implements GQLField {
  @override
  String get name => 'width';

  @override
  ScreenshotWidthResolver clone() => new ScreenshotWidthResolver();
}

class ScreenshotHeightResolver extends Object with Scalar<num>, Fields implements GQLField {
  @override
  String get name => 'height';

  @override
  ScreenshotHeightResolver clone() => new ScreenshotHeightResolver();
}

class ScreenshotMimeTypeResolver extends Object with Scalar<String>, Fields implements GQLField {
  @override
  String get name => 'mimeType';

  @override
  ScreenshotMimeTypeResolver clone() => new ScreenshotMimeTypeResolver();
}

class ScreenshotDataResolver extends Object with Scalar<String>, Fields implements GQLField {
  @override
  String get name => 'data';

  @override
  ScreenshotDataResolver clone() => new ScreenshotDataResolver();
}