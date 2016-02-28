/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon.display

import com.wahoofitness.common.{display => w}

object Pages {
  import w.DisplayAlignment._
  import w.DisplayButtonPosition._
  import w.DisplayFont._

  private def clockAndRecStatus: Group = {
    val icon = Icons.clock.frame(x = 1, y = 1)
    val recStopped = Icons.recStopped.frame(x = 1, y = 13)
    val recPaused = Icons.recPaused.frame(x = 1, y = 13).hidden
    val value = Text("--:--:--").frame(x = 20, y = 2, w = 108, h = 0)
      .font(SYSTEM19).align(CENTER).key("value")
    Group(icon, recStopped, recPaused, value).key("CLOCK").frame(w = 128, h = 26)
  }

  private def unitGroup(key: String, icon: Bitmap, unit: String): Group = {
    icon.frame(x = 3, y = 3)
    val units = Text(unit).frame(x = 0, y = 3, w = 61, h = 0)
      .constant.font(SYSTEM10).align(RIGHT).key("units")
    val value = Text("--").frame(x = 0, y = 18, w = 64, h = 0)
      .font(SYSTEM26).align(CENTER).key("value")
    Group(icon, units, value).key(key).frame(w = 64, h = 51)
  }

  private def speedCurrent = unitGroup("SPEED_CURRENT", Icons.speed, "KPH")
  private def distanceWorkout = unitGroup("DISTANCE_WORKOUT", Icons.distance, "KM")
  private def cadenceCurrent = unitGroup("BIKE_CAD_CURRENT", Icons.cadence, "RPM")
  private def heartRateCurrent = unitGroup("HR_CURRENT", Icons.heartRate, "BPM")

  private lazy val overview = {
    val top = clockAndRecStatus
    val northWest = speedCurrent
    val northEast = distanceWorkout
    val southWest = cadenceCurrent
    val southEast = heartRateCurrent
    Page(
      top      .frame( 0,  0, 128, 26),
      Rect()   .frame( 0, 26, 128,  0),
      northWest.frame( 0, 26,  64, 51),
      Rect()   .frame(64, 26,   0, 51),
      northEast.frame(64, 26,  64, 51),
      Rect()   .frame( 0, 77, 128,  0),
      southWest.frame( 0, 77,  64, 51),
      Rect()   .frame(64, 77,   0, 51),
      southEast.frame(64, 77,  64, 51)
    ).key("OVERVIEW")
  }

  private def navClock: Group = {
    val rect = Rect().frame(w = 128, h = 22)
    val value = Text("--:--:--").frame(w = 128, h = 0)
      .font(SYSTEM19).align(CENTER).key("value")
    Group(rect, value).key("CLOCK").frame(w = 128, h = 22)
  }

  private def navAction: Group = {
    val rect = Rect().frame(w = 49, h = 33)
    val icons = Icons.navigation.map(_.inside(rect))
    Group((rect :: icons): _*).frame(w = 49, h = 33)
  }

  private def navDist: Group = {
    val rect = Rect().frame(w = 80, h = 33)
    val units = Text("km").frame(x = 58, y = 21, w = 20, h = 0)
      .constant.font(SYSTEM10).align(RIGHT).key("units")
    val value = Text("--").frame(x = 0, y = 8, w = 58, h = 0)
      .font(SYSTEM19).align(RIGHT).key("value")
    Group(rect, units, value).frame(w = 80, h = 33)
  }

  private def navName: Group = {
    val rect = Rect().frame(w = 128, h = 22)
    val value = Text("--").frame(w = 128, h = 0)
      .font(SYSTEM19).align(CENTER).key("value")
    Group(rect, value).frame(w = 128, h = 22)
  }

  private lazy val navigation = {
    Page(
      navClock.frame(0, 0, 128, 22),

      navAction.key("NAV1_ACTION").frame(0, 21, 49, 33),
      navDist.key("NAV1_DIST").frame(48, 21, 80, 33),
      navName.key("NAV1_NAME").frame(0, 53, 128, 22),

      navAction.key("NAV2_ACTION").frame(0, 74, 49, 33),
      navDist.key("NAV2_DIST").frame(48, 74, 80, 33),
      navName.key("NAV2_NAME").frame(0, 106, 128, 22)
    ).key("NAVIGATION")
  }

  type ButtonConf = Seq[(w.DisplayButtonPosition, String)]

  def conf(buttons: ButtonConf): Configuration = {
    val c = Configuration(overview, navigation)
      .id("LocusRflktAddon")
      .name("LocusRflktAddon")
    buttons.foreach((c.button _).tupled)
    c
  }
}

object Icons {
  def clock = Bitmap("/w8A//8PAAD/P8A/wP/ADz/wA//wDzz8D//Dw/8APzzw/8DAD/z/A/8D/AP8/wAA8P//APD/").frame(w = 18, h = 12).key("icon")
  def speed = Bitmap("////////A/z//wD/8//D/w//D/8/PD/8/8DD8P8D/wD/D/wP8D/A/wD/D/8P////////////").frame(w = 18, h = 12).key("icon")
  def distance = Bitmap("////8P///wD///8A/P//AMD//wMA//8DAPD/DwDA////A/z////w////D//////8////////").frame(w = 18, h = 12).key("icon")
  def cadence = Bitmap("//8A////8A///8P/8/8///z8//PDz/8/D/z8/zPw8///ww///w8D/////P//PwD8////////").frame(w = 18, h = 12).key("icon")
  def heartRate = Bitmap("P8AP8P8AMAD8AwAAAD8AAADwAwAAAP8AAAD8PwAA8P8PAMD//wMA////APz//z/w////z///").frame(w = 18, h = 12).key("icon")
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
    Bitmap("//////////////////////8A/P///////////z8A/////////////w/A////////////DwAA/P////////8DAAAAAP///////w8A8P8DAPz/////PwD8////APD/////A/D/D/z/A/D///8/wP//AP//D/D///8D/P8PwP//D/D//z/A//8A8P//D/D//w/8/w8M/P//D/z//8D///8D////D/w/APD////A////AwAPAPz//z/w////A8ADAP///w/8//8/APD/wP///wP///8P/P//wP///8D////A//8/wP//P/D//w/w//8/wP//D/z//wD///8/wP8DAAD/D/D///8/AP////8/AP////8/APz///8A8P//////AAD/PwDA////////AwAAAAD//////////wAAwP///////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_roundabout_1"),
    Bitmap("//////////////////////8A/P///////////z8A/////////////w/A////////////DwAA/P////////8DAAAAAP///////w8A8P8DAPz/////PwD8////APD/////A/D/////A/D///8/wP//APz/D/D///8D/P8PAPz/D/D//z/A///AD/z/D/D//w/8/w/8D/z/D/z//8D/////A///D/w/APD///8/8P//AwAPAPz///8D////A8ADAP///w/w//8/APD/wP//P8D///8P/P//wP//A/z////A//8/wP8/8P///w/w//8/wP8P/P///wD///8/wP8DAAD/D/D///8/AP////8/AP////8/APz///8A8P//////AAD/PwDA////////AwAAAAD//////////wAAwP///////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_roundabout_2"),
    Bitmap("//////////////////////8A/P///////////z8A/////////////w/A////////////DwAA/P////////8DAAAAAP///////w8A8P8DAPz/////PwD8////APD/////A/D/////A/D///8/wP//APz/D/D///8D/P8DDPD/D/D//z/A/z/AD/D/D/D//w/8/w/8D/z/D/z//8D/////AP//D/w/APD///8P8P//AwAPAPz//z/A////A8ADAP////8A//8/APD/wP////8A//8P/P//wP//wP/A///A//8/wP8/wA/w/w/w//8/wP8/wAD//wD///8/wP//APz/D/D///8/AP////8/AP////8/APz///8A8P//////AAD/PwDA////////AwAAAAD//////////wAAwP///////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_roundabout_3"),
    Bitmap("//////////////////////8A/P///////////z8A/////////////w/A////////////DwAA/P////////8DAAAAAP///////w8A8P8DAPz/////PwD8////APD/////A/D/////A/D///8/wP///8D/D/D///8D/P//D/D/D/D//z/A////APz/D/D//w/8//8PAP//D/z//8D////AwP//D/w/APD//w888P//AwAPAPz//8AP/P//A8ADAP//D/wD//8/APD/wP//AAAA/P8P/P//wP///z/w///A//8/wP///w/8/w/w//8/wP///wP//wD///8/wP///8D/D/D///8/AP////8/AP////8/APz///8A8P//////AAD/PwDA////////AwAAAAD//////////wAAwP///////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_roundabout_4"),
    Bitmap("//////////////////////8A/P///////////z8A/////////////w/A////////////DwAA/P////////8DAAAAAP///////w8A8P8DAPz/////PwD8////APD/////A/D/////A/D///8/wP8DAAD/D/D///8D/P/A////D/D//z/A/z/w////D/D//w/8/w/8////D/z//8D//wP/////D/w/APD//wAA/P//AwAPAPz/P8AP/P//A8ADAP//D/wP/P8/APD/wP////8D//8P/P//wP/////A///A//8/wP8/8D/w/w/w//8/wP8/8AP//wD///8/wP8/APD/D/D///8/AP////8/AP////8/APz///8A8P//////AAD/PwDA////////AwAAAAD//////////wAAwP///////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_roundabout_5"),
    Bitmap("//////////////////////8A/P///////////z8A/////////////w/A////////////DwAA/P////////8DAAAAAP///////w8A8P8DAPz/////PwD8////APD/////A/D/A/D/A/D///8/wP8/APD/D/D///8D/P8DP/D/D/D//z/A/z/wP/D/D/D//w/8/w/8////D/z//8D//wP/////D/w/APD//wAA////AwAPAPz/PwAA////A8ADAP//D8AA//8/APD/wP//A/wA//8P/P//wP//wP/A///A//8/wP8/wA/w/w/w//8/wP8/wAD//wD///8/wP8/APD/D/D///8/AP8/AP8/AP////8/APz///8A8P//////AAD/PwDA////////AwAAAAD//////////wAAwP///////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_roundabout_6"),
    Bitmap("//////////////////////8A/P///////////z8A/////////////w/A////////////DwAA/P////////8DAAAAAP///////w8A8P8DAPz/////PwD8////APD/////A/D/////A/D///8/wP8DAAD/D/D///8D/P8AAMD/D/D//z/A////P/D/D/D//w/8////A///D/z//8D/////wP//D/w/APD///8P/P//AwAPAPz///8D////A8ADAP///z/w//8/APD/wP///w/8//8P/P//wP///8D////A//8/wP//P/D//w/w//8/wP//A////wD///8/wP//wP//D/D///8/AP8/8P8/AP////8/APz///8A8P//////AAD/PwDA////////AwAAAAD//////////wAAwP///////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_roundabout_7"),
    Bitmap("//////////////////////8A/P///////////z8A/////////////w/A////////////DwAA/P////////8DAAAAAP///////w8A8P8DAPz/////PwD8////APD/////A/D/A/D/A/D///8/wP8PAMD/D/D///8D/P8AP8D/D/D//z/A/z/wP/D/D/D//w/8/w/8D/z/D/z//8D//wP8AP//D/w/APD//wMA8P//AwAPAPz//w/A////A8ADAP//PwAA//8/APD/wP//A/wA//8P/P//wP//wP/A///A//8/wP8/8D/w/w/w//8/wP8P8AP8/wD///8/wP8PAMD/D/D///8/AP8/AP8/AP////8/APz///8A8P//////AAD/PwDA////////AwAAAAD//////////wAAwP///////////w/A/////////////wPw/////////////wD8/////////////////////wM=").key("nav_roundabout_8")
  ).map(_.frame(w = 47, h = 31).hidden)
}
