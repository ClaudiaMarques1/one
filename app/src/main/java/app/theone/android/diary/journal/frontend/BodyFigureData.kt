package app.theone.android.diary.journal.frontend

/**
 * Source geometry for the sore-body-map figure (see design_handoff_diary_journal's "Body map
 * figure" section). A 200x400 coordinate space, left half only - [BodyFigure] draws each shape
 * twice (as-is, then mirrored around x = 100) and mirrors tapped points the same way for hit
 * testing, so only one copy of this data is needed.
 */
internal sealed class FigureShape {
    data class PathData(val d: String) : FigureShape()

    data class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : FigureShape()

    data class RoundRect(val x: Float, val y: Float, val w: Float, val h: Float, val r: Float) : FigureShape()
}

internal data class BodyPart(val id: String, val shape: FigureShape)

internal data class FigureSide(val neutral: List<FigureShape>, val parts: List<BodyPart>)

internal object BodyFigureData {
    val head = FigureShape.Ellipse(cx = 100f, cy = 30f, rx = 18f, ry = 22f)

    private val delt = FigureShape.PathData(
        "M66 74C52 74 40 82 38 98C37 108 40 116 44 120C50 112 58 104 66 100C70 92 72 82 66 74Z"
    )
    private val upperArm = FigureShape.PathData(
        "M44 122C40 130 36 140 35 152C34 160 38 166 44 166C50 164 54 156 56 146C58 134 60 116 62 104C56 108 48 114 44 122Z"
    )
    private val forearm = FigureShape.PathData(
        "M35 166C31 182 28 200 27 216C27 224 30 230 34 230C38 228 42 216 46 202C49 190 52 178 52 166C46 164 40 164 35 166Z"
    )
    private val calfFront = FigureShape.PathData(
        "M76 306C72 322 72 340 76 358C78 366 82 370 86 370C90 368 93 358 94 344C95 330 94 316 92 306C86 302 80 302 76 306Z"
    )
    private val calfBack = FigureShape.PathData(
        "M76 302C70 316 70 334 76 350C80 362 84 370 88 370C92 364 95 350 95 334C95 320 93 308 90 302C85 298 80 298 76 302Z"
    )
    private val chest = FigureShape.PathData(
        "M68 79C80 77 92 78 99 79L99 117C90 121 78 120 71 115C65 109 65 100 67 95C68 89 68 84 68 79Z"
    )
    private val quads = FigureShape.PathData(
        "M72 198C66 214 66 240 70 262C72 276 78 288 86 292C92 290 96 280 97 268C99 248 99 226 98 212C92 206 82 200 72 198Z"
    )
    private val hamstrings = FigureShape.PathData(
        "M72 224C68 240 70 262 74 278C78 288 84 292 90 290C95 284 97 270 97 252C97 240 98 232 98 228C90 232 80 230 72 224Z"
    )

    // Elbow, knee and ankle/foot - never tappable, always drawn in the figure-neutral colour.
    private val neutralCommon = listOf(
        FigureShape.Ellipse(cx = 31f, cy = 240f, rx = 7f, ry = 11f),
        FigureShape.Ellipse(cx = 85f, cy = 298f, rx = 9f, ry = 7f),
        FigureShape.PathData("M78 373C76 380 76 388 80 392L96 392C98 386 96 378 92 373Z")
    )

    val front = FigureSide(
        neutral = listOf(
            FigureShape.PathData("M100 76L70 78C66 100 70 116 74 130C76 150 74 170 72 190L100 202Z"),
            FigureShape.PathData("M100 50L91 52C89 62 82 70 66 74L86 77L100 77Z"),
            FigureShape.PathData("M72 190C70 200 70 206 72 212L100 214L100 200Z")
        ) + neutralCommon,
        parts = listOf(
            BodyPart("chest", chest),
            BodyPart("abs", FigureShape.RoundRect(84f, 123f, 15f, 21f, 6f)),
            BodyPart("abs", FigureShape.RoundRect(84f, 147f, 15f, 21f, 6f)),
            BodyPart("abs", FigureShape.RoundRect(85f, 171f, 14f, 22f, 6f)),
            BodyPart("arms", upperArm),
            BodyPart("arms", forearm),
            BodyPart("quads", quads),
            BodyPart("calves", calfFront),
            BodyPart("shoulders", delt)
        )
    )

    val back = FigureSide(
        neutral = listOf(
            FigureShape.PathData("M100 74L70 78C66 100 70 120 76 140C80 160 82 175 76 182L100 184Z"),
            FigureShape.PathData("M100 50L92 52L92 60L100 60Z")
        ) + neutralCommon,
        parts = listOf(
            BodyPart("back", FigureShape.PathData("M100 52L92 54C88 64 78 72 66 76L80 92L100 108Z")),
            BodyPart("back", FigureShape.PathData("M70 82C66 98 68 116 74 134C78 150 80 164 84 176L100 179L100 112L80 96Z")),
            BodyPart("glutes", FigureShape.PathData("M76 181C70 191 68 204 72 216C78 226 92 228 99 222L99 183C92 181 84 179 76 181Z")),
            BodyPart("arms", upperArm),
            BodyPart("arms", forearm),
            BodyPart("hamstrings", hamstrings),
            BodyPart("calves", calfBack),
            BodyPart("shoulders", delt)
        )
    )

    /** ids shown on the Front segment's count badge. */
    val frontIds = listOf("shoulders", "chest", "arms", "abs", "quads", "calves")

    /** ids shown on the Back segment's count badge. */
    val backIds = listOf("shoulders", "back", "arms", "glutes", "hamstrings", "calves")
}
