import 'dart:async';

import 'package:awesome_dev/api/api.dart';

class TagsClient {
  static final TagsClient _singleton = TagsClient._internal();

  //Leveraging Dart Factory constructors to build singletons
  factory TagsClient() {
    return _singleton;
  }

  TagsClient._internal();

  Future<List<String>> _getTags(String graphqlQuery, String queryKey) async {
    var map = await issueGraphQLQuery(graphqlQuery);
    final dataMap = map["data"];
    var tagsList = dataMap[queryKey];
    if (tagsList == null) {
      throw StateError('No content');
    }
    List<String> result = [];
    for (var tag in tagsList) {
      result.add(tag.toString());
    }
    result
        .sort((tag1, tag2) => tag1.toLowerCase().compareTo(tag2.toLowerCase()));
    return result;
  }

  Future<List<String>> getTags({int limit, int offset, String search}) async {
    String query = "query { \n "
        " tags";
    if (limit != null ||
        offset != null ||
        (search != null && search.isNotEmpty)) {
      final params = <String>[];
      if (limit != null) {
        params.add("limit: $limit");
      }
      if (offset != null) {
        params.add("offset: $offset");
      }
      if (search.isNotEmpty) {
        params.add("search: $search");
      }
      if (params.isNotEmpty) {
        query += "(";
        query += params.join(", ");
        query += ")";
      }
    }
    query += "\n"
        "}";

    return _getTags(query, "tags");
  }
}
