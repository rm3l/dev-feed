import 'package:fluro/fluro.dart';

import 'package:awesome_dev/main.dart';
import 'package:awesome_dev/ui/tag_lookup.dart';

class Routes {
  static final _rootHandler =
      Handler(handlerFunc: (context, params) => AwesomeDevApp());
  static final _notFoundHandler = Handler(handlerFunc: (context, params) {
    print("ROUTE WAS NOT FOUND !!! $params");
  });
  static final _tagLookupHandler = Handler(
      handlerFunc: (context, parameters) => TagLookup(parameters["id"][0]));

  static void configureRoutes(Router router) {
    router.notFoundHandler = _notFoundHandler;
    router.define("/", handler: _rootHandler);

    router.define("/tags/:id", handler: _tagLookupHandler);
  }
}
