import 'package:fluro/fluro.dart';
import 'package:dev_feed/config/application.dart';
import 'package:dev_feed/config/routes.dart';
import 'package:flutter/material.dart';
import 'main.dart';

class Env {
  static Env value;

  String baseUrl;

  Env() {
//  assert(() {
//    //assert will execute only in Debug Mode
//    //Note in particular the () at the end of the call -
//    // assert can only operate on a boolean, so just passing in a function doesn't work.
//    HttpOverrides.global = StethoHttpOverrides();
//    return true;
//  }());

    value = this;

    final router = Router();
    Routes.configureRoutes(router);
    Application.router = router;

    runApp(DevFeedApp(this));
  }

  String get name => runtimeType.toString();
}
