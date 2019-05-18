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

import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:package_info/package_info.dart';
import 'package:url_launcher/url_launcher.dart';

class _LinkTextSpan extends TextSpan {
  // Beware!
  //
  // This class is only safe because the TapGestureRecognizer is not
  // given a deadline and therefore never allocates any resources.
  //
  // In any other situation -- setting a deadline, using any of the less trivial
  // recognizers, etc -- you would have to manage the gesture recognizer's
  // lifetime and call dispose() when the TextSpan was no longer being rendered.
  //
  // Since TextSpan itself is @immutable, this means that you would have to
  // manage the recognizer from outside the TextSpan, e.g. in the State of a
  // stateful widget that then hands the recognizer to the TextSpan.

  _LinkTextSpan({TextStyle style, String url, String text})
      : super(
            style: style,
            text: text ?? url,
            recognizer: TapGestureRecognizer()
              ..onTap = () {
                launch(url, forceSafariVC: false);
              });
}

Future<Null> showGalleryAboutDialog(BuildContext context) async {
  final PackageInfo packageInfo = await PackageInfo.fromPlatform();

  final ThemeData themeData = Theme.of(context);
  final TextStyle aboutTextStyle = themeData.textTheme.body2;
  final TextStyle linkStyle =
      themeData.textTheme.body2.copyWith(color: themeData.accentColor);

  showAboutDialog(
    context: context,
    applicationVersion: packageInfo.version,
    applicationIcon: Image.asset("assets/icons/dev_feed.png",
        fit: BoxFit.scaleDown, width: 90.0),
    applicationLegalese: '© 2018 Armel S.',
    children: <Widget>[
      Padding(
        padding: const EdgeInsets.only(top: 24.0),
        child: RichText(
          text: TextSpan(
            children: <TextSpan>[
              TextSpan(
                  style: aboutTextStyle,
                  text:
                      'Dev Feed allows you to keep up with the top engineering '
                      'content from companies all over the world!'
//                      '\nContent carefully handpicked by AI and a network of globally distributed nerds!'
                      '\nAvailable on '
                      '${defaultTargetPlatform == TargetPlatform.iOS ? 'multiple platforms' : 'both iOS and Android'}.')
//                      '\n\nThanks to Flutter ('),
//              _LinkTextSpan(
//                style: linkStyle,
//                url: 'https://flutter.io',
//              ),
//              TextSpan(
//                  style: aboutTextStyle,
//                  text: '), this mobile app is available on '
//                      '${defaultTargetPlatform == TargetPlatform.iOS ? 'multiple platforms' : 'iOS and Android'} '
//                      'from a single codebase.'),
            ],
          ),
        ),
      ),
    ],
  );
}
