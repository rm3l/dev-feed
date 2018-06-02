import 'package:fluro/fluro.dart';
import 'package:flutter_billing/flutter_billing.dart';

const IN_APP_PRODUCT_ID = "awesome_dev_premium";

class Application {
  static Router router;

  static final Billing billing = new Billing(onError: (exception) {
    print("Error: ${exception.toString()}");
  });
}
