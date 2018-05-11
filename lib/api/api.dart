import 'dart:async';
import 'dart:convert' show utf8, json;
import 'dart:io';

final _apiEndpoint = Uri.parse('http://tools.rm3l.org:9000/graphql');
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
    ..headers.add(HttpHeaders.ACCEPT, ContentType.JSON)
    ..headers.add(HttpHeaders.USER_AGENT, 'org.rm3l.discoverdev.io')
    ..headers.contentType = ContentType.JSON
    ..headers.contentLength = requestBody.length
    ..headers.chunkedTransferEncoding = false;

  request.write(requestBody);
  HttpClientResponse response = await request.close();
  if (response.headers.contentType.toString() != ContentType.JSON.toString()) {
    throw UnsupportedError('Server returned an unsupported content type: '
        '${response.headers.contentType} from ${request.uri}');
  }
  if (response.statusCode != HttpStatus.OK) {
    throw StateError(
        'Server responded with error: ${response.statusCode} ${response.reasonPhrase}');
  }
  return json.decode(await response.transform(utf8.decoder).join());
}
