package repositories

import javax.inject.{Inject, Singleton}
import domain._
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

@Singleton
class NotificationRepository @Inject() ():
  private val notifications = new ConcurrentHashMap[Long, NotificationItem]()
  private val idGen = new java.util.concurrent.atomic.AtomicLong(1)

  // Seed initial notification
  private val n1 = NotificationItem(1, 1, "AP Chemistry Quiz Available", "New chapter diagnostic quiz posted for Thermodynamics.", "academic")
  notifications.put(n1.id, n1)

  def findByUser(userId: Long): Seq[NotificationItem] =
    notifications.values().asScala.toSeq.filter(_.userId == userId).sortBy(-_.createdAt.toEpochMilli)

  def markAsRead(id: Long, userId: Long): Option[NotificationItem] =
    Option(notifications.get(id)).filter(_.userId == userId).map { item =>
      val updated = item.copy(isRead = true)
      notifications.put(id, updated)
      updated
    }

  def create(userId: Long, title: String, message: String, category: String = "academic"): NotificationItem =
    val id = idGen.incrementAndGet()
    val item = NotificationItem(id, userId, title, message, category, isRead = false)
    notifications.put(id, item)
    item
