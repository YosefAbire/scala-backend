package repositories

import javax.inject.{Inject, Singleton}
import domain._
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._
import java.time.Instant

@Singleton
class HiSchoolRepository @Inject() ():
  private val notes = new ConcurrentHashMap[Long, StudyNote]()
  private val quizzes = new ConcurrentHashMap[Long, PracticeQuiz]()
  private val quizAttempts = new ConcurrentHashMap[Long, QuizAttempt]()
  private val circles = new ConcurrentHashMap[Long, StudyCircle]()
  private val pathways = new ConcurrentHashMap[Long, GraduatePathway]()
  private val discussions = new ConcurrentHashMap[Long, DiscussionPost]()
  private val masteries = new ConcurrentHashMap[String, SubjectMastery]()
  private val subjectProgressMap = new ConcurrentHashMap[String, SubjectLearningProgress]()

  // Seed default masteries & topic progress for demo user 1
  private val physicsTopics = List(
    TopicMastery("Kinematics", 82, 3, 82),
    TopicMastery("Newton's Laws", 61, 2, 60),
    TopicMastery("Energy", 74, 4, 75),
    TopicMastery("Momentum", 43, 1, 40)
  )
  private val physProgress = SubjectLearningProgress(1, "Physics", 65, 10, physicsTopics, List("Momentum", "Newton's Laws"))
  subjectProgressMap.put("1_physics", physProgress)

  private val chemTopics = List(
    TopicMastery("Equilibrium", 65, 3, 60),
    TopicMastery("Thermodynamics", 78, 2, 80),
    TopicMastery("Stoichiometry", 90, 5, 92)
  )
  private val chemProgress = SubjectLearningProgress(1, "AP Chemistry", 77, 10, chemTopics, List("Equilibrium"))
  subjectProgressMap.put("1_ap chemistry", chemProgress)

  private val calcTopics = List(
    TopicMastery("Taylor Series", 92, 5, 95),
    TopicMastery("Integration", 92, 4, 90)
  )
  private val calcProgress = SubjectLearningProgress(1, "AP Calculus", 92, 9, calcTopics, List("Integration"))
  subjectProgressMap.put("1_ap calculus", calcProgress)

  private val m1 = SubjectMastery(1, "AP Chemistry", 77, 3, List("Equilibrium"))
  private val m2 = SubjectMastery(1, "AP Calculus", 92, 5, List("Taylor Series"))
  private val m3 = SubjectMastery(1, "Physics", 65, 2, List("Momentum", "Newton's Laws"))
  masteries.put(s"1_${m1.subject.toLowerCase}", m1)
  masteries.put(s"1_${m2.subject.toLowerCase}", m2)
  masteries.put(s"1_${m3.subject.toLowerCase}", m3)
  
  private val noteIdGen = new java.util.concurrent.atomic.AtomicLong(10)
  private val quizAttemptIdGen = new java.util.concurrent.atomic.AtomicLong(100)
  private val discussionIdGen = new java.util.concurrent.atomic.AtomicLong(100)
  private val circleIdGen = new java.util.concurrent.atomic.AtomicLong(10)

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

  // Seed default quizzes with questions
  private val q1Questions = List(
    QuizQuestionItem(1, "Under what thermodynamic condition is ΔG guaranteed negative?", List("ΔH < 0 and ΔS > 0", "ΔH > 0 and ΔS < 0", "ΔH = 0", "T = 0 K"), 0, "When enthalpy is negative and entropy is positive, ΔG = ΔH - TΔS is always negative."),
    QuizQuestionItem(2, "What is the Gibbs Free Energy equation?", List("ΔG = ΔH - TΔS", "ΔG = ΔH + TΔS", "ΔG = TΔH - ΔS", "ΔG = ΔH / TΔS"), 0, "ΔG = ΔH - TΔS is the fundamental thermodynamic equation.")
  )
  private val q2Questions = List(
    QuizQuestionItem(1, "What is the derivative of f(x) = x^3 - 4x?", List("3x^2 - 4", "3x^2 - 4x", "x^2 - 4", "3x^3"), 0, "Power Rule yields 3x^2 - 4.")
  )

  private val q1 = PracticeQuiz(1, 1, "AP Chemistry Equilibrium & Entropy Sprint", "AP Chemistry", "formative", 2, 15, Some(92), q1Questions)
  private val q2 = PracticeQuiz(2, 1, "AP Calculus BC Integration Diagnostic", "AP Calculus", "timed_sprint", 1, 20, Some(88), q2Questions)
  quizzes.put(q1.id, q1)
  quizzes.put(q2.id, q2)

  // Seed default circles
  private val c1 = StudyCircle(1, 1, 1, "AP Chemistry Problem Solvers", "AP Chemistry", "Today • 4:00 PM", isLive = true, membersCount = 12, memberUserIds = List(1, 2, 4))
  private val c2 = StudyCircle(2, 1, 1, "Calculus BC Whiteboard Group", "AP Calculus", "Tomorrow • 5:30 PM", isLive = false, membersCount = 8, memberUserIds = List(1, 3))
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
  
  def findNoteById(id: Long): Option[StudyNote] = Option(notes.get(id))

  def createNote(title: String, subject: String, summary: String, authorId: Long = 1, schoolId: Long = 1): StudyNote =
    val id = noteIdGen.incrementAndGet()
    val note = StudyNote(
      id, schoolId, authorId, title, subject, "Chapter 1", summary,
      verified = false, verifiedByLabel = "", downloadCount = 0
    )
    notes.put(id, note)
    note

  def updateNote(id: Long, title: Option[String], summary: Option[String]): Option[StudyNote] =
    findNoteById(id).map { n =>
      val updated = n.copy(
        title = title.getOrElse(n.title),
        summary = summary.getOrElse(n.summary)
      )
      notes.put(id, updated)
      updated
    }

  def deleteNote(id: Long): Boolean = notes.remove(id) != null

  def verifyNote(id: Long, verifierId: Long, verifierLabel: String, comment: Option[String]): Option[StudyNote] =
    findNoteById(id).map { n =>
      val updated = n.copy(
        verified = true,
        verifiedById = Some(verifierId),
        verifiedByLabel = verifierLabel,
        verifiedAt = Some(Instant.now()),
        verificationComment = comment
      )
      notes.put(id, updated)
      updated
    }

  def incrementNoteDownload(id: Long): Option[StudyNote] =
    findNoteById(id).map { n =>
      val updated = n.copy(downloadCount = n.downloadCount + 1)
      notes.put(id, updated)
      updated
    }

  def allQuizzes(): Seq[PracticeQuiz] = quizzes.values().asScala.toSeq

  def findQuizById(id: Long): Option[PracticeQuiz] = Option(quizzes.get(id))

  def evaluateAndSaveQuizAttempt(quizId: Long, userId: Long, answers: Map[String, Int]): Option[QuizAttempt] =
    findQuizById(quizId).map { q =>
      val total = if (q.questions.nonEmpty) q.questions.length else q.questionsCount
      var correct = 0

      if (q.questions.nonEmpty) {
        q.questions.foreach { item =>
          val givenAnswer = answers.get(item.id.toString).orElse(answers.get(s"q_${item.id}"))
          if (givenAnswer.contains(item.correctOptionIndex)) {
            correct += 1
          }
        }
      } else {
        // Fallback calculation for sample quiz
        correct = Math.min(total, answers.size)
      }

      val percentage = if (total > 0) Math.round((correct.toDouble / total.toDouble) * 100).toInt else 100
      val attemptId = quizAttemptIdGen.incrementAndGet()
      val attempt = QuizAttempt(attemptId, quizId, userId, answers, percentage, total, correct)

      quizAttempts.put(attemptId, attempt)

      // Update quiz mastery score
      val updatedQuiz = q.copy(masteryScore = Some(percentage))
      quizzes.put(quizId, updatedQuiz)

      // Automatically update Subject Mastery & Topic Learning Progress state
      val topicName = q.title.replaceAll("AP |Sprint|Diagnostic|Integration|Equilibrium", "").trim match {
        case s if s.nonEmpty => s
        case _ => s"${q.subject} Core Topic"
      }
      updateMasteryFromAttempt(userId, q.subject, percentage)
      updateProgressFromQuizAttempt(userId, q.subject, topicName, percentage)

      attempt
    }

  def getUserLearningProgress(userId: Long): Seq[SubjectLearningProgress] =
    subjectProgressMap.values().asScala.toSeq.filter(_.userId == userId)

  def updateProgressFromQuizAttempt(userId: Long, subject: String, topic: String, quizScore: Int): SubjectLearningProgress =
    val key = s"${userId}_${subject.toLowerCase}"
    val existingOpt = Option(subjectProgressMap.get(key))
    
    val currentProgress = existingOpt.getOrElse(
      SubjectLearningProgress(userId, subject, quizScore, 0, List(TopicMastery(topic, quizScore, 1, quizScore)), List(topic))
    )

    val existingTopics = currentProgress.topicMasteries
    val targetTopicOpt = existingTopics.find(_.topic.equalsIgnoreCase(topic))
    
    val updatedTopics = targetTopicOpt match {
      case Some(tm) =>
        // Topic Mastery Formula: 70% latest score + 30% historical average
        val newScore = Math.round(0.7 * quizScore.toDouble + 0.3 * tm.masteryScore.toDouble).toInt
        existingTopics.map(t => if (t.topic.equalsIgnoreCase(topic)) t.copy(masteryScore = newScore, attemptsCount = t.attemptsCount + 1, lastScore = quizScore, lastAssessedAt = Instant.now()) else t)
      case None =>
        existingTopics :+ TopicMastery(topic, quizScore, 1, quizScore, Instant.now())
    }

    // Overall Subject Mastery Formula: Average of all topic masteries
    val overall = Math.round(updatedTopics.map(_.masteryScore).sum.toDouble / updatedTopics.length.toDouble).toInt
    val weakTopics = updatedTopics.filter(_.masteryScore < 75).map(_.topic)

    val updatedProgress = currentProgress.copy(
      overallMastery = overall,
      totalAssessmentsCompleted = currentProgress.totalAssessmentsCompleted + 1,
      topicMasteries = updatedTopics,
      recommendedFocusAreas = weakTopics,
      lastAssessedAt = Instant.now()
    )

    subjectProgressMap.put(key, updatedProgress)
    updatedProgress

  def applyStudyTaskBoost(userId: Long, subject: String, topic: String): Option[SubjectLearningProgress] =
    val key = s"${userId}_${subject.toLowerCase}"
    Option(subjectProgressMap.get(key)).map { progress =>
      val updatedTopics = progress.topicMasteries.map { tm =>
        if (tm.topic.equalsIgnoreCase(topic) || subject.equalsIgnoreCase(tm.topic)) {
          tm.copy(masteryScore = Math.min(100, tm.masteryScore + 3))
        } else tm
      }
      val overall = Math.round(updatedTopics.map(_.masteryScore).sum.toDouble / updatedTopics.length.toDouble).toInt
      val updated = progress.copy(
        overallMastery = overall,
        topicMasteries = updatedTopics,
        recommendedFocusAreas = updatedTopics.filter(_.masteryScore < 75).map(_.topic),
        lastAssessedAt = Instant.now()
      )
      subjectProgressMap.put(key, updated)
      updated
    }

  def getUserMasteries(userId: Long): Seq[SubjectMastery] =
    masteries.values().asScala.toSeq.filter(_.userId == userId)

  def updateMasteryFromAttempt(userId: Long, subject: String, scorePercentage: Int): SubjectMastery =
    val key = s"${userId}_${subject.toLowerCase}"
    val existingOpt = Option(masteries.get(key))
    val updated = existingOpt match {
      case Some(m) =>
        val newAttempts = m.totalAttempts + 1
        val newScore = Math.round((m.masteryScore * m.totalAttempts + scorePercentage).toDouble / newAttempts).toInt
        val updatedWeak = if (scorePercentage < 80) List(s"$subject Diagnostic Weak Area") else m.weakTopics
        m.copy(masteryScore = newScore, totalAttempts = newAttempts, weakTopics = updatedWeak, lastAssessedAt = Instant.now())
      case None =>
        SubjectMastery(userId, subject, scorePercentage, 1, if (scorePercentage < 80) List(s"$subject Fundamentals") else Nil, Instant.now())
    }
    masteries.put(key, updated)
    updated

  def findQuizResultsByUser(userId: Long): Seq[QuizAttempt] =
    quizAttempts.values().asScala.toSeq.filter(_.userId == userId).sortBy(-_.createdAt.toEpochMilli)

  def allCircles(): Seq[StudyCircle] = circles.values().asScala.toSeq

  def findCircleById(id: Long): Option[StudyCircle] = Option(circles.get(id))

  def createCircle(name: String, subject: String, leadId: Long, schoolId: Long = 1): StudyCircle =
    val id = circleIdGen.incrementAndGet()
    val circle = StudyCircle(id, schoolId, leadId, name, subject, "Tomorrow • 4:00 PM", isLive = false, membersCount = 1, memberUserIds = List(leadId))
    circles.put(id, circle)
    circle

  def joinCircle(id: Long, userId: Long): Option[StudyCircle] =
    findCircleById(id).map { c =>
      val updatedMembers = if (c.memberUserIds.contains(userId)) c.memberUserIds else userId :: c.memberUserIds
      val updated = c.copy(membersCount = updatedMembers.length, memberUserIds = updatedMembers)
      circles.put(id, updated)
      updated
    }

  def leaveCircle(id: Long, userId: Long): Option[StudyCircle] =
    findCircleById(id).map { c =>
      val updatedMembers = c.memberUserIds.filterNot(_ == userId)
      val updated = c.copy(membersCount = Math.max(0, updatedMembers.length), memberUserIds = updatedMembers)
      circles.put(id, updated)
      updated
    }

  def allPathways(): Seq[GraduatePathway] = pathways.values().asScala.toSeq

  def findDiscussionsByCircle(circleId: Long): Seq[DiscussionPost] =
    discussions.values().asScala.toSeq.filter(_.circleId == circleId).sortBy(_.id)

  def createDiscussionPost(circleId: Long, author: String, initials: String, content: String): DiscussionPost =
    val id = discussionIdGen.incrementAndGet()
    val post = DiscussionPost(id, circleId, author, initials, content, likes = 1, timestamp = "Just now")
    discussions.put(id, post)
    post
