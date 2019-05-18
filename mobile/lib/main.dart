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
import 'dart:io';

import 'package:dev_feed/config/application.dart';
import 'package:dev_feed/config/routes.dart';
import 'package:dev_feed/ui/about.dart';
import 'package:dev_feed/ui/archives.dart';
import 'package:dev_feed/ui/favorites.dart';
import 'package:dev_feed/ui/latest_news.dart';
import 'package:dev_feed/ui/search.dart';
import 'package:dev_feed/ui/tags.dart';
import 'package:fluro/fluro.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_stetho/flutter_stetho.dart';
//import 'package:logging/logging.dart';
import 'package:url_launcher/url_launcher.dart';

import 'package:app_review/app_review.dart';

const contactEmailAddress = "apps+dev_feed@rm3l.org";

enum AppBarMenuItem { ABOUT, SEND_FEEDBACK, RATING, GO_PREMIUM }

void main() {
//  assert(() {
//    //assert will execute only in Debug Mode
//    //Note in particular the () at the end of the call -
//    // assert can only operate on a boolean, so just passing in a function doesn't work.
//    HttpOverrides.global = StethoHttpOverrides();
//    return true;
//  }());

  final router = Router();
  Routes.configureRoutes(router);
  Application.router = router;

  runApp(DevFeedApp());
}

class DevFeedApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
//    Logger.root // Optional
//      ..level = Level.ALL
//      ..onRecord.listen((rec) {
//        print('${rec.level.name}: ${rec.time}: ${rec.message}');
//      });

    return MaterialApp(
      title: 'Dev Feed',
      theme: ThemeData(
          primaryColor: Colors.teal,
          backgroundColor: Colors.white,
          fontFamily: 'Inconsolata'),
      home: DevFeed(),
      onGenerateRoute: (routeSettings) =>
          Application.router.generator(routeSettings),
    );
  }
}

class DevFeed extends StatefulWidget {
  static const String routeName = '/material/bottom_navigation';

  @override
  State<StatefulWidget> createState() => _DevFeedState();
}

class _DevFeedState extends State<DevFeed> with TickerProviderStateMixin {
  // The AppBar's action needs this key to find its own Scaffold.
  final GlobalKey<ScaffoldState> _scaffoldKey = new GlobalKey<ScaffoldState>();

  //  SearchBar searchBar;
  int _currentIndex = 0;
  List<NavigationIconView> _navigationViews;

  String appID = "";
  String output = "";

  bool inAppProductPurchased = false;

  @override
  void initState() {
    super.initState();

    AppReview.getAppID.then((onValue) {
      setState(() {
        appID = onValue;
      });
      print("App ID: $appID");
    });

    _navigationViews = <NavigationIconView>[
      NavigationIconView(
        icon: const Icon(Icons.new_releases),
        title: 'Latest',
//        color: Colors.deepPurple,
        vsync: this,
      ),
      NavigationIconView(
        icon: const Icon(Icons.favorite),
        title: 'Favorites',
        color: Colors.indigo,
        vsync: this,
      ),
      NavigationIconView(
        icon: const Icon(Icons.search),
        title: 'Search',
        color: Colors.deepOrangeAccent,
        vsync: this,
      ),
      NavigationIconView(
        icon: const Icon(Icons.archive),
        title: 'Archives',
        color: Colors.deepOrange,
        vsync: this,
      ),
      NavigationIconView(
        icon: const Icon(Icons.label),
        title: 'Tags',
        color: Colors.teal,
        vsync: this,
      ),
    ];

    for (NavigationIconView view in _navigationViews) {
      view.controller.addListener(_rebuild);
    }
    _navigationViews[_currentIndex].controller.value = 1.0;
  }

  @override
  void dispose() {
    for (NavigationIconView view in _navigationViews) view.controller.dispose();
    super.dispose();
  }

  void _rebuild() {
    setState(() {
      // Rebuild in order to animate views.
    });
  }

  Widget _buildTransitionsStack() {
    if (_currentIndex == 0) {
      return const LatestNews();
    }
    if (_currentIndex == 1) {
      return const FavoriteNews();
    }
    if (_currentIndex == 2) {
      return const Search();
    }
    if (_currentIndex == 3) {
      return const ArticleArchives();
    }
    if (_currentIndex == 4) {
      return const Tags();
    }

    final List<FadeTransition> transitions = <FadeTransition>[];

    for (NavigationIconView view in _navigationViews) {
      transitions.add(view.transition(BottomNavigationBarType.fixed, context));
    }

    // We want to have the newly animating (fading in) views on top.
    transitions.sort((FadeTransition a, FadeTransition b) {
      final Animation<double> aAnimation = a.opacity;
      final Animation<double> bAnimation = b.opacity;
      final double aValue = aAnimation.value;
      final double bValue = bAnimation.value;
      return aValue.compareTo(bValue);
    });

    return Stack(children: transitions);
  }

  @override
  Widget build(BuildContext context) {
    final BottomNavigationBar botNavBar = BottomNavigationBar(
      items: _navigationViews
          .map((NavigationIconView navigationView) => navigationView.item)
          .toList(),
      currentIndex: _currentIndex,
      type: BottomNavigationBarType.fixed,
      onTap: (int index) {
        setState(() {
          _navigationViews[_currentIndex].controller.reverse();
          _currentIndex = index;
          _navigationViews[_currentIndex].controller.forward();
        });
      },
    );

    final List<PopupMenuEntry<AppBarMenuItem>> menuEntries = [];
    menuEntries.addAll(<PopupMenuItem<AppBarMenuItem>>[
      const PopupMenuItem<AppBarMenuItem>(
          value: AppBarMenuItem.ABOUT, child: const Text('About')),
      const PopupMenuItem<AppBarMenuItem>(
          value: AppBarMenuItem.SEND_FEEDBACK,
          child: const Text('Send Feedback')),
      const PopupMenuItem<AppBarMenuItem>(
          value: AppBarMenuItem.RATING, child: const Text('Rate this app!'))
    ]);
    //TODO Handle in-app billing in a future release
//    if (!inAppProductPurchased) {
//      menuEntries.add(const PopupMenuItem<AppBarMenuItem>(
//          value: AppBarMenuItem.GO_PREMIUM, child: const Text('Go Premium!')));
//    }
//    Application.billing.isPurchased(IN_APP_PRODUCT_ID).then((purchased) {
//      if (inAppProductPurchased != purchased) {
//        setState(() {
//          inAppProductPurchased = purchased;
//        });
//      }
//    });

    return Scaffold(
      key: _scaffoldKey,
      appBar: AppBar(
        title: const Text('Dev Feed'),
        actions: <Widget>[
          PopupMenuButton<AppBarMenuItem>(
            onSelected: (AppBarMenuItem value) {
              switch (value) {
                case AppBarMenuItem.ABOUT:
                  showGalleryAboutDialog(_scaffoldKey.currentContext);
                  break;
                case AppBarMenuItem.RATING:
                  {
                    AppReview.requestReview.catchError((onError) {
                      Scaffold.of(context).showSnackBar(SnackBar(
                        content: Text("Error: ${onError.toString()}"),
                      ));
                    });
                  }
                  break;
                case AppBarMenuItem.GO_PREMIUM:
                  {
                    Application.billing
                        .purchase(IN_APP_PRODUCT_ID)
                        .then((purchased) {
                      if (purchased) {
                        setState(() {
                          inAppProductPurchased = true;
                        });
                      } else {
                        Scaffold.of(context).showSnackBar(SnackBar(
                          content:
                              Text("An error ocurred. Please try again later"),
                        ));
                      }
                    });
                  }
                  break;
                case AppBarMenuItem.SEND_FEEDBACK:
                  {
                    //TODO For now, open up the default email address,
                    //but ultimately create a new 'maoni'-like plugin.

                    final ios =
                        Theme.of(_scaffoldKey.currentContext).platform ==
                            TargetPlatform.iOS;
                    final contactUrl =
                        "mailto:$contactEmailAddress}?subject=About+Dev+Feed+app+on+"
                        "${ios ? "iOS" : "Android"}";
                    canLaunch(contactUrl).then((onValue) {
                      launch(contactUrl).catchError((error) {
                        print("Error: ${error.toString()}");
                      });
                    }, onError: (error) {
                      _scaffoldKey.currentState.showSnackBar(SnackBar(
                        content: const Text("Could not open up email app. "
                            "Please reach out to us at $contactEmailAddress!"),
                        duration: Duration(seconds: 7),
                        backgroundColor: Colors.deepPurple,
                        action: SnackBarAction(
                          label: 'Copy email',
                          onPressed: () {
                            Clipboard.setData(
                                ClipboardData(text: contactEmailAddress));
                            _scaffoldKey.currentState.showSnackBar(
                                SnackBar(content: const Text("Copied!")));
                          },
                        ),
                      ));
                    });
                  }
                  break;
                default:
                  throw UnsupportedError("Unsupported menu item: $value");
              }
            },
            itemBuilder: (BuildContext context) => menuEntries,
          ),
        ],
      ),
      body: Center(child: _buildTransitionsStack()),
      bottomNavigationBar: botNavBar,
    );
  }
}

class NavigationIconView {
  NavigationIconView({
    Widget icon,
    String title,
    Color color,
    TickerProvider vsync,
  })  : _icon = icon,
        _color = color,
        _title = title,
        item = BottomNavigationBarItem(
          icon: icon,
          title: Text(title),
          backgroundColor: color,
        ),
        controller = AnimationController(
          duration: kThemeAnimationDuration,
          vsync: vsync,
        ) {
    _animation = CurvedAnimation(
      parent: controller,
      curve: const Interval(0.5, 1.0, curve: Curves.fastOutSlowIn),
    );
  }

  final Widget _icon;
  final Color _color;
  final String _title;
  final BottomNavigationBarItem item;
  final AnimationController controller;
  CurvedAnimation _animation;

  FadeTransition transition(
      BottomNavigationBarType type, BuildContext context) {
    Color iconColor;
    if (type == BottomNavigationBarType.shifting) {
      iconColor = _color;
    } else {
      final ThemeData themeData = Theme.of(context);
      iconColor = themeData.brightness == Brightness.light
          ? themeData.primaryColor
          : themeData.accentColor;
    }

    return FadeTransition(
      opacity: _animation,
      child: SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0.0, 0.02), // Slightly down.
          end: Offset.zero,
        ).animate(_animation),
        child: IconTheme(
          data: IconThemeData(
            color: iconColor,
            size: 120.0,
          ),
          child: Semantics(
            label: 'Placeholder for $_title tab',
            child: _icon,
          ),
        ),
      ),
    );
  }
}
