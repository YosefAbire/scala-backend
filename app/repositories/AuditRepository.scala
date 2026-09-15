package repositories

import javax.inject.{Inject, Singleton}
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters._
import play.api.libs.json._
import play.api.Logger

case class AuditEntry(
    id: Long,
    actorId: Long,
    actorEmail: String,
    actorRole: String,
    action: String,
    schoolId: Option[Long],
    targetEntity: String,
    targetId: Option[Long],
    result: String,
    timestamp: Instant = Instant.now(),
    details: String = ""
)

object AuditEntry:
  implicit val format: OFormat[AuditEntry] = Json.format[AuditEntry]

@Singleton
class AuditRepository @Inject() ():
  private val logger = Logger(classOf[AuditRepository])
  private val entries = new ConcurrentHashMap[Long, AuditEntry]()
  private val idCounter = new AtomicLong(1)

  def record(
      actorId: Long,
      actorEmail: String,
      actorRole: String,
      action: String,
      schoolId: Option[Long],
      targetEntity: String,
      targetId: Option[Long],
      result: String,
      details: String = ""
  ): AuditEntry = {
    val id = idCounter.getAndIncrement()
    val entry = AuditEntry(
      id = id,
      actorId = actorId,
      actorEmail = actorEmail,
      actorRole = actorRole,
      action = action,
      schoolId = schoolId,
      targetEntity = targetEntity,
      targetId = targetId,
      result = result,
      timestamp = Instant.now(),
      details = details
    )
    entries.put(id, entry)
    logger.info(s"[AUDIT LOG] id=$id actor=$actorEmail role=$actorRole action=$action school=${schoolId.getOrElse(0)} target=$targetEntity:$targetId result=$result")
    entry
  }

  def all(): Seq[AuditEntry] =
    entries.values().asScala.toSeq.sortBy(_.timestamp.toEpochMilli).reverse

  def findBySchool(schoolId: Long): Seq[AuditEntry] =
    all().filter(_.schoolId.contains(schoolId))
