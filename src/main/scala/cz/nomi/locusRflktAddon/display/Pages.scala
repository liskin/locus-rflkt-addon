/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon.display

import com.wahoofitness.common.{display => w}

import cz.nomi.locusRflktAddon.LocusService

object Pages {
  import w.DisplayAlignment._
  import w.DisplayButtonPosition._
  import w.DisplayFont._
  import Const.{Widget => W, Page => P, Custom => C}

  private def emptyGroup: Group = Group()

  private def statusWorkout: Group = {
    val recStopped = Icons.recStopped.frame(x = 1, y = 13)
    val recPaused = Icons.recPaused.frame(x = 1, y = 13).hidden
    Group(recStopped, recPaused).key(W.statusWorkout).frame(w = 128, h = 26)
  }

  private def time(key: String)(): Group = {
    val icon = Icons.clock.frame(x = 1, y = 1)
    val value = Text("--:--:--").frame(x = 20, y = 2, w = 108, h = 0)
      .font(SYSTEM19).align(CENTER).key("value")
    Group(icon, value).key(key).frame(w = 128, h = 26)
  }

  private lazy val widgetsNorth: Map[String, () => Group] = Map(
    W.clock -> time(W.clock) _,
    W.timeWorkout -> time(W.timeWorkout) _,
    W.timeMovingWorkout -> time(W.timeMovingWorkout) _
  )

  private def widgetNorth(key: String): Group =
    widgetsNorth.getOrElse(key, emptyGroup _)()

  case class UnitWidget(
    key: String,
    icon: () => Bitmap,
    unit: String,
    longUnit: String,
    description: String
  )

  lazy val unitWidgets = Seq(
    UnitWidget(W.speedCurrent, Icons.speed _, "KPH", "KPH", "Speed (current)"),
    UnitWidget(W.averageSpeedWorkout, Icons.speed _, "AVG", "total AVG", "Average speed – total (workout)"),
    UnitWidget(W.averageMovingSpeedWorkout, Icons.speed _, "AVG", "move AVG", "Average speed – moving (workout)"),
    UnitWidget(W.maxSpeedWorkout, Icons.speed _, "MAX", "MAX", "Max speed (workout)"),
    UnitWidget(W.distanceWorkout, Icons.distance _, "KM", "KM", "Distance (workout)"),
    UnitWidget(W.distanceUphillWorkout, Icons.distanceUphill _, "KM", "KM", "Distance – uphill (workout)"),
    UnitWidget(W.distanceDownhillWorkout, Icons.distanceDownhill _, "KM", "KM", "Distance – downhill (workout)"),
    UnitWidget(W.elevationCurrent, Icons.elevation _, "M", "M", "Elevation (current)"),
    UnitWidget(W.elevationUphillWorkout, Icons.elevationUphill _, "M", "M", "Elevation – uphill (workout)"),
    UnitWidget(W.elevationDownhillWorkout, Icons.elevationDownhill _, "M", "M", "Elevation – downhill (workout)"),
    UnitWidget(W.cadenceCurrent, Icons.cadence _, "RPM", "RPM", "Cadence (current)"),
    UnitWidget(W.heartRateCurrent, Icons.heartRate _, "BPM", "BPM", "Heart rate (current)"),
    UnitWidget(W.averageCadenceWorkout, Icons.cadence _, "AVG", "AVG RPM", "Average cadence (workout)"),
    UnitWidget(W.averageHeartRateWorkout, Icons.heartRate _, "AVG", "AVG BPM", "Average heart rate (workout)"),
    UnitWidget(W.battery, Icons.battery _, "%", "%", "Battery level")
  )

  private def unitGroup2x2(key: String, icon: => Bitmap, unit: String)(): Group = {
    val ic = icon.frame(x = 3, y = 3)
    val units = Text(unit).frame(x = 0, y = 3, w = 61, h = 0)
      .constant.font(SYSTEM10).align(RIGHT).key("units")
    val value = Text("--").frame(x = 0, y = 18, w = 64, h = 0)
      .font(SYSTEM26).align(CENTER).key("value")
    Group(ic, units, value).key(key).frame(w = 64, h = 51)
  }

  private def widget2x2(key: String): Group =
    unitWidgets.find(_.key == key).map(w =>
      unitGroup2x2(w.key, w.icon(), w.unit)
    ).getOrElse(emptyGroup)

  private def page2x2(widgets: ConfPage2x2) = {
    val top1 = widgetNorth(widgets.north)
    val top2 = statusWorkout
    val northWest = widget2x2(widgets.northWest)
    val northEast = widget2x2(widgets.northEast)
    val southWest = widget2x2(widgets.southWest)
    val southEast = widget2x2(widgets.southEast)
    Page(
      top1     .frame( 0,  0, 128, 26),
      top2     .frame( 0,  0, 128, 26),
      Rect()   .frame( 0, 26, 128,  0),
      northWest.frame( 0, 26,  64, 51),
      Rect()   .frame(64, 26,   0, 51),
      northEast.frame(64, 26,  64, 51),
      Rect()   .frame( 0, 77, 128,  0),
      southWest.frame( 0, 77,  64, 51),
      Rect()   .frame(64, 77,   0, 51),
      southEast.frame(64, 77,  64, 51)
    )
  }

  private def unitGroup1x3(key: String, icon: => Bitmap, unit: String)(): Group = {
    val ic = icon.frame(x = 1, y = 1)
    val units = Text(unit).frame(x = 0, y = 1, w = 127, h = 0)
      .constant.font(SYSTEM10).align(RIGHT).key("units")
    val value = Text("--").frame(x = 0, y = 9, w = 128, h = 0)
      .font(SYSTEM19).align(CENTER).key("value")
    Group(ic, units, value).key(key).frame(w = 128, h = 32)
  }

  private def widget1x3(key: String): Group =
    unitWidgets.find(_.key == key).map(w =>
      unitGroup1x3(w.key, w.icon(), w.longUnit)
    ).getOrElse(emptyGroup)

  private def page1x3(widgets: ConfPage1x3) = {
    val top1 = widgetNorth(widgets.north)
    val top2 = statusWorkout
    val line1 = widget1x3(widgets.line1)
    val line2 = widget1x3(widgets.line2)
    val line3 = widget1x3(widgets.line3)
    Page(
      top1     .frame( 0,  0, 128, 26),
      top2     .frame( 0,  0, 128, 26),
      Rect()   .frame( 0, 26, 128,  2),
      line1    .frame( 0, 28, 128, 32),
      Rect()   .frame( 0, 60, 128,  2),
      line2    .frame( 0, 62, 128, 32),
      Rect()   .frame( 0, 94, 128,  2),
      line3    .frame( 0, 96, 128, 32)
    )
  }

  private def navClock: Group = {
    val rect = Rect().frame(w = 128, h = 22)
    // TODO: add time to finish
    // TODO: switch between clock and time to finish by a chosen button
    val value = Text("--:--:--").frame(w = 128, h = 0)
      .font(SYSTEM19).align(CENTER).key("value")
    Group(rect, value).key(W.clock).frame(w = 128, h = 22)
  }

  private def navAction(reduced: Boolean): Group = {
    val rect = Rect().frame(w = 49, h = 33)
    val num = Text("8").frame(x = 1, y = 6, w = 47, h = 0)
      .font(SYSTEM19).align(CENTER).key("roundabout").hidden
    val iconSet = if (reduced) LocusService.reducedNavIcons
                  else LocusService.fullNavIcons
    val icons = Icons.navigation
      .filter(i => iconSet(i.getKey()))
      .map(_.inside(rect))
    Group((rect :: num :: icons): _*).frame(w = 49, h = 33)
  }

  private def navDist: Group = {
    val rect = Rect().frame(w = 80, h = 33)
    val units = Text("km").frame(x = 58, y = 21, w = 20, h = 0)
      .constant.font(SYSTEM10).align(RIGHT).key("units")
    val value = Text("--").frame(x = 0, y = 6, w = 58, h = 0)
      .font(SYSTEM19).align(RIGHT).key("value")
    Group(rect, units, value).frame(w = 80, h = 33)
  }

  private def navName: Group = {
    val rect = Rect().frame(w = 128, h = 22)
    val value = Text("--").frame(w = 128, h = 0)
      .font(SYSTEM19).align(CENTER).key("value")
    Group(rect, value).frame(w = 128, h = 22)
  }

  private def pageNav(c: ConfPageNav) = Page(
    navClock.frame(0, 0, 128, 22),

    navAction(c.reduced).key(W.nav1Action).frame(0, 21, 49, 33),
    navDist.key(W.nav1Dist).frame(48, 21, 80, 33),
    navName.key(W.nav1Name).frame(0, 53, 128, 22),

    navAction(c.reduced).key(W.nav2Action).frame(0, 74, 49, 33),
    navDist.key(W.nav2Dist).frame(48, 74, 80, 33),
    navName.key(W.nav2Name).frame(0, 106, 128, 22)
  ).custom(C.reduced, c.reduced.toString)
  .custom(C.autoSwitch, c.autoSwitch.toString)

  private def notifHeader: Group = {
    val rect = Rect().frame(w = 128, h = 27)
    val value = Text("--").frame(x = 1, y = 0, w = 126, h = 0)
      .font(SYSTEM19).align(CENTER).key("value")
    Group(rect, value).key(W.notifHeader).frame(w = 128, h = 27)
  }

  private def notifLine(i: Int): Group = {
    val value = Text("").frame(x = 0, y = 1, w = 128, h = 0)
      .font(SYSTEM10).align(CENTER).key("value")
    Group(value).key(W.notifLine(i)).frame(w = 128, h = 14)
  }

  private def pageNotif = Page(
    notifHeader.frame(x = 0, y = 0),
    notifLine(0).frame(x = 0, y = 28),
    notifLine(1).frame(x = 0, y = 42),
    notifLine(2).frame(x = 0, y = 56),
    notifLine(3).frame(x = 0, y = 70),
    notifLine(4).frame(x = 0, y = 84),
    notifLine(5).frame(x = 0, y = 98),
    notifLine(6).frame(x = 0, y = 112)
  )

  sealed trait ConfPage {
    def key: String
  }

  class ConfPageNav(val reduced: Boolean, val autoSwitch: Boolean) extends ConfPage {
    val key = P.navigation
  }

  class ConfPageNotif extends ConfPage {
    val key = P.notification
  }

  class ConfPage1x3(
    val key: String,
    val north: String,
    x: Conf1x3
  ) extends Conf1x3(x.line1, x.line2, x.line3)
    with ConfPage

  class ConfPage2x2(
    val key: String,
    val north: String,
    x: Conf2x2
  ) extends Conf2x2(x.northWest, x.northEast, x.southWest, x.southEast)
    with ConfPage

  class Conf1x3(
    val line1: String,
    val line2: String,
    val line3: String
  )

  class Conf2x2(
    val northWest: String,
    val northEast: String,
    val southWest: String,
    val southEast: String
  )

  private def page(c: ConfPage): Page = (c match {
    case c: ConfPage1x3 => page1x3(c)
    case c: ConfPage2x2 => page2x2(c)
    case c: ConfPageNav => pageNav(c)
    case _: ConfPageNotif => pageNotif
  }).key(c.key)

  def conf(buttons: Conf2x2, pages: Seq[ConfPage]): Configuration =
    Configuration(pages.map(page): _*)
      .id("LocusRflktAddon")
      .name("LocusRflktAddon")
      .button(NORTH_WEST, buttons.northWest)
      .button(NORTH_EAST, buttons.northEast)
      .button(SOUTH_WEST, buttons.southWest)
      .button(SOUTH_EAST, buttons.southEast)
}

object Const {
  object Function {
    val hwPageLeft = "hardwarePageLeft"
    val hwPageRight = "hardwarePageRight"
    val startStopWorkout = "START_STOP_WORKOUT"
    val backlight = "BACKLIGHT"
  }

  object Widget {
    val clock = "CLOCK"
    val timeWorkout = "TIME_WORKOUT"
    val timeMovingWorkout = "TIME_WORKOUT_MOVE"
    val statusWorkout = "STATUS_WORKOUT"

    val speedCurrent = "SPEED_CURRENT"
    val averageSpeedWorkout = "SPEED_WORKOUT_AVG"
    val averageMovingSpeedWorkout = "SPEED_WORKOUT_AVG_MOVE"
    val maxSpeedWorkout = "SPEED_WORKOUT_MAX"
    val distanceWorkout = "DISTANCE_WORKOUT"
    val distanceUphillWorkout = "DISTANCE_WORKOUT_UPHILL"
    val distanceDownhillWorkout = "DISTANCE_WORKOUT_DOWNHILL"
    val elevationCurrent = "ELEVATION_CURRENT"
    val elevationUphillWorkout = "ELEVATION_WORKOUT_UPHILL"
    val elevationDownhillWorkout = "ELEVATION_WORKOUT_DOWNHILL"
    val cadenceCurrent = "BIKE_CAD_CURRENT"
    val heartRateCurrent = "HR_CURRENT"
    val averageCadenceWorkout = "CADENCE_WORKOUT_AVG"
    val averageHeartRateWorkout = "HR_WORKOUT_AVG"

    val nav1Action = "NAV1_ACTION"
    val nav1Dist = "NAV1_DIST"
    val nav1Name = "NAV1_NAME"
    val nav2Action = "NAV2_ACTION"
    val nav2Dist = "NAV2_DIST"
    val nav2Name = "NAV2_NAME"

    val notifHeader = "NOTIFICATION_HEADER"
    def notifLine(i: Int) = s"NOTIFICATION_LINE$i"

    val battery = "BATTERY_LEVEL"
  }

  object Page {
    val navigation = "NAVIGATION"
    val notification = "NOTIFICATION"
  }

  object Custom {
    val reduced = "reduced"
    val autoSwitch = "autoSwitch"
  }
}

object Icons {
  def clock = Bitmap("/w8A//8PAAD/P8A/wP/ADz/wA//wDzz8D//Dw/8APzzw/8DAD/z/A/8D/AP8/wAA8P//APD/").frame(w = 18, h = 12).key("icon")
  def speed = Bitmap("////////A/z//wD/8//D/w//D/8/PD/8/8DD8P8D/wD/D/wP8D/A/wD/D/8P////////////").frame(w = 18, h = 12).key("icon")
  def distance = Bitmap("////8P///wD///8A/P//AMD//wMA//8DAPD/DwDA////A/z////w////D//////8////////").frame(w = 18, h = 12).key("icon")
  def distanceUphill = Bitmap("/w8A/P//A8D///8A/P//AMP//wD//PAA//8AAP8/AAD/PwAA/z8AAPAPAAAADwAAAAAAAAAA").frame(w = 18, h = 12).key("icon")
  def distanceDownhill = Bitmap("P/D///8/8P///z/w////P/A/8P8/8ADw/z8AAMD/DwAAwD8AAADA/w8AAAD/AAAAAA8AAAAA").frame(w = 18, h = 12).key("icon")
  def elevation = Bitmap("//////////////z///8D////D8D//z8A8PP/AAAM/A8AMAD/AMAAwA8ADADwADAAAAzAAAAA").frame(w = 18, h = 12).key("icon")
  def elevationUphill = Bitmap("////D////z/Az///ADDw///wAPz/Dw8AP//wAMDADw8AA/DwAAwADA/AAMDwAAMA8A8MAADw").frame(w = 18, h = 12).key("icon")
  def elevationDownhill = Bitmap("////D//////wz///Dz/w///wAPz/Dw8AP//wAMDADw8AAwwAAAwAAwzAAMDwAAMA8A8MAADw").frame(w = 18, h = 12).key("icon")
  def cadence = Bitmap("//8A////8A///8P/8/8///z8//PDz/8/D/z8/zPw8///ww///w8D/////P//PwD8////////").frame(w = 18, h = 12).key("icon")
  def heartRate = Bitmap("P8AP8P8AMAD8AwAAAD8AAADwAwAAAP8AAAD8PwAA8P8PAMD//wMA////APz//z/w////z///").frame(w = 18, h = 12).key("icon")
  def battery = Bitmap("/z/A//8PAAD///z/8//P/z////z/8//PADD//wwA8//P/z///wwA8//PADD///z/8/8PAAD/").frame(w = 18, h = 12).key("icon")

  def recStopped = Bitmap("////////////PwAAwP8DAAD8PwAAwP8DAAD8PwAAwP8DAAD8PwAAwP8DAAD8////////////").frame(w = 18, h = 12).key("rec_stopped")
  def recPaused = Bitmap("/////////////8A/8P8P/AP//8A/8P8P/AP//8A/8P8P/AP//8A/8P8P/AP/////////////").frame(w = 18, h = 12).key("rec_paused")

  def navigation = List(
    Bitmap("/////////////////////w8AwP//////////DwAAAPz///////8/AAAAAPD///////8AAAAAAMD//////wMA8P8/AAD/////DwDA////AAD8////AAD/////AwD8//8/APD/////AwD///8DAP//////AwD///////////8/APD///////////8DAPz//////////w8AwP//////////PwAA////////////AAD8//////////8DAPD//////////w8AwP///////////wAA////////////DwDw////////////AwD/////////////AMD///////////8/APD//////////////////////////////////////////////////////////z8A8P///////////w8A/P///////////wMA/////////////wDA////////////PwDw/////////////////////wM=").key("nav_unknown"),
    Bitmap("//////////////////////8A/P///////////wAAwP//////////AwAAAP////////8PAAAAAPz///////8AAP8DAPz//////w8A/P8PAPz//////wPw//8/AP//////PwD///8/AP//////D8D///8PwP//////APz///8PwP////8/AP////8D8P////8P8P////8D/P////8A/P////8A/P///z8A/////z8A/////w/A/////w/A/////wPw/////wPw/////wD8/////wD8////PwD/////PwD/////D8D/////D8D//w8AAAAA/P//A/D//w8AAADA////APz//w8AAAD8//8/AP///w8AAMD///8PwP///w8AAPz///8D8P///w8AwP////8A/P///w8A/P///z8A/////w/A/////w/A/////w/8/////wPw/////8///////wD8/////////////////wM=").key("nav_turnaround"),
    Bitmap("////////////////AAAAAP////////8/AAAA8P////////8PAADA//////////8DAAD8//////////8AAMD//////////z8AAAD//////////w8AAAD8/////////wPAAwDw/////////wD/DwDA////////P/D/PwAA////////z////wAA/P///////////wMA/P///////////w8A/P///////////w8A/////////////w/A/////////////wPw/////////////wD8////////////PwD/////////////D8D/////////////A/D/////////////APz///////////8/AP////////////8PwP////////////8D8P////////////8A/P///////////z8A/////////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_left_1"),
    Bitmap("//////////////////8////////////////A/////////////wPw////////////DwD8//////////8/AAD///////////8AAAAAwP///////w8AAAAAAPz//////w8AAAAAAPz//////z8AAP8/AP////////8AwP8/AP////////8D8P8PwP////////8P/P8D8P////////8///8A/P///////////z8A/////////////w/A/////////////wPw/////////////wD8////////////PwD/////////////D8D/////////////A/D/////////////APz///////////8/AP////////////8PwP////////////8D8P////////////8A/P///////////z8A/////////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_left_2"),
    Bitmap("/////////////////////////////////////////////////////////////////////wD8////////////DwD8//////////8/AAD//////8////8AAMD//////wP//wMAAPD//////wD/DwDAAPz/////PwA8AAA/AP//////DwAAAPwPwP//////AwAA8P8D8P//////AADA//8A/P////8/AADA/z8A//////8PAADA/w/A//////8DAAAA/wPw//////8AAAAA/wD8////////////PwD/////////////D8D/////////////A/D/////////////APz///////////8/AP////////////8PwP////////////8D8P////////////8A/P///////////z8A/////////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_left_3"),
    Bitmap("/////////////////////////wMAAAD8/////////wMAAAD//////////w8AAMD//////////w8AAPD//////////w8AAPz/////////PwAAAP//////////AAAAwP////////8DAPAA8P///////w8AwP8D/P//////PwAA//8D////////AAD8///P//////8PAPD///////////8AwP///////////z8A/P///////////w/A/////////////wPw/////////////wD8////////////PwD/////////////D8D/////////////A/D/////////////APz///////////8/AP////////////8PwP////////////8D8P////////////8A/P///////////z8A/////////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_right_1"),
    Bitmap("///////////////////////////z///////////////A/////////////z8A/////////////w8A/P///////////wMA8P////////8AAAAAwP///////wAAAAAAwP//////DwAAAAAA/P//////A/D/AwDw//////8/AP//AMD///////8PwP8/AP////////8D8P8P/P////////8A/P/z/////////z8A/////////////w/A/////////////wPw/////////////wD8////////////PwD/////////////D8D/////////////A/D/////////////APz///////////8/AP////////////8PwP////////////8D8P////////////8A/P///////////z8A/////////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_right_2"),
    Bitmap("/////////////////////////////////////////////////////////////////////w/A/////////////wDA////////////PwAA////////////DwAA/P//z///////AwAA8P8/8P//////AAwAwP8D/P////8/AD8AAA8A//////8PwP8AAADA//////8D8P8DAADw//////8A/P8PAAD8/////z8A//8AAAD//////w/A/w8AAMD//////wPwPwAAAPD//////wD8AwAAAPz/////PwD/////////////D8D/////////////A/D/////////////APz///////////8/AP////////////8PwP////////////8D8P////////////8A/P///////////z8A/////////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_right_3"),
    Bitmap("////////////////AAAAAPP/z/8///88AAAA8Pz/8//P/z8PAADAP////P/z/88DAAD8z/8////8//MAAMD/8//P/z///zwAAAD//P//////Pw8AAAD8////////zwPAAwDw////////8wD/DwDA/8//P///PPD/PwAA//P/z/8/z////wAA/Pz/8//P/////wMAPP///P/z/////w8AzP8////8/////w8A/////z//////Pw/A/////8//////zwPw//////P/////8wDM/z////z/////PADz/8//P/////8/D8D8//P/z//////PAzD///z/8//////zAMz/P////P////88AP////8//////z8PwP/////P/////88D8P/////z//////MAzP8////8/////zwA8//P/z//////Pw/A/P/z/8//////zwMw///8//P/////8wDM/z////z//////////////wM=").key("nav_exit_left"),
    Bitmap("/////////////////P/z/8//PwMAAAA8///8//P/zwMAAADP/z////z/8w8AAMDz/8//P////A8AAPD8//P/z/8//w8AADz////////PPwAAAM//////////AAAAwPP///////8DAPAA8Pz/8//P/w8AwP8DPP///P/zPwAA//8Dz/8////8AAD8///P8//P/z8PAPD//////P/z/88AwP////8//////z8A/P/////P/////w/A8//////z/////wPw/P/////8//P/zwA8/////z////z/MwDP/////8//P///DMDz//////P/z/8/A/D8//////z/8//PADz/////P/////8/AM//////z/////8PwPP/////8/////8D8Pz//////P/z/88APP////8////8/zMAz//////P/z///wzA8//////z/8//PwPw/P/////8//P/zwA8/////////////////////wM=").key("nav_exit_right"),
    Bitmap("/////////////////P/z///PP/////8////8/z8Az//////P/z///wMA8//////z/8//PwAA/P/////8//P/AAAA/P///z////8PAAAA/P///8////8AAAAA/P////P//wMAAAAA8P////z/MwAAAAAA8P//P////P8zAM//////z/8///8MwPP/////8//P/z8D8Pz//////P/z/88AMP////8///////8AwP/////P/////z8A8P/////z//////8AwP/////8//P/z/8AwP///z////z/8/8DAP///8//P////P8DAP////P/z/8///8MAPz///z/8//P/z8/APz/P////////88/APD/z/////////P/APD/8/////////z/AMD//P/z/8//P///A8A////8//P/z///AwDP/z////z/8///DwDz/8//P////P//P8D8//P/z/8/////P/D//////////////wA=").key("nav_merge_left"),
    Bitmap("//////////////////////PP//8////8/////zwA///P/z//////PwMA///z/8//////DwAA///8//P/////AAAA/D////z///8PAAAA/P//P/////8AAAAA/P//z////wMAAAAA8P//8///PwAAAAAAMP///P////88APP/z/8//////z8PwPz/8//P/////88DMP///P/z/////zMAzP8////8/////wDA/////z//////PwDw/////8//////AMD///////P///8PAPzP/z////z//z8A8P/z/8//P////wMA///8//P/z///DwDM/z////z/8///APDz/8//P////P8DAP/8//////8//z8A/D/////////P/wDA/8/////////zDwD///P/z/8///88APD///z/8//P/z8DwP//P////P/z/88A////z/8////8/zPw////8//P/z///8z//////////////wM=").key("nav_merge_right"),
    Bitmap("///////////////////////////////////////A/////////////z8A/P///////////w8AwP///////////wMAAPz//////////wAAAPD/////////PwAAAAD/////////DwAAAADw////////AwAAAADA////////AAAAAAD///////8/AAAAAP////////8PAAAA//////////8DAAD8//////////8AAPz//////////z8A/P///////////w/8/////////////wP//////////////8D/////////////P/D///////8/AAAAAAAAAAD////P////A////8/////zAAAAAAAAwPP///88//8/8P//8/z//z/P//8P/P//PP///8/z//////8/z/////MAAAAAAADA8/////z//////////P//PwAAAAAAAAAA/////////////////////////////////wM=").key("nav_finish"),
    Bitmap("/////////////////P/////P///z//88AwAAAPzz///8/z/PAAAAwP/8/z///88zAAAA/z///8////MMAADw/8////P//zwDAAD/////////P88AAAD/////////zzMAAAD8////////8wwAAwD8z///8///PAP8DwD88////P8/z8D/DwDw/P8////PM///PAAw///P///z/P8/PwDP///z//88//////z//////z/P/////////////88zAAAAwP////////MMAAAA/M////P//zwDAADw//P///z/P88AAAD///z/P///zzMAAPD/P///z///8wwAAPD/z///8///PAMAAMD///////8/zwAwAMD////////PM8D/AMD////////zDPz/AADP///z//888//PAwDz///8/z/P///zA/D8/z///8/z///8zz///8////P8/z///8////P///z//////////////wM=").key("nav_stay_left"),
    Bitmap("/////////////////P8////P//////88///P///zDwAAADDP///z///8DwAAAMzz///8/z//PwAAAPP8/z///8//PwAAwDz/////////PwAAMM//////////AwAAzPP///////8PAAAA8/z/P///z/8AAAPAPP//z///8w8A/A8wz///8///PADA/w/M8////P8/AwDP/z/z/P8////PA/Dz//88////////z////z/P/////////////8/z////////AAAAAPP8/z///8//AAAAwDz//8////P/AwAAMM////P///z/AwAAzPP///z/P///AwAA8/z/P///z/8/AADAPP////////8AAAAwz////////w8AMADM8////////wDA/wDz/P8////PAwD8/8A8///P//8zAPD8/zPP///z//88AD///8/z///8/z///M////P8/z///8////P///z//////////////wM=").key("nav_stay_right"),
    Bitmap("///////////////////////P/////////////z8A/////////////wMA////////////DwAA/P//////////AAAA/P////////8DAAAA8P///////z8AAAAA8P///////wAAAAAAwP//////DwAAAAAAwP////8/AAAAAAAAAP////8DAAAAAAAAAP////////8D8P////////////8A/P///////////z8A/////////////w/A/////////////wPw/////////////wD8////////////PwD/////////////D8D/////////////A/D/////////////APz///////////8/AP////////////8PwP////////////8D8P////////////8A/P///////////z8A/////////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_straight"),
    Bitmap("//////////////////////////////////8PAAAAAAD8/////w8AAAAAAADA////PwAAAAAAAAAA////AwD//////wMA//8/APz//////w8A//8PwP///////w/A//8A/P///////w/A/z8A/////////wPw/w/A/////////wD8/wD8/////////wD8PwD/////////PwD/D8D///8A/P//D8D/A/D//w8A/P//A/D/APz//wAA/P//APw/AP///wDA//8/AP8PwP///wD8//8PwP8D8P////////8D8P8A/P////////8A/P8A/P///////w/A/z8A/////////wPw/w/A/////////wD8/w/A////////D8D//wPA////////APD//wMA//////8DAP///wMAAAAAAAAA8P///w8AAAAAAADA//////8AAAAAAMD//////////////////////////////////wM=").key("nav_waypoint"),
    Bitmap("////////////////////////////////////DwAAAPz/////P/8PAAAAAMD/////Dz8A8P//AwD/////AwDw////PwD8////AMD//////wD8//8/APz//////wP8//8PAPz//////wP8//8DAPz//////wP8/////////////wP8/////////////wP//8D//////////8D/D/D/////////P8D/A///////////P/D/wP//////////D/w/8P//////////A/8P8P////////8/wP8P/P////////8P/P8D//////////////8A//////////////8A////////AAD///8A////////AMD///8A////////APD///8A/P////8PAPz///8A8P///z8AAP////8DAP//PwDww/////8PAAAAAMD/8///////AAAAwP///////////////////////////////////wM=").key("nav_roundabout")
  ).map(_.frame(w = 47, h = 31).hidden)
}
