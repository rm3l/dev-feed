import 'package:dev_feed/env.dart';

void main() => Heroku();

class Heroku extends Env {
  //TODO Deploy on Heroku once ready
  final String baseUrl = 'http://10.10.10.134:8080';
}
