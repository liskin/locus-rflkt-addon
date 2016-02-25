/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon.display

import collection.JavaConverters._

import com.wahoofitness.common.{display => w}
import org.json.{JSONObject, JSONArray}

trait DisplayObject[T] extends w.DisplayObject
{ this: T =>
  private type Self = T with DisplayObject[T]

  def key(k: String): Self = { setKey(k); this }
}

trait DisplayElement[T] extends w.DisplayElement with DisplayObject[T]
{ this: T =>
  import DisplayElement._

  private type Self = T with DisplayElement[T]

  private var (fx, fy, fw, fh) = (0, 0, 128, 128)
  frame()

  def frame(x: Int = fx, y: Int = fy, w: Int = fw, h: Int = fh): Self = {
    fx = x; fy = y; fw = w; fh = h
    setFrame(Seq[Integer](x, y, w, h).asJava)
    this
  }

  def hidden(): Self = { setVisible(false); this }

  private var pos: Option[Position] = None
  private def setPos(p: Position): Self = {
    pos = pos orElse Some(p)
    this
  }

  def topLevel(): Self = setPos(TopLevel)
  def inside[U](what: DisplayElement[U]): Self = setPos(Inside(what))
  def below[U](what: DisplayElement[U]): Self = setPos(Below(what))
  def rightOf[U](what: DisplayElement[U]): Self = setPos(RightOf(what))

  private[display] def computeFrame() {
    pos.get match {
      case TopLevel =>
      case Inside(what) =>
        val Seq(wx, wy, ww, wh) = what.getFrame.asScala
        val Seq(x, y, w, h) = getFrame.asScala
        if (what.isInstanceOf[Rect])
          frame(wx + x + 1, wy + y + 1, (ww - x - 2) min w, (wh - y - 2) min h)
        else
          frame(wx + x, wy + y, (ww - x) min w, (wh - y) min h)
      case Below(what) =>
        val Seq(wx, wy, ww, wh) = what.getFrame.asScala
        val Seq(x, y, w, h) = getFrame.asScala
        frame(wx + x, wy + wh + y, w, h)
      case RightOf(what) =>
        val Seq(wx, wy, ww, wh) = what.getFrame.asScala
        val Seq(x, y, w, h) = getFrame.asScala
        frame(wx + ww + x, wy + y, w, h)
    }
  }
}

trait DisplayButtonCfg[T] { this: T =>
  private type Self = T with DisplayButtonCfg[T]

  def getButtonCfg: w.DisplayButtonCfg

  def button(pos: w.DisplayButtonPosition, fun: String): T = {
    getButtonCfg.setButtonFunction(pos, fun)
    this
  }
}

object DisplayElement {
  private sealed abstract class Position
  private object TopLevel extends Position
  private case class Inside[T](what: DisplayElement[T]) extends Position
  private case class Below[T](what: DisplayElement[T]) extends Position
  private case class RightOf[T](what: DisplayElement[T]) extends Position
}

case class Configuration(private val pages: Page*)
  extends w.DisplayConfiguration with DisplayButtonCfg[Configuration]
{
  populateFromJson(
    new JSONObject(Map(
      "pages" -> new JSONArray(pages.map(_.toJson).asJava)
    ).asJava)
  )

  private type Self = Configuration

  def name(n: String): Self = { setName(n); this }
  def id(i: String): Self = { setId(i); this }
  def custom(k: String, v: String): Self = { setCustom(k, v); this }
}

case class Page(private val elements: DisplayElement[_]*)
  extends w.DisplayPage with DisplayButtonCfg[Page]
  with DisplayObject[Page]
{
  elements.foreach(_.topLevel().computeFrame())

  populateFromJson(
    new JSONObject(Map(
      "elements" -> new JSONArray(elements.map(_.toJson).asJava)
    ).asJava)
  )

  private type Self = Page

  def hidden(): Self = { setHidden(true); this }
  def comment(c: String): Self = { setComment(c); this }
  def custom(k: String, v: String): Self = { setCustom(k, v); this }
}

case class Group(private val elements: DisplayElement[_]*)
  extends w.DisplayElementGroup with DisplayElement[Group]
{
  getElements.addAll(elements.asJava)

  override private[display] def computeFrame() {
    super.computeFrame()
    elements.foreach(_.inside(this).computeFrame())
  }
}

case class Bitmap(private val value: String)
  extends w.DisplayElementBitmap with DisplayElement[Bitmap]
{
  // FIXME: setTransparentColor
  setValue(value)
}

case class Rect()
  extends w.DisplayElementRect with DisplayElement[Rect]

case class Text(private val value: String)
  extends w.DisplayElementString with DisplayElement[Text]
{
  populateFromJson(new JSONObject()) // NPE otherwise :-(
  setValue(value)

  private type Self = Text

  def font(f: w.DisplayFont): Self = { setFont(f); this }
  def align(a: w.DisplayAlignment): Self = { setAlign(a); this }
  def constant(): Self = { setConstant(true); this }
  def bold(): Self = { setBold(true); this }
}
