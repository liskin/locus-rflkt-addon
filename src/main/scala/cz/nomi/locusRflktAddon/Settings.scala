/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import android.content.{Context, SharedPreferences}
import android.preference._
import android.support.v7.app.AppCompatActivity
import android.widget.{LinearLayout, TextView}

import Log._

import display.Pages.{Conf2x2, ConfPage2x2}

class Settings extends AppCompatActivity
  with RActivity with BackToParentActivity
{
  import macroid.Tweak

  onCreate {
    logger.info("Settings: onCreate")

    setContentView {
      import scala.language.postfixOps
      import macroid.FullDsl._
      import macroid.contrib.LpTweaks.matchWidth

      getUi {
        l[LinearLayout](
          w[TextView] <~ text("(need reconnect to take effect)") <~ matchWidth <~ center <~ padding(all = 3 dp),
          f[SettingsFragment].framed(Gen.Id.settings, Gen.Tag.settings)
        ) <~ vertical
      }
    }
  }

  private def center: Tweak[TextView] = {
    import android.view.Gravity
    Tweak[TextView](_.setGravity(Gravity.CENTER_HORIZONTAL))
  }
}

class SettingsFragment extends PreferenceFragment with RFragment {
  onCreate {
    val root = getPreferenceManager().createPreferenceScreen(getActivity())
    ButtonSettings.addToGroup(this, root)
    OverviewSettings.addToGroup(this, root)
    ShowNavPage.addToGroup(this, root)
    setPreferenceScreen(root)
  }
}

object ButtonSettings extends SettingCategory with Setting2x2 {
  lazy val prefix = "allPages.buttons"
  lazy val title = "RFLKT button functions"

  import display.Const.{Function => F}
  lazy val entries = Seq(
    "Previous page" -> F.pageLeft,
    "Next page" -> F.pageRight,
    "Start/pause track recording" -> F.startStopWorkout,
    "Backlight for 5 seconds" -> F.backlight
  )
  lazy val northWestDef = F.startStopWorkout
  lazy val northEastDef = F.backlight
  lazy val southWestDef = F.pageLeft
  lazy val southEastDef = F.pageRight
}

object OverviewSettings extends SettingPage2x2 {
  lazy val prefix = "pages.1.widgets"
  lazy val title = "Overview page widgets"

  import display.Const.{Widget => W}

  lazy val northEntries = Seq(
    "Clock" -> W.clock,
    "Time – total (workout)" -> W.timeWorkout,
    "Time – moving (workout)" -> W.timeMovingWorkout
  )
  lazy val northDef = W.clock

  lazy val entries = Seq(
    "Speed (current)" -> W.speedCurrent,
    "Average speed – total (workout)" -> W.averageSpeedWorkout,
    "Average speed – moving (workout)" -> W.averageMovingSpeedWorkout,
    "Max speed (workout)" -> W.maxSpeedWorkout,
    "Distance (workout)" -> W.distanceWorkout,
    "Cadence (current)" -> W.cadenceCurrent,
    "Heart rate (current)" -> W.heartRateCurrent
  )
  lazy val northWestDef = W.speedCurrent
  lazy val northEastDef = W.distanceWorkout
  lazy val southWestDef = W.cadenceCurrent
  lazy val southEastDef = W.heartRateCurrent
}

trait SettingPage2x2 extends SettingCategory with Setting2x2 {
  def northEntries: Seq[(String, String)]
  def northDef: String

  private lazy val north =
    ListPref(s"$prefix.north", "top", northEntries, northDef)

  override def addPreferences(pf: PreferenceFragment, group: PreferenceGroup) {
    north.addToGroup(pf, group)
    super.addPreferences(pf, group)
  }

  override def getValue(pref: SharedPreferences) =
    new ConfPage2x2(
      north.getValue(pref),
      super.getValue(pref))
}

trait Setting2x2 extends SettingGroup with SettingValue[Conf2x2] {
  def prefix: String

  def entries: Seq[(String, String)]
  def northWestDef: String
  def northEastDef: String
  def southWestDef: String
  def southEastDef: String

  private lazy val northWest =
    ListPref(s"$prefix.northWest", "top left", entries, northWestDef)
  private lazy val northEast =
    ListPref(s"$prefix.northEast", "top right", entries, northEastDef)
  private lazy val southWest =
    ListPref(s"$prefix.southWest", "bottom left", entries, southWestDef)
  private lazy val southEast =
    ListPref(s"$prefix.southEast", "bottom right", entries, southEastDef)

  def addPreferences(pf: PreferenceFragment, group: PreferenceGroup) {
    northWest.addToGroup(pf, group)
    northEast.addToGroup(pf, group)
    southWest.addToGroup(pf, group)
    southEast.addToGroup(pf, group)
  }

  def getValue(pref: SharedPreferences) =
    new Conf2x2(
      northWest.getValue(pref),
      northEast.getValue(pref),
      southWest.getValue(pref),
      southEast.getValue(pref)
    )
}

object ShowNavPage extends SettingCategory with SettingValue[Boolean] {
  def title = "Navigation page"

  private lazy val enable =
    SwitchPref("navigationPage.enabled", "Enable",
      "(loading pages faster if disabled)", true)

  def addPreferences(pf: PreferenceFragment, group: PreferenceGroup) {
    enable.addToGroup(pf, group)
  }

  def getValue(pref: SharedPreferences) =
    enable.getValue(pref)
}

abstract class SettingCategory extends SettingGroup {
  def createGroup(pf: PreferenceFragment): PreferenceGroup = {
    val cat = new PreferenceCategory(pf.getActivity())
    cat.setTitle(title)
    cat
  }
}

abstract class SettingGroup extends Setting {
  def title: String
  def createGroup(pf: PreferenceFragment): PreferenceGroup
  def addPreferences(pf: PreferenceFragment, group: PreferenceGroup): Unit

  def addToGroup(pf: PreferenceFragment, root: PreferenceGroup) {
    val group = createGroup(pf)
    root.addPreference(group)
    addPreferences(pf, group)
  }
}

case class ListPref(key: String, title: String,
    entries: Seq[(String, String)], default: String)
  extends SettingWidget[String]
{
  protected def preference(pf: PreferenceFragment): Preference =
    new ListPreference(pf.getActivity()) {
      setKey(key)
      setTitle(title)
      setSummary("%s")
      setEntries(entries.map(_._1: CharSequence).toArray)
      setEntryValues(entries.map(_._2: CharSequence).toArray)
      setDefaultValue(default)
    }

  protected def preferenceVar: PreferenceVar[String] =
    Preferences.preferenceVar(key, default)
}

case class SwitchPref(key: String, title: String, summary: String, default: Boolean)
  extends SettingWidget[Boolean]
{
  protected def preference(pf: PreferenceFragment): Preference =
    new SwitchPreference(pf.getActivity()) {
      setKey(key)
      setTitle(title)
      setSummary(summary)
      setDefaultValue(default)
    }

  protected def preferenceVar: PreferenceVar[Boolean] =
    Preferences.preferenceVar(key, default)
}

trait SettingWidget[T] extends SettingValue[T] {
  protected def preference(pf: PreferenceFragment): Preference
  protected def preferenceVar: PreferenceVar[T]

  def addToGroup(pf: PreferenceFragment, root: PreferenceGroup) {
    root.addPreference(preference(pf))
  }

  def getValue(pref: SharedPreferences): T = preferenceVar(pref)
}

trait SettingValue[T] extends Setting {
  def getValue(pref: SharedPreferences): T
}

abstract class Setting {
  def addToGroup(pf: PreferenceFragment, root: PreferenceGroup): Unit
}
