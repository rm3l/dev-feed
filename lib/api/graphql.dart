import 'package:awesome_dev/api/articles.dart';

import 'package:http/http.dart';
import 'package:logging/logging.dart';
import 'package:graphql_client/graphql_client.dart';

final client = new Client();
final logger = new Logger('GQLClient'); // Optional.

final graphQLClient = new GQLClient(
  client: client,
  logger: logger,
  endPoint: API_ENDPOINT,
);