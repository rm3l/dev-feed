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
import 'dart:convert' show utf8, json;
import 'dart:io';

import 'package:dev_feed/env.dart';

final _apiEndpoint = Uri.parse('${Env.value.baseUrl}/graphql');
final _httpClient = HttpClient();

/*
Example of query
  recentArticles {
    timestamp
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
 */
Future<Map<String, dynamic>> issueGraphQLQuery(String query,
    {String operationName = "",
    Map<String, dynamic> variables = const {}}) async {
  Map<String, dynamic> jsonBody = {
    "query": query,
    "operationName": operationName,
    "variables": variables
  };

  final String requestBody = json.encode(jsonBody);
  HttpClientRequest request = await _httpClient.postUrl(_apiEndpoint)
    ..headers.add(HttpHeaders.acceptHeader, ContentType.json)
    ..headers.add(HttpHeaders.userAgentHeader, 'org.rm3l.dev_feed')
    ..headers.contentType = ContentType.json
    ..headers.contentLength = requestBody.length
    ..headers.chunkedTransferEncoding = false;

  request.write(requestBody);
  HttpClientResponse response = await request.close();
  if (response.headers.contentType.toString() != ContentType.json.toString()) {
    throw UnsupportedError('Server returned an unsupported content type: '
        '${response.headers.contentType} from ${request.uri}');
  }
  if (response.statusCode != HttpStatus.ok) {
    throw StateError(
        'Server responded with error: ${response.statusCode} ${response.reasonPhrase}');
  }
  return json.decode(await response.transform(utf8.decoder).join());
}
