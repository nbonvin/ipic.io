/*
 * The iPic.io Project (http://ipic.io)
 * Copyright (c) 2014 All rights reserved.
 *
 * Author: Nicolas Bonvin (@nbonvin)
 */

package util

import javax.crypto._
import javax.crypto.spec.{PBEKeySpec, IvParameterSpec, SecretKeySpec}
import java.io._
import java.nio.charset.Charset
import play.api.libs.Crypto
import play.api.Play

object CipherStream {

  val ALGORITHM = "AES"
  val BLOCK_MODE = "CBC"
  val PADDING = "PKCS5Padding"
  val CHARSET = Charset.forName( "UTF-8" )
  val SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA1"
  val ITERATION_COUNT = 5000
  val conf = Play.current.configuration
  val CBC_SALT = new IvParameterSpec(conf.getString("ipic.CBC_SALT").getOrElse("").split(",").map(_.trim).map(Integer.parseInt(_).toByte).toList.toArray)
  val KEY_SALT = conf.getString("ipic.KEY_SALT").getOrElse("").split(",").map(_.trim).map(Integer.parseInt(_).toByte).toList.toArray


  /**
   * Generate a key
   * @param adminToken
   * @return
   */
  private def getKey(adminToken: String): SecretKeySpec = {
    val key = Crypto.sign(adminToken).toCharArray
    val factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM)
    val secretKey = factory.generateSecret(new PBEKeySpec(key, KEY_SALT, ITERATION_COUNT, 256))
    val secretBytes = secretKey.getEncoded()
    return new SecretKeySpec(secretBytes, ALGORITHM )
  }

  /**
   * Get a Ciphered stream
   * @param adminToken
   * @param is
   * @param mode
   * @return
   */
  def getCipheredInputStream (adminToken: String, is: InputStream, mode: Int) = {
    val cipher = Cipher.getInstance( ALGORITHM + "/" + BLOCK_MODE + "/" + PADDING )
    val key = getKey(adminToken)
    cipher.init( mode, key, CBC_SALT )
    new CipherInputStream(is, cipher)
  }

  /**
   * Get an encrypt stream
   * @param adminToken
   * @param is
   * @return
   */
  def getEncryptInputStream (adminToken: String, is: InputStream) = getCipheredInputStream(adminToken, is,Cipher.ENCRYPT_MODE)

  /**
   * Get a decrypt stream
   * @param adminToken
   * @param is
   * @return
   */
  def getDecryptInputStream (adminToken: String, is: InputStream) = getCipheredInputStream(adminToken, is,Cipher.DECRYPT_MODE)

}

