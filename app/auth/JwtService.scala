package auth

import javax.inject.{Inject, Singleton}
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.Configuration
import play.api.libs.json._
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant

case class UserClaim(
    userId: Long,
    email: String,
    role: String
)

object UserClaim:
  implicit val format: OFormat[UserClaim] = Json.format[UserClaim]

@Singleton
class JwtService @Inject() (config: Configuration):
  private val secretKey: String = config.getOptional[String]("play.http.secret.key").getOrElse("dev-secret-key-at-least-32-bytes")
  private val algorithm = JwtAlgorithm.HS256

  def hashPassword(plain: String): String =
    BCrypt.hashpw(plain, BCrypt.gensalt(12))

  def checkPassword(plain: String, hashed: String): Boolean =
    try
      BCrypt.checkpw(plain, hashed)
    catch
      case _: Exception => false

  def createToken(userId: Long, email: String, role: String, ttlSeconds: Long = 86400): String =
    val now = Instant.now().getEpochSecond
    val claim = Json.obj(
      "userId" -> userId,
      "email" -> email,
      "role" -> role,
      "iat" -> now,
      "exp" -> (now + ttlSeconds)
    )
    JwtJson.encode(claim, secretKey, algorithm)

  def validateToken(token: String): Option[UserClaim] =
    JwtJson.decodeJson(token, secretKey, Seq(algorithm)).toOption.flatMap { json =>
      for
        userId <- (json \ "userId").asOpt[Long]
        email <- (json \ "email").asOpt[String]
        role <- (json \ "role").asOpt[String]
      yield UserClaim(userId, email, role)
    }
