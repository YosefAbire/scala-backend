package repositories

import javax.inject.{Inject, Singleton}
import domain._
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

@Singleton
class HiSchoolRepository @Inject() ():
  private val notes = new ConcurrentHashMap[Long, StudyNote]()
  private val quizzes = new ConcurrentHashMap[Long, PracticeQuiz]()
  private val circles = new ConcurrentHashMap[Long, StudyCircle]()
  private val pathways = new ConcurrentHashMap[Long, GraduatePathway]()
  private val discussions = new ConcurrentHashMap[Long, DiscussionPost]()
  
  private val noteIdGen = new java.util.concurrent.atomic.AtomicLong(10)
  private val discussionIdGen = new java.util.concurrent.atomic.AtomicLong(100)

  // Seed default notes
  private val n1 = StudyNote(
    1, 1, 1,
    "AP Chemistry: Thermodynamics & Free Energy Derivations",
    "AP Chemistry", "Ch. 8",
    "Detailed derivation of Gibbs free energy equation, entropy microstate distribution, and enthalpy change in equilibrium.",
    verified = true, verifiedByLabel = "Mr. Davies (Faculty Chair)", downloadCount = 142
  )
  private val n2 = StudyNote(
    2, 1, 1,
    "AP Calculus BC: Taylor Polynomials Error Bounds",
    "AP Calculus", "Ch. 9",
    "Lagrange error bound theorems and radius of convergence calculation proofs for alternating series.",
    verified = true, verifiedByLabel = "Dr. Aris Vance", downloadCount = 98
  )
  notes.put(n1.id, n1)
  notes.put(n2.id, n2)

  // Seed default quizzes
  private val q1 = PracticeQuiz(1, 1, "AP Chemistry Equilibrium & Entropy Sprint", "AP Chemistry", "formative", 10, 15, Some(92))
  private val q2 = PracticeQuiz(2, 1, "AP Calculus BC Integration Diagnostic", "AP Calculus", "timed_sprint", 15, 20, Some(88))
  quizzes.put(q1.id, q1)
  quizzes.put(q2.id, q2)

  // Seed default circles
  private val c1 = StudyCircle(1, 1, 1, "AP Chemistry Problem Solvers", "AP Chemistry", "Today • 4:00 PM", isLive = true, membersCount = 12)
  private val c2 = StudyCircle(2, 1, 1, "Calculus BC Whiteboard Group", "AP Calculus", "Tomorrow • 5:30 PM", isLive = false, membersCount = 8)
  circles.put(c1.id, c1)
  circles.put(c2.id, c2)

  // Seed default discussions
  private val d1 = DiscussionPost(1, 1, "Maya Chen", "MC", "Has anyone worked through Problem Set 9 question 14 on Taylor polynomials error bounds?", likes = 4, timestamp = "10 mins ago")
  private val d2 = DiscussionPost(2, 1, "Dr. Aris Vance", "AV", "Remember to check the (n+1)th derivative maximum bound on the interval [0, x].", likes = 8, timestamp = "5 mins ago")
  discussions.put(d1.id, d1)
  discussions.put(d2.id, d2)

  // Seed default pathways
  private val p1 = GraduatePathway(
    1, "Biomechanical Systems & Cellular Engineering",
    "Elena Rostova", "Class of 2021", "Johns Hopkins Department of Biomedical Engineering",
    "Focus heavily on Grade 11 AP Chemistry and Physics 1. Understanding principles deeply pays dividends in university lab research.",
    "Pair AP Chemistry and Calculus BC with whiteboard problem-solving study circles.",
    List("✓ AP Physics 1", "AP Chemistry", "Calculus BC", "Organic Prep")
  )
  private val p2 = GraduatePathway(
    2, "Applied Computer Science & AI Systems",
    "David Kim", "Class of 2022", "MIT School of Engineering",
    "Learn Discrete Math and Data Structures in Grade 11. Coding is just syntax; logic is structure.",
    "Focus on AP Calculus BC and Computer Science Principles.",
    List("✓ AP Calculus BC", "AP Computer Science A", "Linear Algebra")
  )
  pathways.put(p1.id, p1)
  pathways.put(p2.id, p2)

  def allNotes(): Seq[StudyNote] = notes.values().asScala.toSeq.sortBy(-_.createdAt.toEpochMilli)
  
  def createNote(title: String, subject: String, summary: String, authorId: Long = 1, schoolId: Long = 1): StudyNote =
    val id = noteIdGen.incrementAndGet()
    val note = StudyNote(
      id, schoolId, authorId, title, subject, "Chapter 1", summary,
      verified = true, verifiedByLabel = "Verified Faculty", downloadCount = 0
    )
    notes.put(id, note)
    note

  def incrementNoteDownload(id: Long): Option[StudyNote] =
    Option(notes.get(id)).map { n =>
      val updated = n.copy(downloadCount = n.downloadCount + 1)
      notes.put(id, updated)
      updated
    }

  def allQuizzes(): Seq[PracticeQuiz] = quizzes.values().asScala.toSeq
  def recordQuizScore(id: Long, score: Int): Option[PracticeQuiz] =
    Option(quizzes.get(id)).map { q =>
      val updated = q.copy(masteryScore = Some(score))
      quizzes.put(id, updated)
      updated
    }

  def allCircles(): Seq[StudyCircle] = circles.values().asScala.toSeq
  def allPathways(): Seq[GraduatePathway] = pathways.values().asScala.toSeq

  def findDiscussionsByCircle(circleId: Long): Seq[DiscussionPost] =
    discussions.values().asScala.toSeq.filter(_.circleId == circleId).sortBy(_.id)

  def createDiscussionPost(circleId: Long, author: String, initials: String, content: String): DiscussionPost =
    val id = discussionIdGen.incrementAndGet()
    val post = DiscussionPost(id, circleId, author, initials, content, likes = 1, timestamp = "Just now")
    discussions.put(id, post)
    post
