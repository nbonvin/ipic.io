/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */
package controllers

import scala.concurrent._
import ExecutionContext.Implicits.global

import play.api.libs.iteratee._


import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.ws._
import play.api.libs.oauth._
import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._
import play.Play
import java.net.URLEncoder
import play.api.libs.oauth.OAuth
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.oauth.ConsumerKey


/**
 * Tweet wall, using new Twitter Stream API.
 *
 * Created by nmartignole on 03/04/2014.
 */
/*
object Tweetwall extends Controller {

  val cfg = Play.application.configuration

  // Set your Twitter key in your application.conf
  val KEY = ConsumerKey(cfg.getString("twitter.consumerKey"), cfg.getString("twitter.consumerSecret"))

  val TWITTER = OAuth(ServiceInfo(
    "https://api.twitter.com/oauth/request_token",
    "https://api.twitter.com/oauth/access_token",
    "https://api.twitter.com/oauth/authorize", KEY),
    use10a = false)

  def index = Action {
    implicit request =>
      request.session.get("token").map {
        token: String =>
          Ok(views.html.Tweetwall.wallDevoxxFR2014()) // yes ma biche, c'est pour Devoxx France
      }.getOrElse {
        Redirect(routes.Tweetwall.authenticate)
      }
  }

  def authenticate = Action {
    implicit request =>
      request.queryString.get("oauth_verifier").flatMap(_.headOption).map {
        verifier =>
          val tokenPair = sessionTokenPair(request).get
          // We got the verifier; now get the access token, store it and back to index
          TWITTER.retrieveAccessToken(tokenPair, verifier) match {
            case Right(t) => {
              // We received the unauthorized tokens in the OAuth object - store it before we proceed
              Redirect(routes.Tweetwall.index).withSession("token" -> t.token, "secret" -> t.secret)
            }
            case Left(e) => throw e
          }
      }.getOrElse(
          TWITTER.retrieveRequestToken(routes.Tweetwall.authenticate.absoluteURL()) match {
            case Right(t) => {
              // We received the unauthorized tokens in the OAuth object - store it before we proceed
              Redirect(TWITTER.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
            }
            case Left(e) => throw e
          })
  }

  // Ce code, tres simple quand on le voit comme ça, fait revenir l'etre aimé, repousser les cheveux et aussi
  // peut streamer directement Twitter
  def watchTweets(keywords: String) = Action {
    implicit request =>

      val (tweetsOut, tweetChanel) = Concurrent.broadcast[JsValue]
      // See Twitter parameters doc https://dev.twitter.com/docs/streaming-apis/parameters
      WS.url(s"https://stream.twitter.com/1.1/statuses/filter.json?stall_warnings=true&filter_level=none&track=" + URLEncoder.encode(keywords, "UTF-8"))
        .withRequestTimeout(-1) // Connected forever
        .sign(OAuthCalculator(KEY, sessionTokenPair.get))
        .withHeaders("Connection" -> "keep-alive")
        .postAndRetrieveStream("")(headers => Iteratee.foreach[Array[Byte]] {
        ba =>
          val msg = new String(ba, "UTF-8")
          val tweet = Json.parse(msg)
          tweetChanel.push(tweet)
      }).flatMap(_.run)

      // Je suis plus un fan d'Event-Source, bien que cela marche pas avec IE
      // Voir la référence ici http://caniuse.com/#feat=eventsource
      Ok.feed(tweetsOut &> EventSource()).as("text/event-stream")
  }

  def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
    for {
      token <- request.session.get("token")
      secret <- request.session.get("secret")
    } yield {
      RequestToken(token, secret)
    }
  }
}
*/