package app.sanad.core

import java.io.File
import kotlin.test.Test

/** يولّد build/pose-sheet.svg: كل إطارات كل تمرين، للمراجعة البصرية. */
class PoseSheet {
    private fun limb(f: Pose, a: Joint, b: Joint, w: Int, c: String) =
        """<line x1="${f.x(a)}" y1="${f.y(a)}" x2="${f.x(b)}" y2="${f.y(b)}" stroke="$c" stroke-width="$w" stroke-linecap="round"/>"""

    private fun figure(f: Pose): String {
        val far = "#8b97b3"; val near = "#14213d"
        return buildString {
            append("""<line x1="0" y1="94.5" x2="100" y2="94.5" stroke="#d7dde8"/>""")
            append(limb(f, Joint.HIP, Joint.KNEE_F, 7, far)); append(limb(f, Joint.KNEE_F, Joint.FOOT_F, 6, far))
            append(limb(f, Joint.NECK, Joint.ELBOW_F, 6, far)); append(limb(f, Joint.ELBOW_F, Joint.HAND_F, 5, far))
            append("""<path d="M${f.x(Joint.NECK)} ${f.y(Joint.NECK)} Q${f.x(Joint.MID)} ${f.y(Joint.MID)} ${f.x(Joint.HIP)} ${f.y(Joint.HIP)}" stroke="#a83a2f" stroke-width="11" fill="none" stroke-linecap="round"/>""")
            append(limb(f, Joint.HIP, Joint.KNEE_N, 8, near)); append(limb(f, Joint.KNEE_N, Joint.FOOT_N, 7, near))
            append(limb(f, Joint.NECK, Joint.ELBOW_N, 7, near)); append(limb(f, Joint.ELBOW_N, Joint.HAND_N, 6, near))
            append("""<circle cx="${f.x(Joint.HEAD)}" cy="${f.y(Joint.HEAD)}" r="6.5" fill="#d9962e"/>""")
        }
    }

    @Test fun writeSheet() {
        val cell = 110
        val rows = EXERCISES.flatMap { e -> e.frames.mapIndexed { i, f -> Triple(e, i, f) } }
        val cols = 6
        val h = ((rows.size + cols - 1) / cols) * (cell + 16)
        val svg = buildString {
            append("""<svg xmlns="http://www.w3.org/2000/svg" width="${cols * cell}" height="$h" style="background:#fff;font-family:sans-serif">""")
            rows.forEachIndexed { k, (e, i, f) ->
                val x = (k % cols) * cell; val y = (k / cols) * (cell + 16)
                append("""<g transform="translate($x ${y + 14})"><rect width="${cell - 6}" height="${cell - 6}" fill="#f1f3f7" rx="8"/><g transform="translate(2 2)">${figure(f)}</g></g>""")
                append("""<text x="${x + 4}" y="${y + 11}" font-size="10">${e.id} #$i</text>""")
            }
            append("</svg>")
        }
        File("build/pose-sheet.svg").writeText(svg)
    }
}
