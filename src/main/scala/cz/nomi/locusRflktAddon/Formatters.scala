/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

package cz.nomi.locusRflktAddon

object Formatters {
  import RflktApi.Str

  private val timeFormat = new java.text.SimpleDateFormat("HH:mm:ss")

  def formatString(s: Option[String]): Str = Str(s.getOrElse("--"))

  def formatTime(t: java.util.Date): Str = Str(timeFormat.format(t))

  def formatDuration(totalSecondsOpt: Option[Long]): Str =
    formatString {
      totalSecondsOpt.map { totalSeconds =>
        val seconds = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val minutes = totalMinutes % 60
        val totalHours = totalMinutes / 60
        f"$totalHours%02d:$minutes%02d:$seconds%02d"
      }
    }

  def formatInt(i: Option[Int]): Str =
    formatString(i.map(v => f"$v%d"))

  def formatFloatRound(f: Option[Float]): Str =
    formatString(f.map(v => f"$v%.0f"))

  def formatDoubleRound(d: Option[Double]): Str =
    formatString(d.map(v => f"$v%.0f"))

  def formatFloatFixed(f: Option[Float]): Str =
    formatString(f.map(v => f"$v%.1f"))

  def formatDoubleFixed(d: Option[Double]): Str =
    formatString(d.map(v => f"$v%.1f"))

  def formatDouble(d: Option[Double]): Str =
    formatString {
      d.map { v =>
        if (v.abs > 99) {
          f"$v%.0f"
        } else if (v.abs > 9) {
          f"$v%.1f"
        } else {
          f"$v%.2f"
        }
      }
    }

  def normalizeString(s: String): String = {
    import java.text.Normalizer
    val split = Normalizer.normalize(s, Normalizer.Form.NFD)
    "\\p{M}".r.replaceAllIn(split, "")
  }
}
