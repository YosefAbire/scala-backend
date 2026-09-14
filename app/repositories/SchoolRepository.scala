package repositories

import javax.inject.{Inject, Singleton}
import domain._
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

@Singleton
class SchoolRepository @Inject() ():
  private val schools = new ConcurrentHashMap[Long, School]()
  private val idCounter = new java.util.concurrent.atomic.AtomicLong(10)

  // Seed default schools
  private val s1 = School(1, "St. Jude Collegiate Academy", "STJUDE")
  private val s2 = School(2, "Beacon Hill Preparatory Center", "BEACON")
  private val s3 = School(3, "Oakwood Senior High Center", "OAKWOOD")

  schools.put(s1.id, s1)
  schools.put(s2.id, s2)
  schools.put(s3.id, s3)

  def all(): Seq[School] =
    schools.values().asScala.toSeq.sortBy(_.name)

  def findById(id: Long): Option[School] =
    Option(schools.get(id))

  def findByCode(code: String): Option[School] =
    schools.values().asScala.find(_.code.equalsIgnoreCase(code))

  def create(name: String, code: String): School =
    val id = idCounter.incrementAndGet()
    val school = School(id = id, name = name, code = code.toUpperCase)
    schools.put(id, school)
    school
