import 'package:dev_feed/env.dart';

void main() => Heroku();

class Heroku extends Env {
  final String baseUrl = 'https://dev-feed-api.onrender.com';
}
