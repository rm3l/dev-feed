//The MIT License (MIT)
//
//Copyright (c) 2019 Armel Soro
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.
import 'dart:async';

import 'package:dev_feed/api/api.dart';

class TagsClient {
  static final TagsClient _singleton = TagsClient._internal();

  //Leveraging Dart Factory constructors to build singletons
  factory TagsClient() {
    return _singleton;
  }

  TagsClient._internal();

  Future<List<String>> _getTags(String graphqlQuery, String queryKey,
      {bool withCache = true}) async {
    var map = await issueGraphQLQuery(graphqlQuery, withCache: withCache);
    final dataMap = map["data"];
    var tagsList = dataMap[queryKey];
    if (tagsList == null) {
      throw StateError('No content');
    }
    List<String> result = [];
    for (var tag in tagsList) {
      if (tag.toString().length >= 2) {
        //At least '#<something>'
        result.add(tag.toString());
      }
    }
    result
        .sort((tag1, tag2) => tag1.toLowerCase().compareTo(tag2.toLowerCase()));
    return result;
  }

  Future<List<String>> getTags(
      {int limit, int offset, String search, bool withCache = true}) async {
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

    return _getTags(query, "tags", withCache: withCache);
  }
}
