/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

import scala.collection.mutable.ListBuffer

import android.content.{Context, SharedPreferences}
import android.preference._
import android.support.v7.app.AppCompatActivity
import android.widget.{LinearLayout, TextView}

import Log._

import display.Pages.{ConfPage, ConfPageNav, ConfPage2x2, Conf2x2}

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
    setPreferenceScreen(root)
    ButtonSettings.addToGroup(this, root); ()
    PageSettings.addToGroup(this, root); ()
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

object PageSettings extends SettingCategory with SettingValue[Seq[ConfPage]] {
  lazy val title = "RFLKT pages"

  lazy val pages = (1 to 4).map(new SettingPage(_))

  lazy val showNavPage =
    SwitchPref("navigationPage.enabled", "Navigation page",
      "(loading pages faster if disabled)", true)

  override def addPreferences(pf: PreferenceFragment,
      group: PreferenceGroup): Seq[Preference] =
    super.addPreferences(pf, group) ++
    pages.map(_.addToGroup(pf, group)) :+
    showNavPage.addToGroup(pf, group)

  def getValue(pref: SharedPreferences): Seq[ConfPage] = {
    var confPages = ListBuffer.empty[ConfPage]
    confPages ++= pages.map(_.getValue(pref)).flatten
    if (showNavPage.getValue(pref))
      confPages += new ConfPageNav("NAVIGATION")
    confPages
  }
}

class SettingPage(number: Int) extends SettingScreen with SettingValue[Option[ConfPage]] {
  lazy val title = s"Page $number"

  lazy val enabled =
    if (number == 1)
      ConstPref(true)
    else
      SwitchPref(s"pages.$number.enabled", "Enabled", null, false)

  lazy val widgets2x2 = new SettingPage2x2(number)

  override def addPreferences(pf: PreferenceFragment, group: PreferenceGroup): Seq[Preference] = {
    val sup = super.addPreferences(pf, group)
    val switch = enabled.addToGroup(pf, group)
    val widgets = widgets2x2.addToGroup(pf, group)

    if (switch == null) { // first page
      sup :+ widgets
    } else {
      switch.setDisableDependentsState(false)
      widgets.setDependency(switch.getKey())
      sup :+ switch :+ widgets
    }
  }

  override def getValue(pref: SharedPreferences) =
    if (enabled.getValue(pref))
      Some(widgets2x2.getValue(pref))
    else
      None
}

class SettingPage2x2(number: Int) extends SettingPageWidgets(number)
  with SettingNorth with Setting2x2
{
  import display.Const.{Widget => W}

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

  override def getValue(pref: SharedPreferences) =
    new ConfPage2x2(
      s"PAGE$number",
      north.getValue(pref),
      super.getValue(pref))
}

trait SettingNorth extends SettingPageWidgets {
  import display.Const.{Widget => W}

  lazy val northEntries = Seq(
    "Clock" -> W.clock,
    "Time – total (workout)" -> W.timeWorkout,
    "Time – moving (workout)" -> W.timeMovingWorkout
  )
  lazy val northDef = W.clock
  lazy val north =
    ListPref(s"$prefix.north", "top", northEntries, northDef)

  override def addPreferences(pf: PreferenceFragment, group: PreferenceGroup): Seq[Preference] =
    super.addPreferences(pf, group) :+
    north.addToGroup(pf, group)
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

  override def addPreferences(pf: PreferenceFragment,
      group: PreferenceGroup): Seq[Preference] =
    super.addPreferences(pf, group) ++
    Seq(
      northWest.addToGroup(pf, group),
      northEast.addToGroup(pf, group),
      southWest.addToGroup(pf, group),
      southEast.addToGroup(pf, group)
    )

  def getValue(pref: SharedPreferences) =
    new Conf2x2(
      northWest.getValue(pref),
      northEast.getValue(pref),
      southWest.getValue(pref),
      southEast.getValue(pref)
    )
}

abstract class SettingPageWidgets(number: Int) extends SettingCategory {
  lazy val prefix = s"pages.$number.widgets"
  lazy val title = "Widgets"
}

abstract class SettingCategory extends SettingGroup {
  def createGroup(pf: PreferenceFragment): PreferenceGroup = {
    val cat = new PreferenceCategory(pf.getActivity())
    cat.setTitle(title)
    cat
  }
}

abstract class SettingScreen extends SettingGroup {
  def createGroup(pf: PreferenceFragment): PreferenceGroup = {
    val screen = pf.getPreferenceManager().createPreferenceScreen(pf.getActivity())
    screen.setTitle(title)
    screen
  }
}

abstract class SettingGroup extends Setting {
  def title: String
  def createGroup(pf: PreferenceFragment): PreferenceGroup
  def addPreferences(pf: PreferenceFragment,
    group: PreferenceGroup): Seq[Preference] = Seq()

  def addToGroup(pf: PreferenceFragment, root: PreferenceGroup): PreferenceGroup = {
    val group = createGroup(pf)
    root.addPreference(group)
    addPreferences(pf, group)
    group
  }
}

case class ListPref(key: String, title: String,
    entries: Seq[(String, String)], default: String)
  extends SettingWidget[String, ListPreference]
{
  protected def preference(pf: PreferenceFragment): ListPreference =
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
  extends SettingWidget[Boolean, SwitchPreference]
{
  protected def preference(pf: PreferenceFragment): SwitchPreference =
    new SwitchPreference(pf.getActivity()) {
      setKey(key)
      setTitle(title)
      setSummary(summary)
      setDefaultValue(default)
    }

  protected def preferenceVar: PreferenceVar[Boolean] =
    Preferences.preferenceVar(key, default)
}

case class ConstPref[T](default: T) extends SettingWidget[T, Null] {
  protected def preference(pf: PreferenceFragment): Null = null
  protected def preferenceVar: PreferenceVar[T] =
    new PreferenceVar[T](null, default) {
      protected def get(value: T, pref: SharedPreferences): T = value
      protected def put(value: T, editor: SharedPreferences.Editor): Unit = ???
    }
}

trait SettingWidget[T, P <: Preference] extends SettingValue[T] {
  protected def preference(pf: PreferenceFragment): P
  protected def preferenceVar: PreferenceVar[T]

  def addToGroup(pf: PreferenceFragment, root: PreferenceGroup): P = {
    val widget = preference(pf)
    if (widget != null) root.addPreference(widget)
    widget
  }

  def getValue(pref: SharedPreferences): T = preferenceVar(pref)
}

trait SettingValue[T] extends Setting {
  def getValue(pref: SharedPreferences): T
}

abstract class Setting {
  def addToGroup(pf: PreferenceFragment, root: PreferenceGroup): Preference
}
