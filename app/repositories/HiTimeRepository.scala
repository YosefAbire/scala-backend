package repositories

import javax.inject.{Inject, Singleton}
import domain._
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

@Singleton
class HiTimeRepository @Inject() ():
  private val tasks = new ConcurrentHashMap[Long, TaskItem]()
  private val routines = new ConcurrentHashMap[Long, RoutineItem]()
  private val sessions = new ConcurrentHashMap[Long, FocusSession]()
  private val taskIdGen = new java.util.concurrent.atomic.AtomicLong(100)

  // Seed default tasks matching frontend defaults
  private val t1 = TaskItem(1, 1, "Calculus BC Problem Set 9 (Problems 12–24)", "Math", "Now", "Now", "40 min", false, notes = "Active Target")
  private val t2 = TaskItem(2, 1, "Revise Physics lab methodology section", "Physics", "Now", "Now", "20 min", false)
  private val t3 = TaskItem(3, 1, "Review AP Macroeconomics formulas & graphs", "Economics", "Now", "Completed at 11:20 AM", "15 min", true)
  private val t4 = TaskItem(4, 1, "Read Literature Ch. 6–8 (The Great Gatsby)", "English", "Next", "Thursday", "45 min", false)
  private val t5 = TaskItem(5, 1, "Draft History essay thesis & outline", "History", "Next", "Friday", "35 min", false)
  tasks.put(t1.id, t1)
  tasks.put(t2.id, t2)
  tasks.put(t3.id, t3)
  tasks.put(t4.id, t4)
  tasks.put(t5.id, t5)

  // Seed default routines
  private val r1 = RoutineItem(1, 1, "morning", "Review Today's Now Tasks in HiTime", done = true)
  private val r2 = RoutineItem(2, 1, "morning", "Check Physics Lab Uncertainty Calculations", done = true)
  private val r3 = RoutineItem(3, 1, "morning", "Prepare Calculus Problem Set 9 Materials", done = false)
  private val r4 = RoutineItem(4, 1, "evening", "Log completed tasks & update Next/Later queue", done = false)
  routines.put(r1.id, r1)
  routines.put(r2.id, r2)
  routines.put(r3.id, r3)
  routines.put(r4.id, r4)

  def findTasksByUser(userId: Long): Seq[TaskItem] =
    tasks.values().asScala.toSeq.filter(_.userId == userId).sortBy(-_.createdAt.toEpochMilli)

  def createTask(userId: Long, title: String, subject: String, duePeriod: String, timeEstimate: String, notes: String = ""): TaskItem =
    val id = taskIdGen.incrementAndGet()
    val task = TaskItem(id, userId, title, subject, duePeriod, "Today", timeEstimate, completed = false, notes = notes)
    tasks.put(id, task)
    task

  def updateTaskStatus(id: Long, completed: Boolean): Option[TaskItem] =
    Option(tasks.get(id)).map { t =>
      val updated = t.copy(completed = completed)
      tasks.put(id, updated)
      updated
    }

  def deleteTask(id: Long): Boolean =
    tasks.remove(id) != null

  def findRoutinesByUser(userId: Long): Seq[RoutineItem] =
    routines.values().asScala.toSeq.filter(_.userId == userId)

  def toggleRoutineStatus(id: Long, done: Boolean): Option[RoutineItem] =
    Option(routines.get(id)).map { r =>
      val updated = r.copy(done = done)
      routines.put(id, updated)
      updated
    }

  def findSessionsByUser(userId: Long): Seq[FocusSession] =
    sessions.values().asScala.toSeq.filter(_.userId == userId)

  def createFocusSession(userId: Long, mode: String, durationMinutes: Int): FocusSession =
    val id = System.currentTimeMillis()
    val session = FocusSession(id, userId, None, mode, durationMinutes, completed = true)
    sessions.put(id, session)
    session

